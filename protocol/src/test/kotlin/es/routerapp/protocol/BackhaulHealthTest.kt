package es.routerapp.protocol

import es.routerapp.protocol.pages.BackhaulHealth
import es.routerapp.protocol.pages.MeshNode
import es.routerapp.protocol.pages.evaluateBackhaulHealth
import org.junit.Assert.assertEquals
import org.junit.Test

class BackhaulHealthTest {

    private fun createNode(
        isController: Boolean = false,
        active: Boolean = true,
        backhaulType: String? = null,
        backhaulSignal: Int? = null,
        linkRate: Long? = null,
    ) = MeshNode(
        mac = "AA:BB:CC:DD:EE:FF",
        name = "Satellite",
        model = "HX220",
        ip = "192.168.1.2",
        isController = isController,
        active = active,
        backhaulType = backhaulType,
        backhaulSignal = backhaulSignal,
        linkRate = linkRate,
        uptime = 3600L,
    )

    @Test
    fun `controller returns NONE`() {
        val controller = createNode(isController = true, backhaulType = null)
        assertEquals(BackhaulHealth.NONE, evaluateBackhaulHealth(controller))
        assertEquals(BackhaulHealth.NONE, controller.backhaulHealth)
    }

    @Test
    fun `inactive satellite returns NONE`() {
        val inactive = createNode(active = false, backhaulType = "Ethernet")
        assertEquals(BackhaulHealth.NONE, evaluateBackhaulHealth(inactive))
    }

    @Test
    fun `satellite without backhaul type returns NONE`() {
        val node = createNode(backhaulType = null)
        assertEquals(BackhaulHealth.NONE, evaluateBackhaulHealth(node))
    }

    @Test
    fun `ethernet backhaul returns WIRED_OPTIMAL`() {
        val wired = createNode(backhaulType = "Ethernet", linkRate = 1000L)
        assertEquals(BackhaulHealth.WIRED_OPTIMAL, evaluateBackhaulHealth(wired))
    }

    @Test
    fun `high rate wifi backhaul returns WIFI_EXCELLENT`() {
        val node = createNode(backhaulType = "Wi-Fi", linkRate = 866L, backhaulSignal = 120)
        assertEquals(BackhaulHealth.WIFI_EXCELLENT, evaluateBackhaulHealth(node))
    }

    @Test
    fun `high signal wifi backhaul returns WIFI_EXCELLENT`() {
        val nodeWithRcpi = createNode(backhaulType = "Wi-Fi", linkRate = 150L, backhaulSignal = 150)
        assertEquals(BackhaulHealth.WIFI_EXCELLENT, evaluateBackhaulHealth(nodeWithRcpi))

        val nodeWithLevel = createNode(backhaulType = "Wi-Fi", linkRate = 150L, backhaulSignal = 4)
        assertEquals(BackhaulHealth.WIFI_EXCELLENT, evaluateBackhaulHealth(nodeWithLevel))
    }

    @Test
    fun `medium signal or rate wifi backhaul returns WIFI_GOOD`() {
        val node = createNode(backhaulType = "Wi-Fi", linkRate = 300L, backhaulSignal = 80)
        assertEquals(BackhaulHealth.WIFI_GOOD, evaluateBackhaulHealth(node))

        val nodeLevel3 = createNode(backhaulType = "Wi-Fi", linkRate = 100L, backhaulSignal = 3)
        assertEquals(BackhaulHealth.WIFI_GOOD, evaluateBackhaulHealth(nodeLevel3))

        val nodeRcpi120 = createNode(backhaulType = "Wi-Fi", linkRate = 100L, backhaulSignal = 120)
        assertEquals(BackhaulHealth.WIFI_GOOD, evaluateBackhaulHealth(nodeRcpi120))
    }

    @Test
    fun `low signal and low rate wifi backhaul returns WIFI_WEAK`() {
        val node = createNode(backhaulType = "Wi-Fi", linkRate = 54L, backhaulSignal = 1)
        assertEquals(BackhaulHealth.WIFI_WEAK, evaluateBackhaulHealth(node))

        val nodeRcpiLow = createNode(backhaulType = "Wi-Fi", linkRate = 72L, backhaulSignal = 60)
        assertEquals(BackhaulHealth.WIFI_WEAK, evaluateBackhaulHealth(nodeRcpiLow))
    }
}
