package es.digirouter.app

import android.app.Application
import es.routerapp.protocol.RouterException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.UnknownServiceException

class RouterViewModelTest {

    @Test
    fun `mapLoginError maps SecurityException to CleartextRestricted`() {
        val error = mapLoginError(SecurityException("Cleartext communication is restricted to local network ranges: 8.8.8.8"))
        assertEquals(LoginError.CleartextRestricted, error)
    }

    @Test
    fun `mapLoginError maps UnknownServiceException to CleartextRestricted`() {
        val error = mapLoginError(UnknownServiceException("CLEARTEXT communication to 192.168.1.50 not permitted by network security policy"))
        assertEquals(LoginError.CleartextRestricted, error)
    }

    @Test
    fun `mapLoginError maps IOException with CLEARTEXT message to CleartextRestricted`() {
        val error = mapLoginError(IOException("Cleartext HTTP traffic to example.com not permitted"))
        assertEquals(LoginError.CleartextRestricted, error)
    }

    @Test
    fun `mapLoginError maps RouterException to WrongPassword`() {
        val error = mapLoginError(RouterException(71233, "Invalid credentials"))
        assertEquals(LoginError.WrongPassword, error)
    }

    @Test
    fun `mapLoginError maps unexpected end of stream to SessionExpired`() {
        val error = mapLoginError(IOException("unexpected end of stream on http://192.168.1.1/cgi_gdpr?9"))
        assertEquals(LoginError.SessionExpired, error)
    }

    @Test
    fun `mapLoginError maps session timeout RouterException to SessionExpired`() {
        val error = mapLoginError(RouterException(-1, "Session expired"))
        assertEquals(LoginError.SessionExpired, error)
    }

    @Test
    fun `mapLoginError maps SocketException to SessionExpired`() {
        val error = mapLoginError(java.net.SocketException("Connection reset"))
        assertEquals(LoginError.SessionExpired, error)
    }

    @Test
    fun `mapLoginError maps generic IOException to Unreachable`() {
        val error = mapLoginError(IOException("Failed to connect to /192.168.1.1:80"))
        assertEquals(LoginError.Unreachable, error)
    }

    @Test
    fun `mapLoginError maps other throwables to Other`() {
        val error = mapLoginError(IllegalStateException("Unexpected state"))
        assertEquals(LoginError.Other, error)
    }

    @Test
    fun `initial state loads saved password and rememberPassword from CredentialStore`() {
        val store = FakeCredentialStore("saved_pass")
        val vm = RouterViewModel(Application(), store)
        assertEquals(true, vm.state.value.rememberPassword)
        assertEquals("saved_pass", vm.state.value.savedPassword)
    }

    @Test
    fun `initial state has false rememberPassword when store is empty`() {
        val store = FakeCredentialStore(null)
        val vm = RouterViewModel(Application(), store)
        assertEquals(false, vm.state.value.rememberPassword)
        assertEquals("", vm.state.value.savedPassword)
    }

    @Test
    fun `setRememberPassword false purges store and updates state`() {
        val store = FakeCredentialStore("saved_pass")
        val vm = RouterViewModel(Application(), store)
        vm.setRememberPassword(false)
        assertEquals(false, vm.state.value.rememberPassword)
        assertEquals("", vm.state.value.savedPassword)
        assertFalse(store.hasSavedPassword())
        assertNull(store.getSavedPassword())
    }

    @Test
    fun `setRememberPassword true enables remember flag in state`() {
        val store = FakeCredentialStore(null)
        val vm = RouterViewModel(Application(), store)
        vm.setRememberPassword(true)
        assertEquals(true, vm.state.value.rememberPassword)
    }

    @Test
    fun `logout retains saved credential in store and state`() {
        val store = FakeCredentialStore("saved_pass")
        val vm = RouterViewModel(Application(), store)
        vm.logout()
        assertEquals(false, vm.state.value.loggedIn)
        assertEquals(true, vm.state.value.rememberPassword)
        assertEquals("saved_pass", vm.state.value.savedPassword)
        assertEquals("saved_pass", store.getSavedPassword())
    }

    @Test
    fun `fingerprints state flow is initialized with store fingerprints and updated on scan`() {
        val metaPrefs = FakeSharedPreferences()
        val metaStore = SharedPrefsDeviceMetadataStore(metaPrefs)
        val fp = DeviceFingerprint(
            exactModel = "PlayStation 5",
            vendor = "Sony",
            category = DeviceCategory.CONSOLE,
            discoveryMethod = "UPnP",
        )
        metaStore.saveFingerprint("AA:BB:CC:DD:EE:99", fp)

        val vm = RouterViewModel(
            app = Application(),
            credentialStore = FakeCredentialStore(null),
            metadataStore = metaStore,
            fingerprintScanner = NetworkFingerprintScanner(null, metaStore, multicastBurstMs = 0L),
        )

        assertEquals(1, vm.fingerprints.value.size)
        assertEquals("PlayStation 5", vm.fingerprints.value["AA:BB:CC:DD:EE:99"]?.exactModel)
    }
    @Test
    fun `isRecoverableLoginFailure identifies stream and socket drops as recoverable`() {
        assertTrue(isRecoverableLoginFailure(IOException("unexpected end of stream on http://192.168.1.1/cgi_gdpr?9")))
        assertTrue(isRecoverableLoginFailure(java.net.SocketException("Connection reset")))
        assertTrue(isRecoverableLoginFailure(java.io.EOFException("End of stream")))
        assertTrue(isRecoverableLoginFailure(IOException("Failed to connect to /192.168.1.1:80")))
        assertTrue(isRecoverableLoginFailure(RouterException(-1, "Session expired: router returned cleartext redirect response")))
    }

    @Test
    fun `isRecoverableLoginFailure identifies credentials and security errors as non-recoverable`() {
        assertFalse(isRecoverableLoginFailure(RouterException(71233, "Invalid credentials")))
        assertFalse(isRecoverableLoginFailure(SecurityException("Cleartext communication is restricted to local network ranges: 8.8.8.8")))
        assertFalse(isRecoverableLoginFailure(UnknownServiceException("CLEARTEXT communication to 192.168.1.50 not permitted")))
        assertFalse(isRecoverableLoginFailure(IOException("Cleartext HTTP traffic to example.com not permitted")))
        assertFalse(isRecoverableLoginFailure(IllegalStateException("Unknown state")))
    }

    @Test
    fun `login automatically retries on transient transport failure and succeeds`() {
        var attempts = 0
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val vm = RouterViewModel(
            app = Application(),
            credentialStore = FakeCredentialStore(null),
            clientFactory = {
                attempts++
                if (attempts == 1) {
                    throw IOException("unexpected end of stream on http://192.168.1.1/cgi_gdpr?9")
                }
                throw RouterException(71233, "Invalid credentials")
            },
            coroutineScope = testScope,
            ioDispatcher = Dispatchers.Unconfined,
            retryDelayMs = 0L,
        )

        vm.login("admin_pass")

        assertEquals(2, attempts)
        assertEquals(LoginError.WrongPassword, vm.state.value.error)
    }

    @Test
    fun `login does not retry on non-recoverable credential rejection`() {
        var attempts = 0
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val vm = RouterViewModel(
            app = Application(),
            credentialStore = FakeCredentialStore(null),
            clientFactory = {
                attempts++
                throw RouterException(71233, "Invalid credentials")
            },
            coroutineScope = testScope,
            ioDispatcher = Dispatchers.Unconfined,
            retryDelayMs = 0L,
        )

        vm.login("wrong_pass")

        assertEquals(1, attempts)
        assertEquals(LoginError.WrongPassword, vm.state.value.error)
    }

    @Test
    fun `login does not retry on cleartext restriction security exception`() {
        var attempts = 0
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val vm = RouterViewModel(
            app = Application(),
            credentialStore = FakeCredentialStore(null),
            clientFactory = {
                attempts++
                throw SecurityException("Cleartext communication is restricted to local network ranges: 8.8.8.8")
            },
            coroutineScope = testScope,
            ioDispatcher = Dispatchers.Unconfined,
            retryDelayMs = 0L,
        )
        vm.setAddress("8.8.8.8")
        vm.login("pass")

        assertEquals(0, attempts)
        assertEquals(LoginError.CleartextRestricted, vm.state.value.error)
    }

    @Test
    fun `login exhausts retry and maps to unreachable when transport continues failing`() {
        var attempts = 0
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val vm = RouterViewModel(
            app = Application(),
            credentialStore = FakeCredentialStore(null),
            clientFactory = {
                attempts++
                throw IOException("Failed to connect to /192.168.1.1:80")
            },
            coroutineScope = testScope,
            ioDispatcher = Dispatchers.Unconfined,
            retryDelayMs = 0L,
        )

        vm.login("any_pass")

        assertEquals(2, attempts)
        assertEquals(LoginError.Unreachable, vm.state.value.error)
    }
}
