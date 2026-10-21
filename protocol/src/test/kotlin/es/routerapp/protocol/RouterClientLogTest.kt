package es.routerapp.protocol

import es.routerapp.protocol.log.LogSeverity
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RouterClientLogTest {

    @Test
    fun testGetSystemLogsRetrievesAndParses() {
        var requestedPath: String? = null
        val interceptor = Interceptor { chain ->
            val req = chain.request()
            requestedPath = req.url.encodedPath
            val sampleLogs = """
                0
                2026-10-20 22:50:00 [3] OMCI: Loss of signal
                2026-10-20 22:50:05 [6] DHCP: ACK sent
            """.trimIndent()
            Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(sampleLogs.toResponseBody("text/plain".toMediaType()))
                .build()
        }

        val okHttp = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val client = RouterClient("http://192.168.1.1", okHttp)

        val logs = client.getSystemLogs()
        assertEquals("/cgi/log", requestedPath)
        assertEquals(2, logs.size)
        assertEquals("2026-10-20 22:50:05", logs[0].timestamp)
        assertEquals(LogSeverity.INFO, logs[0].severity)
        assertEquals("DHCP", logs[0].module)
    }

    @Test
    fun testGetSystemLogsWithEncryptedResponse() {
        val crypto = GdprCrypto(
            modulusHex = "00",
            exponentHex = "010001",
            seq = 1000L,
            username = "admin",
            password = "password",
        )
        crypto.setKey("1234567890123456", "6543210987654321")

        val plaintextLogs = """
            0
            2026-10-21 19:03:33 [5] DHCPD: Send OFFER with ip 192.168.33.128
            2026-10-21 19:10:03 [5] MESH: Success Add Client : MAC(7C:D9:5C:11:E9:58)
        """.trimIndent()

        val encryptedCiphertext = crypto.aesEncryptBase64(plaintextLogs)

        var requestedPath: String? = null
        val interceptor = Interceptor { chain ->
            val req = chain.request()
            requestedPath = req.url.encodedPath
            Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(encryptedCiphertext.toResponseBody("text/plain".toMediaType()))
                .build()
        }

        val okHttp = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val client = RouterClient("http://192.168.1.1", okHttp)
        client.crypto = crypto

        val logs = client.getSystemLogs()
        assertEquals("/cgi/log", requestedPath)
        assertEquals(2, logs.size)
        // Reverse order (newest first)
        assertEquals("2026-10-21 19:10:03", logs[0].timestamp)
        assertEquals(LogSeverity.NOTICE, logs[0].severity)
        assertEquals("MESH", logs[0].module)
        assertEquals("Success Add Client : MAC(7C:D9:5C:11:E9:58)", logs[0].message)

        assertEquals("2026-10-21 19:03:33", logs[1].timestamp)
        assertEquals(LogSeverity.NOTICE, logs[1].severity)
        assertEquals("DHCPD", logs[1].module)
        assertEquals("Send OFFER with ip 192.168.33.128", logs[1].message)
    }

    @Test
    fun testClearSystemLogsCallsEndpoint() {
        var requestedPath: String? = null
        val interceptor = Interceptor { chain ->
            val req = chain.request()
            requestedPath = req.url.encodedPath
            Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("0\r\n".toResponseBody("text/plain".toMediaType()))
                .build()
        }

        val okHttp = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val client = RouterClient("http://192.168.1.1", okHttp)

        val success = client.clearSystemLogs()
        assertEquals("/cgi/log", requestedPath)
        assertTrue(success)
    }
}
