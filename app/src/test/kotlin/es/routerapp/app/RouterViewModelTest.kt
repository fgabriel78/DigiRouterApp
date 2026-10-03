package es.routerapp.app

import es.routerapp.protocol.RouterException
import org.junit.Assert.assertEquals
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
    fun `mapLoginError maps generic IOException to Unreachable`() {
        val error = mapLoginError(IOException("Failed to connect to /192.168.1.1:80"))
        assertEquals(LoginError.Unreachable, error)
    }

    @Test
    fun `mapLoginError maps other throwables to Other`() {
        val error = mapLoginError(IllegalStateException("Unexpected state"))
        assertEquals(LoginError.Other, error)
    }
}
