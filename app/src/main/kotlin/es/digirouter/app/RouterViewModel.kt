package es.digirouter.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import es.routerapp.protocol.RouterClient
import es.routerapp.protocol.RouterException
import es.routerapp.protocol.pages.PageEngine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.UnknownServiceException

enum class LoginError { WrongPassword, Unreachable, CleartextRestricted, SessionExpired, Other }

data class UiState(
    val address: String = "192.168.1.1",
    val username: String = "user",
    val loggingIn: Boolean = false,
    val loggedIn: Boolean = false,
    val otherSessionActive: Boolean = false,
    val error: LoginError? = null,
    val rememberPassword: Boolean = false,
    val savedPassword: String = "",
)

interface RecoveryRunner {
    suspend fun <T> run(block: suspend () -> T): T

    object NoOp : RecoveryRunner {
        override suspend fun <T> run(block: suspend () -> T): T = block()
    }
}

/** Holds the router session for the lifetime of the app process. */
class RouterViewModel @JvmOverloads constructor(
    app: Application,
    private val credentialStore: CredentialStore = SecureCredentialStore(app),
    metadataStore: DeviceMetadataStore? = null,
    fingerprintScanner: NetworkFingerprintScanner? = null,
    backupStore: BackupStore? = null,
    private val clientFactory: (String) -> RouterClient = { address -> RouterClient("http://$address") },
    coroutineScope: CoroutineScope? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val retryDelayMs: Long = 500L,
) : AndroidViewModel(app), RecoveryRunner {
    private val effectiveScope = coroutineScope ?: viewModelScope
    override suspend fun <T> run(block: suspend () -> T): T = executeWithRecovery(block)
    val metadataStore: DeviceMetadataStore = metadataStore
        ?: runCatching { SharedPrefsDeviceMetadataStore(app) }.getOrElse { InMemoryDeviceMetadataStore() }
    val fingerprintScanner: NetworkFingerprintScanner = fingerprintScanner
        ?: NetworkFingerprintScanner(app, this.metadataStore)
    val backupStore: BackupStore = backupStore ?: FileBackupStore(app)
    private val initialSavedPassword = credentialStore.getSavedPassword() ?: ""
    private val _state = MutableStateFlow(
        UiState(
            rememberPassword = credentialStore.hasSavedPassword(),
            savedPassword = initialSavedPassword,
        ),
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _fingerprints = MutableStateFlow<Map<String, DeviceFingerprint>>(emptyMap())
    val fingerprints: StateFlow<Map<String, DeviceFingerprint>> = _fingerprints.asStateFlow()

    init {
        runCatching {
            _fingerprints.value = this.metadataStore.getAllFingerprints()
        }
    }

    private var scanJob: kotlinx.coroutines.Job? = null

    fun triggerDeviceScan(devices: List<es.routerapp.protocol.pages.ConnectedDevice>) {
        if (devices.isEmpty()) return
        scanJob?.cancel()
        scanJob = effectiveScope.launch(ioDispatcher) {
            fingerprintScanner.scanFlow(devices).collect { (mac, fp) ->
                _fingerprints.update { current ->
                    current + (mac to fp)
                }
            }
        }
    }

    @Volatile
    private var activePassword: String? = null

    private val recoveryMutex = Mutex()

    @Volatile
    var client: RouterClient? = null
        private set

    @Volatile
    var engine: PageEngine? = null
        private set

    fun evictIdleConnections() {
        client?.evictIdleConnections()
    }

    fun setAddress(value: String) = _state.update { it.copy(address = value.trim(), error = null) }

    fun setUsername(value: String) = _state.update { it.copy(username = value.trim(), error = null) }

    fun setRememberPassword(value: Boolean) {
        if (!value) {
            credentialStore.clear()
            _state.update { it.copy(rememberPassword = false, savedPassword = "") }
        } else {
            _state.update { it.copy(rememberPassword = true) }
        }
    }

    fun login(password: String) {
        if (_state.value.loggingIn) return
        _state.update { it.copy(loggingIn = true, error = null) }
        effectiveScope.launch {
            val result = withContext(ioDispatcher) {
                val address = _state.value.address
                if (!LocalNetworkValidator.isLocalAddress(address)) {
                    return@withContext Result.failure(
                        SecurityException("Cleartext communication is restricted to local network ranges: $address")
                    )
                }

                suspend fun doAttempt(): Result<Boolean> = runCatching {
                    val c = clientFactory(address)
                    val busyLogined = runCatching { c.busy().isLogined }.getOrDefault(false)
                    c.login(password, _state.value.username)
                    client = c
                    engine = PageEngine(c)
                    activePassword = password
                    busyLogined
                }

                val firstAttempt = doAttempt()
                if (firstAttempt.isSuccess) {
                    firstAttempt
                } else {
                    val ex = firstAttempt.exceptionOrNull()
                    if (ex != null && isRecoverableLoginFailure(ex)) {
                        delay(retryDelayMs)
                        doAttempt()
                    } else {
                        firstAttempt
                    }
                }
            }
            _state.update { s ->
                result.fold(
                    onSuccess = {
                        if (s.rememberPassword) {
                            credentialStore.savePassword(password)
                        } else {
                            credentialStore.clear()
                        }
                        s.copy(
                            loggingIn = false,
                            loggedIn = true,
                            otherSessionActive = it,
                            savedPassword = if (s.rememberPassword) password else "",
                        )
                    },
                    onFailure = { s.copy(loggingIn = false, error = mapLoginError(it)) },
                )
            }
        }
    }

    suspend fun <T> executeWithRecovery(block: suspend () -> T): T {
        return try {
            block()
        } catch (t: Throwable) {
            val c = client
            val pass = activePassword
            if (c != null && pass != null && c.isSessionOrStreamFailure(t)) {
                recoveryMutex.withLock {
                    // If another concurrent call already refreshed the session successfully,
                    // retry block() immediately.
                    val quickRetry = runCatching { block() }
                    if (quickRetry.isSuccess) {
                        return quickRetry.getOrThrow()
                    }

                    // Otherwise, purge idle sockets and re-authenticate
                    c.evictIdleConnections()
                    val reauthResult = runCatching {
                        withContext(Dispatchers.IO) {
                            c.login(pass, _state.value.username)
                        }
                    }
                    if (reauthResult.isSuccess) {
                        try {
                            return block()
                        } catch (retryErr: Throwable) {
                            if (c.isSessionOrStreamFailure(retryErr)) {
                                handleSessionExpired(retryErr)
                            }
                            throw retryErr
                        }
                    } else {
                        val reauthErr = reauthResult.exceptionOrNull() ?: t
                        handleSessionExpired(reauthErr)
                        throw reauthErr
                    }
                }
            } else if (c != null && c.isSessionOrStreamFailure(t)) {
                handleSessionExpired(t)
                throw t
            } else {
                throw t
            }
        }
    }

    private fun handleSessionExpired(cause: Throwable) {
        activePassword = null
        client = null
        engine = null
        _state.update {
            it.copy(
                loggedIn = false,
                error = LoginError.SessionExpired,
            )
        }
    }

    fun logout() {
        val c = client ?: return
        activePassword = null
        client = null
        engine = null
        _state.update { it.copy(loggedIn = false) }
        effectiveScope.launch(ioDispatcher) { runCatching { c.logout() } }
    }
}

internal fun isRecoverableLoginFailure(t: Throwable): Boolean = when {
    RouterClient.isSessionOrStreamFailure(t) -> true
    t is SecurityException -> false
    t is UnknownServiceException -> false
    t.message?.contains("CLEARTEXT", ignoreCase = true) == true -> false
    t is RouterException -> false
    t is IOException -> true
    else -> false
}

internal fun mapLoginError(t: Throwable): LoginError = when {
    RouterClient.isSessionOrStreamFailure(t) -> LoginError.SessionExpired
    t is SecurityException -> LoginError.CleartextRestricted
    t is UnknownServiceException -> LoginError.CleartextRestricted
    t.message?.contains("CLEARTEXT", ignoreCase = true) == true -> LoginError.CleartextRestricted
    t is RouterException -> LoginError.WrongPassword
    t is IOException -> LoginError.Unreachable
    else -> LoginError.Other
}
