package es.routerapp.protocol.speedtest

import kotlinx.serialization.Serializable

enum class SpeedTestPhase {
    IDLE,
    PING,
    DOWNLOAD,
    UPLOAD,
    COMPLETED,
    ERROR,
}

data class SpeedTestProgress(
    val phase: SpeedTestPhase = SpeedTestPhase.IDLE,
    val progress: Float = 0f,
    val currentSpeedMbps: Double = 0.0,
    val pingMs: Double? = null,
    val jitterMs: Double? = null,
    val downloadMbps: Double? = null,
    val uploadMbps: Double? = null,
    val transferredBytes: Long = 0L,
    val totalBytes: Long = 0L,
)

@Serializable
data class SpeedTestResult(
    val pingMs: Double,
    val jitterMs: Double,
    val downloadMbps: Double,
    val uploadMbps: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val serverLocation: String? = null,
    val clientIp: String? = null,
)

sealed interface SpeedTestState {
    data object Idle : SpeedTestState
    data class Running(val progress: SpeedTestProgress) : SpeedTestState
    data class Completed(val result: SpeedTestResult) : SpeedTestState
    data class Error(val message: String, val partialResult: SpeedTestResult? = null) : SpeedTestState
}
