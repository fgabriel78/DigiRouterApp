package es.routerapp.app

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import es.routerapp.protocol.speedtest.SpeedTestEngine
import es.routerapp.protocol.speedtest.SpeedTestResult
import es.routerapp.protocol.speedtest.SpeedTestState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SpeedTestViewModel @JvmOverloads constructor(
    app: Application,
    private val engine: SpeedTestEngine = SpeedTestEngine(),
) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("speed_test_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _state = MutableStateFlow<SpeedTestState>(SpeedTestState.Idle)
    val state: StateFlow<SpeedTestState> = _state.asStateFlow()

    private val _lastResult = MutableStateFlow<SpeedTestResult?>(loadLastResult())
    val lastResult: StateFlow<SpeedTestResult?> = _lastResult.asStateFlow()

    private var activeJob: Job? = null

    private fun loadLastResult(): SpeedTestResult? {
        val raw = prefs.getString("last_result", null) ?: return null
        return runCatching { json.decodeFromString<SpeedTestResult>(raw) }.getOrNull()
    }

    private fun saveLastResult(result: SpeedTestResult) {
        _lastResult.value = result
        runCatching {
            prefs.edit().putString("last_result", json.encodeToString(result)).apply()
        }
    }

    fun startTest() {
        if (activeJob?.isActive == true) return
        activeJob = viewModelScope.launch {
            engine.runSpeedTest().collect { s ->
                _state.value = s
                if (s is SpeedTestState.Completed) {
                    saveLastResult(s.result)
                }
            }
        }
    }

    fun stopTest() {
        activeJob?.cancel()
        activeJob = null
        _state.value = SpeedTestState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        stopTest()
    }
}
