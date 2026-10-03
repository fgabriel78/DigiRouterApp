package es.routerapp.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Opt-in live test (set env ROUTER_LIVE=1). Only performs unauthenticated, read-only requests,
 * so it never touches an active admin session.
 */
class RouterLiveTest {
    private val live = System.getenv("ROUTER_LIVE") == "1"

    @Test
    fun publicParamsAreParsed() {
        if (!live) return
        val c = RouterClient()
        val p = c.fetchParams()
        assertEquals(128, p.modulusHex.length)
        assertEquals("010001", p.exponentHex)
        assertTrue(p.seq > 0)
        println("busy=${c.busy()}")
    }
}
