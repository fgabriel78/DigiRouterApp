package es.routerapp.protocol

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.ConnectionPool
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.EOFException
import java.io.IOException
import java.net.SocketException
import java.util.concurrent.TimeUnit

/** Error returned by the router (non-zero `errorcode` / `$.ret`). */
class RouterException(val code: Int, message: String) : IOException(message)

/** State of the "one admin session at a time" lock of the router. */
data class BusyStatus(val isLogined: Boolean, val isBusy: Boolean)

/** Public parameters published by the router before login. */
data class GdprParams(val modulusHex: String, val exponentHex: String, val seq: Long)

/**
 * Blocking client for the router's web API. All calls must be made from a background thread
 * (the Android app wraps them in `Dispatchers.IO`).
 *
 * Wire format (derived from the router's own JS, `gdprProxy.js`):
 *  - `POST /cgi_gdpr?9` with an AES+RSA encrypted JSON `{"data":{..},"operation":"go|gl|gs|so|ao|do|op|cgi","oid":".."}`
 *  - `TokenID` header with the session token obtained after login.
 */
class RouterClient(
    baseUrl: String = "http://192.168.1.1",
    httpClient: OkHttpClient? = null,
) {
    val baseUrl: String = baseUrl.trimEnd('/')

    private val cookieStore = mutableMapOf<String, Cookie>()
    private val http: OkHttpClient = (httpClient ?: OkHttpClient()).newBuilder()
        .apply {
            if (httpClient == null) {
                connectionPool(ConnectionPool(5, 15, TimeUnit.SECONDS))
            }
        }
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .cookieJar(object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                synchronized(cookieStore) {
                    cookies.forEach { if (it.expiresAt < System.currentTimeMillis()) cookieStore.remove(it.name) else cookieStore[it.name] = it }
                }
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> = synchronized(cookieStore) { cookieStore.values.toList() }
        })
        .build()

    /** Evicts all idle pooled TCP connections to prevent sending requests into dead router sockets. */
    fun evictIdleConnections() {
        http.connectionPool.evictAll()
    }

    /**
     * Checks whether an error is caused by a severed TCP transport stream,
     * an expired router session, or a cleartext redirect due to router timeout.
     */
    fun isSessionOrStreamFailure(t: Throwable): Boolean = Companion.isSessionOrStreamFailure(t)

    private var crypto: GdprCrypto? = null

    /** Value of the `TokenID` header; "0" before login. Updated by [login] when the router provides one. */
    @Volatile
    var tokenId: String = "0"

    val isLoggedIn: Boolean get() = crypto != null && loggedIn

    @Volatile
    private var loggedIn = false

    private fun builder(path: String): Request.Builder = Request.Builder()
        .url(baseUrl + path)
        // The router answers "406 Not Acceptable" to clients without a browser-like UA / Referer.
        .header("User-Agent", USER_AGENT)
        .header("Referer", "$baseUrl/")
        .header("Accept", "*/*")
        .header("TokenID", tokenId)

    private fun execute(req: Request): String =
        http.newCall(req).execute().use { resp ->
            val text = resp.body.string()
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code} for ${req.url.encodedPath}")
            text
        }

    // ---------------------------------------------------------------- unauthenticated

    fun fetchParams(): GdprParams {
        val body = execute(builder("/cgi/getGDPRParm").post("".toRequestBody(TEXT)).build())
        fun grab(name: String): String =
            Regex("""var\s+$name\s*=\s*"?([^";\r\n]+)"?\s*;""").find(body)?.groupValues?.get(1)
                ?: throw IOException("Parameter '$name' not found in getGDPRParm response")
        return GdprParams(grab("nn"), grab("ee"), grab("seq").toLong())
    }

    fun busy(): BusyStatus {
        val body = execute(builder("/cgi/getBusy").post("".toRequestBody(TEXT)).build())
        fun flag(name: String) = Regex("""var\s+$name\s*=\s*(\d+)""").find(body)?.groupValues?.get(1) == "1"
        return BusyStatus(flag("isLogined"), flag("isBusy"))
    }

    // ---------------------------------------------------------------- session

    /**
     * Logs in as [username] (the DIGI firmware only asks for the password; the user is `admin`).
     * @throws RouterException with the router's error code if the credentials are wrong
     *  (e.g. 71233 = wrong user/password).
     */
    fun login(password: String, username: String = "admin"): Unit = synchronized(this) {
        loggedIn = false
        tokenId = "0"
        val p = fetchParams()
        val c = GdprCrypto(p.modulusHex, p.exponentHex, p.seq, username, password)
        crypto = c
        val payload = buildJsonObject {
            put("data", buildJsonObject {
                put("UserName", java.util.Base64.getEncoder().encodeToString(username.toByteArray()))
                put("Passwd", java.util.Base64.getEncoder().encodeToString(password.toByteArray()))
                put("Action", "1")
                put("stack", "0,0,0,0,0,0")
                put("pstack", "0,0,0,0,0,0")
            })
            put("operation", "cgi")
            put("oid", "/cgi/login")
        }
        val reply = postEncrypted(payload.toString() + "\r\n", isLogin = true)
        val ret = parseCgiRet(reply)
        if (ret != 0) {
            crypto = null
            throw RouterException(ret, "Login rejected by router (code $ret)")
        }
        loggedIn = true
        refreshToken()
    }

    /** Fetches the authenticated landing page and extracts the session `TokenID` if it is embedded there. */
    fun refreshToken(): String? {
        val html = execute(builder("/").get().build())
        val token = TOKEN_REGEXES.firstNotNullOfOrNull { it.find(html)?.groupValues?.get(1) }
        if (token != null) tokenId = token
        return token
    }

    fun logout() {
        if (!loggedIn) return
        runCatching { cgi("/cgi/logout") }
        loggedIn = false
        crypto = null
        tokenId = "0"
        synchronized(cookieStore) { cookieStore.clear() }
    }

    // ---------------------------------------------------------------- data model operations

    /** `go`: reads one object instance. */
    fun get(oid: String, attrs: Map<String, String> = emptyMap(), stack: String = DEFAULT_STACK): JsonElement =
        dm("go", oid, attrs, stack)

    /** `gl`: reads a list (multi-instance object). */
    fun getList(oid: String, attrs: Map<String, String> = emptyMap(), stack: String = DEFAULT_STACK): JsonElement =
        dm("gl", oid, attrs, stack)

    /** `gs`: reads a sub list. */
    fun getSubList(oid: String, attrs: Map<String, String> = emptyMap(), stack: String = DEFAULT_STACK): JsonElement =
        dm("gs", oid, attrs, stack)

    /** `so`: WRITES attributes. Never call from discovery tooling. */
    fun set(oid: String, attrs: Map<String, String>, stack: String = DEFAULT_STACK): JsonElement =
        dm("so", oid, attrs, stack)

    /** `ao`: adds an instance. [parentStack] is the `pstack` of the parent instance for child objects. */
    fun add(oid: String, attrs: Map<String, String>, parentStack: String = DEFAULT_STACK): JsonElement =
        dm("ao", oid, attrs, DEFAULT_STACK, parentStack)

    fun delete(oid: String, stack: String = DEFAULT_STACK): JsonElement = dm("do", oid, emptyMap(), stack)

    /** `op`: runs an action (reboot, factory reset, WPS...). */
    fun operate(oid: String, attrs: Map<String, String> = emptyMap(), stack: String = DEFAULT_STACK): JsonElement =
        dm("op", oid, attrs, stack)

    /** `cgi`: legacy script-style operation, returns `$.ret`. */
    fun cgi(oid: String, attrs: Map<String, String> = emptyMap()): Int {
        val payload = envelope("cgi", oid, attrs, DEFAULT_STACK)
        val reply = postEncrypted(payload, isLogin = false)
        val trimmed = reply.trim()
        if (trimmed.startsWith("<") || trimmed.contains("location.href") || trimmed.contains("/cgi/login")) {
            throw RouterException(-1, "Session expired: router returned cleartext redirect response")
        }
        return parseCgiRet(reply)
    }

    private fun envelope(operation: String, oid: String, attrs: Map<String, String>, stack: String, pstack: String = DEFAULT_STACK): String =
        buildJsonObject {
            put("data", buildJsonObject {
                attrs.forEach { (k, v) -> put(k, v) }
                put("stack", stack)
                put("pstack", pstack)
            })
            put("operation", operation)
            put("oid", oid)
        }.toString() + "\r\n"

    private fun dm(operation: String, oid: String, attrs: Map<String, String>, stack: String, pstack: String = DEFAULT_STACK): JsonElement {
        val reply = postEncrypted(envelope(operation, oid, attrs, stack, pstack), isLogin = false)
        val trimmed = reply.trim()
        if (trimmed.startsWith("<") || trimmed.contains("location.href") || trimmed.contains("/cgi/login")) {
            throw RouterException(-1, "Session expired: router returned cleartext redirect response")
        }
        val json = Json.parseToJsonElement(trimmed)
        val obj = json.jsonObject
        val success = obj["success"]?.jsonPrimitive?.boolean ?: false
        if (!success) {
            val code = obj["errorcode"]?.jsonPrimitive?.int ?: -1
            throw RouterException(code, "Router error $code for $operation $oid")
        }
        return obj["data"] ?: JsonObject(emptyMap())
    }

    /** Low level authenticated GET of a router resource (HTML/JS); encrypted resources are decrypted. */
    fun rawGet(path: String, decrypt: Boolean = false): String {
        val sep = if (path.contains('?')) "&" else "?"
        val text = execute(builder("$path${sep}_=${System.currentTimeMillis()}").get().build())
        return if (decrypt) runCatching { requireNotNull(crypto).aesDecryptBase64(text) }.getOrDefault(text) else text
    }

    private fun postEncrypted(plain: String, isLogin: Boolean): String {
        val c = crypto ?: throw IllegalStateException("Not logged in")
        val body = c.encryptRequest(plain, isLogin).toRequestBody().toRequestBody(TEXT)
        val raw = execute(builder("/cgi_gdpr?9").post(body).build())
        return try {
            c.aesDecryptBase64(raw)
        } catch (e: Exception) {
            // Router replies in clear text on some errors (e.g. expired session).
            raw
        }
    }

    private fun parseCgiRet(text: String): Int =
        Regex("""\$\.ret\s*=\s*(\d+)""").find(text)?.groupValues?.get(1)?.toInt() ?: 0

    companion object {
        const val DEFAULT_STACK = "0,0,0,0,0,0"
        private val TEXT = "text/plain".toMediaType()
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"
        private val TOKEN_REGEXES = listOf(
            Regex("""\$\.tokenid\s*=\s*["']?([^"';\s]+)"""),
            Regex("""tokenid\s*[:=]\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""var\s+token\s*=\s*["']([^"']+)["']"""),
        )

        /**
         * Checks whether an error is caused by a severed TCP transport stream,
         * an expired router session, or a cleartext redirect due to router timeout.
         */
        fun isSessionOrStreamFailure(t: Throwable): Boolean {
            var curr: Throwable? = t
            while (curr != null) {
                if (curr is RouterException) {
                    if (curr.code == -1 || curr.code in 71000..71010 || curr.code == 71234) return true
                    val msg = curr.message?.lowercase().orEmpty()
                    if (msg.contains("session") || msg.contains("timeout") || msg.contains("redirect") || msg.contains("<html")) {
                        return true
                    }
                }
                if (curr is EOFException) return true
                if (curr is SocketException) return true
                if (curr is IOException) {
                    val msg = curr.message?.lowercase().orEmpty()
                    if (msg.contains("unexpected end of stream") ||
                        msg.contains("connection reset") ||
                        msg.contains("broken pipe") ||
                        msg.contains("software caused connection abort") ||
                        msg.contains("stream reset") ||
                        msg.contains("socket closed")
                    ) {
                        return true
                    }
                }
                if (curr is SerializationException || curr is IllegalArgumentException) {
                    val msg = curr.message?.lowercase().orEmpty()
                    if (msg.contains("expected start of the object") ||
                        msg.contains("unexpected symbol") ||
                        msg.contains("<html") ||
                        msg.contains("location") ||
                        msg.contains("redirect")
                    ) {
                        return true
                    }
                }
                if (curr is IllegalStateException) {
                    val msg = curr.message?.lowercase().orEmpty()
                    if (msg.contains("not logged in") || msg.contains("session")) return true
                }
                curr = curr.cause
            }
            return false
        }
    }
}
