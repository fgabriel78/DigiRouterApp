package es.routerapp.protocol

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Golden vectors in `golden.json` were produced by running the router's *own* JavaScript
 * (`encrypt.js`, `tpEncrypt.js`, `cryptoJS.min.js`) in Node, so these tests prove that the Kotlin
 * implementation is byte-for-byte compatible with the web UI.
 */
class GdprCryptoGoldenTest {
    private val golden: JsonObject = Json.parseToJsonElement(
        javaClass.getResource("/golden.json")!!.readText()
    ).jsonObject
    private val nn = golden["nn"]!!.jsonPrimitive.content
    private val ee = golden["ee"]!!.jsonPrimitive.content

    private fun arr(name: String): JsonArray = golden[name]!!.jsonArray

    @Test
    fun rsaNoPaddingMatchesWebUi() {
        for (v in arr("rsa")) {
            val o = v.jsonObject
            assertEquals(
                o["hex"]!!.jsonPrimitive.content,
                GdprCrypto.rsaEncryptNoPadding(o["plain"]!!.jsonPrimitive.content, nn, ee),
            )
        }
    }

    @Test
    fun md5MatchesWebUi() {
        for (v in arr("md5")) {
            val o = v.jsonObject
            assertEquals(o["hex"]!!.jsonPrimitive.content, GdprCrypto.md5Hex(o["plain"]!!.jsonPrimitive.content))
        }
    }

    @Test
    fun aesAndSignatureMatchWebUi() {
        val c = GdprCrypto(nn, ee, seq = 868691960L, username = "admin", password = "secret-pass")
        c.setKey("1234567890123456", "6543210987654321")
        for (v in arr("aes")) {
            val o = v.jsonObject
            val plain = o["plain"]!!.jsonPrimitive.content
            val body = c.encryptRequest(plain, isLogin = o["login"]!!.jsonPrimitive.int == 1)
            assertEquals(o["data"]!!.jsonPrimitive.content, body.data)
            assertEquals(o["sign"]!!.jsonPrimitive.content, body.sign)
            assertEquals(plain, c.aesDecryptBase64(body.data))
        }
    }
}
