package es.routerapp.protocol

import kotlinx.serialization.SerializationException
import java.io.EOFException
import java.io.IOException
import java.net.SocketException
import java.net.UnknownHostException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RouterClientTest {

    @Test
    fun testEvictIdleConnectionsDoesNotThrow() {
        val client = RouterClient()
        client.evictIdleConnections()
    }

    @Test
    fun testIsSessionOrStreamFailureMatchesStreamDrops() {
        val streamDrop = IOException("unexpected end of stream on http://192.168.1.1/cgi_gdpr?9")
        assertTrue(RouterClient.isSessionOrStreamFailure(streamDrop))

        val socketReset = SocketException("Connection reset by peer")
        assertTrue(RouterClient.isSessionOrStreamFailure(socketReset))

        val eof = EOFException("End of stream")
        assertTrue(RouterClient.isSessionOrStreamFailure(eof))

        val brokenPipe = IOException("Broken pipe")
        assertTrue(RouterClient.isSessionOrStreamFailure(brokenPipe))

        val client = RouterClient()
        assertTrue(client.isSessionOrStreamFailure(streamDrop))
    }

    @Test
    fun testIsSessionOrStreamFailureMatchesSessionExpirationAndRedirects() {
        val cleartextRedirect = RouterException(-1, "Session expired: router returned cleartext redirect response")
        assertTrue(RouterClient.isSessionOrStreamFailure(cleartextRedirect))

        val sessionTimeoutCode = RouterException(71000, "session timeout")
        assertTrue(RouterClient.isSessionOrStreamFailure(sessionTimeoutCode))

        val sessionErrorCode = RouterException(71234, "session error")
        assertTrue(RouterClient.isSessionOrStreamFailure(sessionErrorCode))

        val serializationError = SerializationException("Unexpected symbol '<' at offset 0 while parsing JSON")
        assertTrue(RouterClient.isSessionOrStreamFailure(serializationError))

        val wrapped = RuntimeException("Wrapped failure", IOException("unexpected end of stream"))
        assertTrue(RouterClient.isSessionOrStreamFailure(wrapped))
    }

    @Test
    fun testIsSessionOrStreamFailureDoesNotMatchUnrelatedErrors() {
        val wrongPassword = RouterException(71233, "Login rejected by router (code 71233)")
        assertFalse(RouterClient.isSessionOrStreamFailure(wrongPassword))

        val unknownHost = UnknownHostException("192.168.1.1")
        assertFalse(RouterClient.isSessionOrStreamFailure(unknownHost))

        val invalidParam = IllegalArgumentException("Invalid argument format")
        assertFalse(RouterClient.isSessionOrStreamFailure(invalidParam))
    }
}
