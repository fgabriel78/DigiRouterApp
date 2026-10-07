package es.routerapp.protocol.speedtest

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class SpeedTestEngine(
    private val client: OkHttpClient = defaultClient(),
    private val baseUrl: String = DEFAULT_BASE_URL,
) {
    companion object {
        const val DEFAULT_BASE_URL = "https://speed.cloudflare.com"
        const val DEFAULT_DOWNLOAD_BYTES = 25_000_000L // 25 MB
        const val DEFAULT_UPLOAD_BYTES = 10_000_000L // 10 MB
        const val PING_SAMPLES = 5

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        fun calculateJitter(samples: List<Double>): Double {
            if (samples.size < 2) return 0.0
            val differences = samples.zipWithNext { a, b -> abs(a - b) }
            return differences.average()
        }

        fun calculateMbps(bytes: Long, durationNanos: Long): Double {
            if (durationNanos <= 0 || bytes <= 0) return 0.0
            val seconds = durationNanos.toDouble() / 1_000_000_000.0
            val bits = bytes.toDouble() * 8.0
            return (bits / seconds) / 1_000_000.0
        }
    }

    fun runSpeedTest(
        downloadBytes: Long = DEFAULT_DOWNLOAD_BYTES,
        uploadBytes: Long = DEFAULT_UPLOAD_BYTES,
    ): Flow<SpeedTestState> = flow {
        emit(SpeedTestState.Running(SpeedTestProgress(phase = SpeedTestPhase.PING, progress = 0f)))

        var finalPing = 0.0
        var finalJitter = 0.0
        var finalDownload = 0.0
        var finalUpload = 0.0
        var serverLocation: String? = null
        var clientIp: String? = null

        // 1. PING & JITTER PHASE
        val pingSamples = mutableListOf<Double>()
        try {
            for (i in 1..PING_SAMPLES) {
                if (!currentCoroutineContext().isActive) return@flow

                val pingReq = Request.Builder()
                    .url("$baseUrl/__down?bytes=0")
                    .header("Cache-Control", "no-cache")
                    .build()

                val call = client.newCall(pingReq)
                val startNanos = System.nanoTime()
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Ping failed with HTTP ${response.code}")
                    }
                    val elapsedMs = (System.nanoTime() - startNanos).toDouble() / 1_000_000.0
                    pingSamples.add(elapsedMs)

                    // Extract server location / ray if available
                    val cfRay = response.header("cf-ray")
                    if (cfRay != null && serverLocation == null) {
                        val parts = cfRay.split("-")
                        if (parts.size > 1) {
                            serverLocation = parts[1].trim()
                        }
                    }
                }

                finalPing = pingSamples.average()
                finalJitter = calculateJitter(pingSamples)

                emit(
                    SpeedTestState.Running(
                        SpeedTestProgress(
                            phase = SpeedTestPhase.PING,
                            progress = i.toFloat() / PING_SAMPLES.toFloat(),
                            pingMs = finalPing,
                            jitterMs = finalJitter,
                        )
                    )
                )
            }
        } catch (e: Exception) {
            emit(SpeedTestState.Error(e.message ?: "Failed ping measurement"))
            return@flow
        }

        // 2. DOWNLOAD PHASE
        emit(
            SpeedTestState.Running(
                SpeedTestProgress(
                    phase = SpeedTestPhase.DOWNLOAD,
                    progress = 0f,
                    pingMs = finalPing,
                    jitterMs = finalJitter,
                )
            )
        )

        try {
            val dlReq = Request.Builder()
                .url("$baseUrl/__down?bytes=$downloadBytes")
                .header("Cache-Control", "no-cache")
                .build()

            val call = client.newCall(dlReq)
            val startDlNanos = System.nanoTime()
            var totalBytesRead = 0L
            var lastUpdateNanos = startDlNanos
            var lastUpdateBytes = 0L

            call.execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Download failed with HTTP ${response.code}")
                }
                val body = response.body
                val stream = body.byteStream()
                val buffer = ByteArray(16384)

                while (currentCoroutineContext().isActive) {
                    val read = stream.read(buffer)
                    if (read == -1) break
                    totalBytesRead += read

                    val nowNanos = System.nanoTime()
                    val windowDuration = nowNanos - lastUpdateNanos
                    if (windowDuration >= 100_000_000L) { // Every 100ms
                        val windowBytes = totalBytesRead - lastUpdateBytes
                        val instantMbps = calculateMbps(windowBytes, windowDuration)
                        val overallMbps = calculateMbps(totalBytesRead, nowNanos - startDlNanos)
                        val dlProgress = (totalBytesRead.toFloat() / downloadBytes.toFloat()).coerceIn(0f, 1f)

                        emit(
                            SpeedTestState.Running(
                                SpeedTestProgress(
                                    phase = SpeedTestPhase.DOWNLOAD,
                                    progress = dlProgress,
                                    currentSpeedMbps = instantMbps,
                                    downloadMbps = overallMbps,
                                    pingMs = finalPing,
                                    jitterMs = finalJitter,
                                    transferredBytes = totalBytesRead,
                                    totalBytes = downloadBytes,
                                )
                            )
                        )
                        lastUpdateNanos = nowNanos
                        lastUpdateBytes = totalBytesRead
                    }
                }
            }

            val totalDlDuration = System.nanoTime() - startDlNanos
            finalDownload = calculateMbps(totalBytesRead, totalDlDuration)

            emit(
                SpeedTestState.Running(
                    SpeedTestProgress(
                        phase = SpeedTestPhase.DOWNLOAD,
                        progress = 1f,
                        currentSpeedMbps = finalDownload,
                        downloadMbps = finalDownload,
                        pingMs = finalPing,
                        jitterMs = finalJitter,
                        transferredBytes = totalBytesRead,
                        totalBytes = downloadBytes,
                    )
                )
            )
        } catch (e: Exception) {
            val partial = SpeedTestResult(
                pingMs = finalPing,
                jitterMs = finalJitter,
                downloadMbps = 0.0,
                uploadMbps = 0.0,
                serverLocation = serverLocation,
            )
            emit(SpeedTestState.Error(e.message ?: "Failed download measurement", partial))
            return@flow
        }

        // 3. UPLOAD PHASE
        emit(
            SpeedTestState.Running(
                SpeedTestProgress(
                    phase = SpeedTestPhase.UPLOAD,
                    progress = 0f,
                    pingMs = finalPing,
                    jitterMs = finalJitter,
                    downloadMbps = finalDownload,
                )
            )
        )

        try {
            val startUpNanos = System.nanoTime()
            var totalBytesWritten = 0L
            var lastUpdateNanos = startUpNanos
            var lastUpdateBytes = 0L

            val chunk = ByteArray(16384)
            val requestBody = object : RequestBody() {
                override fun contentType() = "application/octet-stream".toMediaType()
                override fun contentLength() = uploadBytes

                override fun writeTo(sink: BufferedSink) {
                    var remaining = uploadBytes
                    while (remaining > 0) {
                        val toWrite = minOf(remaining, chunk.size.toLong()).toInt()
                        sink.write(chunk, 0, toWrite)
                        sink.flush()
                        remaining -= toWrite
                        totalBytesWritten += toWrite

                        val nowNanos = System.nanoTime()
                        val windowDuration = nowNanos - lastUpdateNanos
                        if (windowDuration >= 100_000_000L) {
                            val windowBytes = totalBytesWritten - lastUpdateBytes
                            val instantMbps = calculateMbps(windowBytes, windowDuration)
                            val overallMbps = calculateMbps(totalBytesWritten, nowNanos - startUpNanos)
                            val upProgress = (totalBytesWritten.toFloat() / uploadBytes.toFloat()).coerceIn(0f, 1f)

                            // Note: cannot directly suspend/emit from inside OkHttp writeTo,
                            // but we update the tracking timestamps for final calculation
                            lastUpdateNanos = nowNanos
                            lastUpdateBytes = totalBytesWritten
                        }
                    }
                }
            }

            val upReq = Request.Builder()
                .url("$baseUrl/__up")
                .header("Cache-Control", "no-cache")
                .post(requestBody)
                .build()

            val call = client.newCall(upReq)
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Upload failed with HTTP ${response.code}")
                }
            }

            val totalUpDuration = System.nanoTime() - startUpNanos
            finalUpload = calculateMbps(totalBytesWritten, totalUpDuration)
        } catch (e: Exception) {
            val partial = SpeedTestResult(
                pingMs = finalPing,
                jitterMs = finalJitter,
                downloadMbps = finalDownload,
                uploadMbps = 0.0,
                serverLocation = serverLocation,
            )
            emit(SpeedTestState.Error(e.message ?: "Failed upload measurement", partial))
            return@flow
        }

        // 4. COMPLETED
        val result = SpeedTestResult(
            pingMs = (finalPing * 10.0).toLong() / 10.0,
            jitterMs = (finalJitter * 10.0).toLong() / 10.0,
            downloadMbps = (finalDownload * 10.0).toLong() / 10.0,
            uploadMbps = (finalUpload * 10.0).toLong() / 10.0,
            serverLocation = serverLocation,
            clientIp = clientIp,
        )

        emit(SpeedTestState.Completed(result))
    }.flowOn(Dispatchers.IO)
}
