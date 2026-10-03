package es.routerapp.protocol

import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Re-implementation (in pure JVM code, also valid on Android) of the cryptographic scheme used
 * by the router web UI ("GDPR login encryption", see the router's `tpEncrypt.js` / `encrypt.js`):
 *
 *  - The router publishes a 512-bit RSA public key (`nn`, `ee`) and a sequence number (`seq`)
 *    at `POST /cgi/getGDPRParm`.
 *  - The client creates an AES-128-CBC key/iv. Request bodies are `AES(base64)` and are
 *    accompanied by an RSA "signature" (really a raw RSA encryption without padding) of
 *    `[key=..&iv=..&]h=MD5(user+password)&s=seq+len(data)`.
 *  - Responses are AES-CBC encrypted + base64 with the same key.
 */
class GdprCrypto(
    private val modulusHex: String,
    private val exponentHex: String,
    private val seq: Long,
    username: String,
    password: String,
    private val random: SecureRandom = SecureRandom(),
) {
    /** MD5(username + password) as lowercase hex – identifies the session owner for the router. */
    val hash: String = md5Hex(username + password)

    var key: String = randomDigits(KEY_LEN)
        private set
    var iv: String = randomDigits(IV_LEN)
        private set

    /** Replaces the AES key/iv (used by tests to reproduce the web UI's output). */
    fun setKey(key: String, iv: String) {
        require(key.length == KEY_LEN && iv.length == IV_LEN) { "AES-128 key and iv must be 16 chars" }
        this.key = key
        this.iv = iv
    }

    fun regenerateKey() {
        key = randomDigits(KEY_LEN)
        iv = randomDigits(IV_LEN)
    }

    private fun randomDigits(n: Int): String =
        buildString { repeat(n) { append(('0' + random.nextInt(10))) } }

    private fun cipher(mode: Int): Cipher {
        // Not using `apply {}`: inside it `iv` would resolve to Cipher.getIV().
        val c = Cipher.getInstance("AES/CBC/PKCS5Padding")
        c.init(
            mode,
            SecretKeySpec(this.key.toByteArray(Charsets.UTF_8), "AES"),
            IvParameterSpec(this.iv.toByteArray(Charsets.UTF_8)),
        )
        return c
    }

    fun aesEncryptBase64(plain: String): String =
        Base64.getEncoder().encodeToString(cipher(Cipher.ENCRYPT_MODE).doFinal(plain.toByteArray(Charsets.UTF_8)))

    fun aesDecryptBase64(encrypted: String): String {
        val clean = encrypted.trim()
        return String(cipher(Cipher.DECRYPT_MODE).doFinal(Base64.getDecoder().decode(clean)), Charsets.UTF_8)
    }

    /** Builds the `sign=..\r\ndata=..\r\n` request body for an already serialised [plain] payload. */
    fun encryptRequest(plain: String, isLogin: Boolean): EncryptedBody {
        val data = aesEncryptBase64(plain)
        val s = (seq + data.length).toString()
        val signPlain = if (isLogin) "key=$key&iv=$iv&h=$hash&s=$s" else "h=$hash&s=$s"
        return EncryptedBody(sign = rsaEncryptNoPadding(signPlain, modulusHex, exponentHex), data = data)
    }

    data class EncryptedBody(val sign: String, val data: String) {
        fun toRequestBody(): String = "sign=$sign\r\ndata=$data\r\n"
    }

    companion object {
        const val KEY_LEN = 16
        const val IV_LEN = 16
        private const val RSA_BYTES = 64 // 512 bit key

        fun md5Hex(text: String): String =
            MessageDigest.getInstance("MD5").digest(text.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }

        /**
         * Raw RSA (no padding) as done by `$.rsa.encrypt(val, nn, ee, 512, 0)` in the web UI:
         * the text is split in 64-byte blocks, each block is right-padded with zero bytes,
         * interpreted as a big-endian integer and exponentiated. Every ciphertext block is
         * emitted as 128 lowercase hex characters.
         */
        fun rsaEncryptNoPadding(text: String, modulusHex: String, exponentHex: String): String {
            val n = BigInteger(modulusHex, 16)
            val e = BigInteger(exponentHex, 16)
            val bytes = text.toByteArray(Charsets.UTF_8)
            val out = StringBuilder()
            var offset = 0
            while (offset < bytes.size) {
                val end = minOf(offset + RSA_BYTES, bytes.size)
                val block = ByteArray(RSA_BYTES)
                System.arraycopy(bytes, offset, block, 0, end - offset)
                val c = BigInteger(1, block).modPow(e, n)
                out.append(c.toString(16).padStart(RSA_BYTES * 2, '0'))
                offset += RSA_BYTES
            }
            return out.toString()
        }
    }
}
