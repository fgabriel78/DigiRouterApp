package es.routerapp.app

import android.app.Application
import es.routerapp.protocol.RouterException
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
}
