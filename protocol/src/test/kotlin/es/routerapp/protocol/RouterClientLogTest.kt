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
