package es.routerapp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceHostnameHeuristicsTest {

    @Test
    fun `infers Apple devices correctly`() {
        val iphone = DeviceHostnameHeuristics.infer("iPhone-de-Carlos")
        assertEquals("Apple", iphone.vendor)
        assertEquals(DeviceCategory.PHONE, iphone.category)

        val ipad = DeviceHostnameHeuristics.infer("iPad-Air-5")
        assertEquals("Apple", ipad.vendor)
        assertEquals(DeviceCategory.TABLET, ipad.category)

        val macbook = DeviceHostnameHeuristics.infer("MacBook-Pro-M2")
        assertEquals("Apple", macbook.vendor)
        assertEquals(DeviceCategory.COMPUTER, macbook.category)

        val watch = DeviceHostnameHeuristics.infer("Apple-Watch-Series-8")
        assertEquals("Apple", watch.vendor)
        assertEquals(DeviceCategory.IOT, watch.category)

        val appleTv = DeviceHostnameHeuristics.infer("Apple-TV-4K")
        assertEquals("Apple", appleTv.vendor)
        assertEquals(DeviceCategory.TV, appleTv.category)
    }

    @Test
    fun `infers Samsung devices correctly`() {
        val s23 = DeviceHostnameHeuristics.infer("Galaxy-S23-Ultra")
        assertEquals("Samsung", s23.vendor)
        assertEquals(DeviceCategory.PHONE, s23.category)

        val a52 = DeviceHostnameHeuristics.infer("Galaxy-A52s")
        assertEquals("Samsung", a52.vendor)
        assertEquals(DeviceCategory.PHONE, a52.category)

        val modelNum = DeviceHostnameHeuristics.infer("SM-G991B")
        assertEquals("Samsung", modelNum.vendor)
        assertEquals(DeviceCategory.PHONE, modelNum.category)

        val tab = DeviceHostnameHeuristics.infer("Galaxy-Tab-S8")
        assertEquals("Samsung", tab.vendor)
        assertEquals(DeviceCategory.TABLET, tab.category)

        val samsungTv = DeviceHostnameHeuristics.infer("Samsung-TV-QLED")
        assertEquals("Samsung", samsungTv.vendor)
        assertEquals(DeviceCategory.TV, samsungTv.category)
    }

    @Test
    fun `infers Google devices correctly`() {
        val pixel = DeviceHostnameHeuristics.infer("Pixel-8-Pro")
        assertEquals("Google", pixel.vendor)
        assertEquals(DeviceCategory.PHONE, pixel.category)

        val nest = DeviceHostnameHeuristics.infer("Nest-Mini-Kitchen")
        assertEquals("Google", nest.vendor)
        assertEquals(DeviceCategory.IOT, nest.category)

        val chromecast = DeviceHostnameHeuristics.infer("Chromecast-Living-Room")
        assertEquals("Google", chromecast.vendor)
        assertEquals(DeviceCategory.TV, chromecast.category)
    }

    @Test
    fun `infers Xiaomi and subbrands`() {
        val redmi = DeviceHostnameHeuristics.infer("Redmi-Note-12-Pro")
        assertEquals("Xiaomi", redmi.vendor)
        assertEquals(DeviceCategory.PHONE, redmi.category)

        val poco = DeviceHostnameHeuristics.infer("POCO-X5-Pro")
        assertEquals("Xiaomi", poco.vendor)
        assertEquals(DeviceCategory.PHONE, poco.category)
    }

    @Test
    fun `infers Amazon devices`() {
        val echo = DeviceHostnameHeuristics.infer("Echo-Dot-Salon")
        assertEquals("Amazon", echo.vendor)
        assertEquals(DeviceCategory.IOT, echo.category)

        val fireStick = DeviceHostnameHeuristics.infer("Fire-TV-Stick-4K")
        assertEquals("Amazon", fireStick.vendor)
        assertEquals(DeviceCategory.TV, fireStick.category)

        val kindle = DeviceHostnameHeuristics.infer("Kindle-Paperwhite")
        assertEquals("Amazon", kindle.vendor)
        assertEquals(DeviceCategory.TABLET, kindle.category)
    }

    @Test
    fun `infers Consoles and TVs`() {
        val nintendo = DeviceHostnameHeuristics.infer("Nintendo-Switch")
        assertEquals("Nintendo", nintendo.vendor)
        assertEquals(DeviceCategory.CONSOLE, nintendo.category)

        val ps5 = DeviceHostnameHeuristics.infer("PS5-Salon")
        assertEquals("Sony", ps5.vendor)
        assertEquals(DeviceCategory.CONSOLE, ps5.category)

        val xbox = DeviceHostnameHeuristics.infer("Xbox-Series-X")
        assertEquals("Microsoft", xbox.vendor)
        assertEquals(DeviceCategory.CONSOLE, xbox.category)

        val lgTv = DeviceHostnameHeuristics.infer("LG-webOSTV")
        assertEquals("LG", lgTv.vendor)
        assertEquals(DeviceCategory.TV, lgTv.category)
    }

    @Test
    fun `infers Computers and Generic Android`() {
        val desktop = DeviceHostnameHeuristics.infer("DESKTOP-8K2A1")
        assertNull(desktop.vendor)
        assertEquals(DeviceCategory.COMPUTER, desktop.category)

        val laptop = DeviceHostnameHeuristics.infer("LAPTOP-F48N")
        assertNull(laptop.vendor)
        assertEquals(DeviceCategory.COMPUTER, laptop.category)

        val genericAndroid = DeviceHostnameHeuristics.infer("android-7c98b21a0f44")
        assertNull(genericAndroid.vendor)
        assertEquals(DeviceCategory.PHONE, genericAndroid.category)
    }

    @Test
    fun `handles null blank and unknown hostnames gracefully`() {
        val nullResult = DeviceHostnameHeuristics.infer(null)
        assertNull(nullResult.vendor)
        assertNull(nullResult.category)

        val blankResult = DeviceHostnameHeuristics.infer("   ")
        assertNull(blankResult.vendor)
        assertNull(blankResult.category)

        val unknown = DeviceHostnameHeuristics.infer("gateway-node-01")
        assertNull(unknown.vendor)
        assertNull(unknown.category)
    }
}
