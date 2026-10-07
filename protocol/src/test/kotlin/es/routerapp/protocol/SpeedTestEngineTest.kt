package es.routerapp.protocol

import es.routerapp.protocol.speedtest.SpeedTestEngine
import es.routerapp.protocol.speedtest.SpeedTestPhase
import es.routerapp.protocol.speedtest.SpeedTestState
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpeedTestEngineTest {

    @Test
    fun calculateJitterTests() {
        assertEquals(0.0, SpeedTestEngine.calculateJitter(emptyList()))
        assertEquals(0.0, SpeedTestEngine.calculateJitter(listOf(15.0)))
        // samples: 10, 20, 15 -> diffs: |20-10|=10, |15-20|=5 -> avg = 7.5
        assertEquals(7.5, SpeedTestEngine.calculateJitter(listOf(10.0, 20.0, 15.0)))
    }

    @Test
    fun calculateMbpsTests() {
        assertEquals(0.0, SpeedTestEngine.calculateMbps(0, 1_000_000_000L))
        assertEquals(0.0, SpeedTestEngine.calculateMbps(1000, 0L))
        // 12_500_000 bytes in 1 second = 100,000,000 bits / s = 100 Mbps
        val mbps = SpeedTestEngine.calculateMbps(12_500_000L, 1_000_000_000L)
        assertEquals(100.0, mbps)
    }

    @Test
    fun speedTestEngineFlowSuccess() = runBlocking {
        val mockInterceptor = Interceptor { chain ->
            val req = chain.request()
            val url = req.url.toString()
            val responseBuilder = Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .header("cf-ray", "abcdef123456-MAD")

            when {
                url.contains("__down?bytes=0") -> {
                    responseBuilder.body("".toResponseBody("text/plain".toMediaType()))
                }
                url.contains("__down?bytes=") -> {
                    // Small test payload: 100 KB
                    val payload = ByteArray(100_000) { 1 }
                    responseBuilder.body(payload.toResponseBody("application/octet-stream".toMediaType()))
                }
                url.contains("__up") -> {
                    responseBuilder.body("OK".toResponseBody("text/plain".toMediaType()))
                }
                else -> {
                    responseBuilder.code(404).message("Not Found")
                }
            }
            responseBuilder.build()
        }

        val mockClient = OkHttpClient.Builder()
            .addInterceptor(mockInterceptor)
            .build()

        val engine = SpeedTestEngine(client = mockClient, baseUrl = "https://speed.cloudflare.com")
        val states = engine.runSpeedTest(downloadBytes = 100_000L, uploadBytes = 50_000L).toList()

        assertTrue(states.isNotEmpty(), "Flow should emit states")
        assertTrue(states.any { it is SpeedTestState.Running && it.progress.phase == SpeedTestPhase.PING })
        assertTrue(states.any { it is SpeedTestState.Running && it.progress.phase == SpeedTestPhase.DOWNLOAD })
        assertTrue(states.any { it is SpeedTestState.Running && it.progress.phase == SpeedTestPhase.UPLOAD })

        val finalState = states.last()
        assertTrue(finalState is SpeedTestState.Completed, "Final state should be Completed, but was $finalState")
        val result = finalState.result
        assertEquals("MAD", result.serverLocation)
        assertTrue(result.downloadMbps >= 0.0)
        assertTrue(result.uploadMbps >= 0.0)
    }

    @Test
    fun speedTestEngineNetworkError() = runBlocking {
        val failingInterceptor = Interceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(500)
                .message("Server Error")
                .body("Failed".toResponseBody("text/plain".toMediaType()))
                .build()
        }

        val client = OkHttpClient.Builder().addInterceptor(failingInterceptor).build()
        val engine = SpeedTestEngine(client = client)
        val states = engine.runSpeedTest(downloadBytes = 1000L, uploadBytes = 1000L).toList()

        val lastState = states.last()
        assertTrue(lastState is SpeedTestState.Error, "Expected Error state on 500 response, but was $lastState")
    }
}
