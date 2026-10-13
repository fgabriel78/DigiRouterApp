package es.digirouter.app

import es.routerapp.protocol.pages.ConnectedDevice
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class NetworkFingerprintScannerTest {

    private lateinit var prefs: FakeSharedPreferences
    private lateinit var store: DeviceMetadataStore

    @Before
    fun setUp() {
        prefs = FakeSharedPreferences()
        store = SharedPrefsDeviceMetadataStore(prefs)
    }

    @Test
    fun `scanFlow emits cached fingerprints without making network requests`() = runBlocking {
        val cachedFp = DeviceFingerprint(
            exactModel = "PlayStation 5",
            vendor = "Sony",
            category = DeviceCategory.CONSOLE,
            discoveryMethod = "UPnP",
        )
        store.saveFingerprint("AA:BB:CC:DD:EE:01", cachedFp)

        val devices = listOf(
            ConnectedDevice(
                name = "ps5-livingroom",
                ip = "192.168.1.50",
                mac = "AA:BB:CC:DD:EE:01",
                wifi = true,
                level = 5,
                rate = 866,
                active = true,
            ),
        )

        val scanner = NetworkFingerprintScanner(
            context = null,
            metadataStore = store,
        )

        val results = scanner.scanFlow(devices).toList()
        assertEquals(1, results.size)
        assertEquals("AA:BB:CC:DD:EE:01", results[0].first)
        assertEquals("PlayStation 5", results[0].second.exactModel)
        assertEquals("Sony", results[0].second.vendor)
        assertEquals(DeviceCategory.CONSOLE, results[0].second.category)
    }

    @Test
    fun `scanFlow discovers device via HTTP probe when mock client returns descriptor`() = runBlocking {
        val xmlDescriptor = """
            <?xml version="1.0" encoding="utf-8"?>
            <root xmlns="urn:schemas-upnp-org:device-1-0">
                <device>
                    <friendlyName>LG OLED55C26LD</friendlyName>
                    <manufacturer>LG Electronics</manufacturer>
                    <modelName>LG TV</modelName>
                    <modelNumber>OLED55C26LD</modelNumber>
                </device>
            </root>
        """.trimIndent()

        val mockClient = OkHttpClient.Builder()
            .addInterceptor(
                Interceptor { chain ->
                    val request = chain.request()
                    if (request.url.toString().contains("192.168.1.60/description.xml")) {
                        Response.Builder()
                            .request(request)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(xmlDescriptor.toResponseBody("application/xml".toMediaType()))
                            .build()
                    } else {
                        Response.Builder()
                            .request(request)
                            .protocol(Protocol.HTTP_1_1)
                            .code(404)
                            .message("Not Found")
                            .body("".toResponseBody("text/plain".toMediaType()))
                            .build()
                    }
                },
            )
            .build()

        val scanner = NetworkFingerprintScanner(
            context = null,
            metadataStore = store,
            okHttpClient = mockClient,
            multicastBurstMs = 0L,
        )

        val devices = listOf(
            ConnectedDevice(
                name = "lg-webos-tv",
                ip = "192.168.1.60",
                mac = "11:22:33:44:55:66",
                wifi = true,
                level = 4,
                rate = 400,
                active = true,
            ),
        )

        val results = scanner.scanFlow(devices).toList()
        assertEquals(1, results.size)
        assertEquals("11:22:33:44:55:66", results[0].first)
        assertEquals("LG OLED55C2", results[0].second.exactModel)
        assertEquals("LG", results[0].second.vendor)
        assertEquals(DeviceCategory.TV, results[0].second.category)

        // Verify it was persisted to store
        val storedFp = store.getFingerprint("11:22:33:44:55:66")
        assertNotNull(storedFp)
        assertEquals("LG OLED55C2", storedFp?.exactModel)
    }
}
