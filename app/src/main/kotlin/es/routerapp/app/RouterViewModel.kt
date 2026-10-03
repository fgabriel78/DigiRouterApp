package es.routerapp.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import es.routerapp.protocol.RouterClient
import es.routerapp.protocol.RouterException
import es.routerapp.protocol.pages.PageEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.UnknownServiceException

enum class LoginError { WrongPassword, Unreachable, CleartextRestricted, Other }

data class UiState(
    val address: String = "192.168.1.1",
    val username: String = "user",
    val loggingIn: Boolean = false,
    val loggedIn: Boolean = false,
    val otherSessionActive: Boolean = false,
    val error: LoginError? = null,
)

/** Holds the router session for the lifetime of the app process. */
class RouterViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    @Volatile
    var client: RouterClient? = null
        private set

    @Volatile
    var engine: PageEngine? = null
        private set

    fun setAddress(value: String) = _state.update { it.copy(address = value.trim(), error = null) }

    fun setUsername(value: String) = _state.update { it.copy(username = value.trim(), error = null) }

    fun login(password: String) {
        if (_state.value.loggingIn) return
        _state.update { it.copy(loggingIn = true, error = null) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val address = _state.value.address
                    if (!LocalNetworkValidator.isLocalAddress(address)) {
                        throw SecurityException("Cleartext communication is restricted to local network ranges: $address")
                    }
                    val c = RouterClient("http://" + address)
                    val busy = c.busy()
                    c.login(password, _state.value.username)
                    client = c
                    engine = PageEngine(c)
                    busy.isLogined
                }
            }
            _state.update { s ->
                result.fold(
                    onSuccess = { s.copy(loggingIn = false, loggedIn = true, otherSessionActive = it) },
                    onFailure = { s.copy(loggingIn = false, error = mapLoginError(it)) },
                )
            }
        }
    }

    fun logout() {
        val c = client ?: return
        client = null
        engine = null
        _state.update { it.copy(loggedIn = false) }
        viewModelScope.launch(Dispatchers.IO) { runCatching { c.logout() } }
    }
}

internal fun mapLoginError(t: Throwable): LoginError = when {
    t is SecurityException -> LoginError.CleartextRestricted
    t is UnknownServiceException -> LoginError.CleartextRestricted
    t.message?.contains("CLEARTEXT", ignoreCase = true) == true -> LoginError.CleartextRestricted
    t is RouterException -> LoginError.WrongPassword
    t is IOException -> LoginError.Unreachable
    else -> LoginError.Other
}

