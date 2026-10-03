package es.routerapp.protocol.discovery

import es.routerapp.protocol.RouterClient
import es.routerapp.protocol.pages.Catalog
import es.routerapp.protocol.pages.PageEngine
import es.routerapp.protocol.pages.Section
import java.io.File

/**
 * Writes that cannot cut connectivity, each one restored afterwards and verified by reading back.
 * Never prints secrets. Args: <pwdFile> [user] [host].
 */
fun main(args: Array<String>) {
    val pwd = File(args[0]).readLines().first().trim()
    val user = args.getOrElse(1) { "user" }
    val host = args.getOrElse(2) { "http://192.168.1.1" }
    val client = RouterClient(host)
    client.login(pwd, user)
    val engine = PageEngine(client)
    val results = linkedMapOf<String, String>()

    fun section(page: String, id: String): Section = Catalog.byId(page)!!.sections.first { it.id == id }

    fun test(name: String, body: () -> String) {
        println("== $name")
        results[name] = try { body().also { println("   $it") } } catch (e: Throwable) { "FAIL: ${e::class.simpleName} ${e.message}".also { println("   $it") } }
    }

    /** Changes [key] to [temp] on instance [idx], verifies, restores, verifies. */
    fun flip(page: String, secId: String, key: String, temp0: String, idx: Int = 0, guard: ((Map<String, String>) -> Boolean)? = null) {
        val sec = section(page, secId)
        test("$page/$secId.$key") {
            val orig = engine.load(sec)[idx]
            if (guard != null && !guard(orig.values)) return@test "SKIPPED (guard)"
            val original = orig.values[key].orEmpty()
            val temp = if (temp0 == "~") (if (original == "1") "0" else "1") else temp0
            var step = "?"
            try {
                engine.save(sec, orig, orig.values + (key to temp))
                val mid = engine.load(sec)[idx].values[key]
                step = if (mid == temp) "write OK" else "write MISMATCH (got '$mid')"
            } finally {
                val cur = engine.load(sec)[idx]
                engine.save(sec, cur, cur.values + (key to original))
                val after = engine.load(sec)[idx].values[key]
                step += if (after == original) ", restored OK" else ", RESTORE FAILED (expected '$original' got '$after')"
            }
            if (step.contains("MISMATCH") || step.contains("FAILED")) "FAIL: $step ('$original' → '$temp')" else "PASS: $step ('$original' → '$temp')"
        }
    }

    try {
        // ---- add / delete
        test("dhcp-static add/delete") {
            val sec = section("dhcp-static", "static")
            val before = engine.load(sec).size
            val mac = "02:00:00:00:00:01"
            fun mine() = engine.load(sec).filter { it.values["chaddr"].equals(mac, true) }
            var msg: String
            try {
                engine.add(sec, mapOf("chaddr" to mac, "yiaddr" to "192.168.1.200"))
                val f = mine()
                msg = "added=${f.size} ip=${f.firstOrNull()?.values?.get("yiaddr")} enable=${f.firstOrNull()?.values?.get("enable")}"
            } finally {
                mine().forEach { engine.delete(sec, it) }
            }
            val after = engine.load(sec).size
            if (after == before && mine().isEmpty() && msg.startsWith("added=1")) "PASS: $msg; clean" else "FAIL: $msg; before=$before after=$after"
        }

        test("port-trigger add/delete") {
            val sec = section("port-trigger", "pt")
            val before = engine.load(sec).size
            fun mine() = engine.load(sec).filter { it.values["applicationName"] == "RT-test" }
            var msg: String
            try {
                engine.add(sec, mapOf("applicationName" to "RT-test", "triggerPort" to "59998", "triggerProtocol" to "TCP",
                    "openPort" to "59998", "openProtocol" to "TCP", "enable" to "0"))
                msg = "added=${mine().size}"
            } finally {
                mine().forEach { engine.delete(sec, it) }
            }
            val after = engine.load(sec).size
            if (after == before && mine().isEmpty() && msg == "added=1") "PASS: $msg; clean" else "FAIL: $msg; before=$before after=$after"
        }

        // ---- guest security (network is disabled): mode + password, restored with raw values
        test("guest security mode+psk") {
            val sec = section("guest", "guest")
            val orig = engine.load(sec)[0]
            if (orig.values["guestEnable"] != "0") return@test "SKIPPED (guest network enabled)"
            val mode = orig.values["guestModeEnabled"].orEmpty()
            val enc = orig.values["guestWPAWPA2EncryptionMode"].orEmpty()
            val psk = orig.values["guestPSK"].orEmpty()
            val target = if (mode == "WPA2-Personal") "WPA2-WPA3-Personal" else "WPA2-Personal"
            println("   original mode=$mode enc=$enc psk=<${psk.length} chars>")
            var step: String
            try {
                engine.save(sec, orig, orig.values + mapOf("guestModeEnabled" to target, "guestPSK" to "RT-test-12345"))
                val mid = engine.load(sec)[0].values
                step = "write " + if (mid["guestModeEnabled"] == target && mid["guestPSK"] == "RT-test-12345") "OK (enc=${mid["guestWPAWPA2EncryptionMode"]})" else "MISMATCH"
            } finally {
                val restore = linkedMapOf("guestModeEnabled" to mode, "guestWPAWPA2EncryptionMode" to enc)
                if (psk.isNotEmpty()) restore["guestPSK"] = psk
                client.set("DEV2_ADT_WIFI_COMMON", restore, orig.stack)
            }
            val after = engine.load(sec)[0].values
            val ok = after["guestModeEnabled"] == mode && after["guestWPAWPA2EncryptionMode"] == enc &&
                (psk.isEmpty() || after["guestPSK"] == psk)
            step += if (ok) ", restored OK" else ", RESTORE FAILED (mode=${after["guestModeEnabled"]} enc=${after["guestWPAWPA2EncryptionMode"]})"
            if (step.contains("MISMATCH") || step.contains("FAILED")) "FAIL: $step" else "PASS: $step"
        }

        // ---- single-value flips ("~" = invert switch)
        flip("guest", "guest", "guestIsolationEnable", "~", guard = { it["guestEnable"] == "0" })
        flip("guest", "guest", "guestSSIDAdvertise", "~", guard = { it["guestEnable"] == "0" })
        flip("mssid", "mssid1", "mssid1SSID", "RT-test", guard = { it["mssid1Enable"] == "0" })
        flip("alg", "alg", "h323Alg", "~")
        flip("ddos", "ddos", "forbidLanPing", "~")
        flip("dmz", "dmz", "IPAddress", "192.168.1.250", guard = { it["enable"] == "0" })
        flip("time", "time", "NTPServer2", "ntp.rt-test.invalid")
        flip("led", "led", "enable", "~")
        flip("led", "led", "startTime", "1380")
        flip("reboot", "sched", "hours", "4", guard = { it["enable"] == "0" })
        flip("stats", "stat", "interval", "15")
        flip("storage", "smb", "anonymous", "~")
        flip("storage", "dlna", "serverName", "RT-test")
        flip("lan", "lan", "DHCPv4LeaseTime", "43200")

        println()
        println("===== SUMMARY =====")
        results.forEach { (k, v) -> println("${v.substringBefore(':').padEnd(8)} $k") }
    } catch (e: Throwable) {
        println("ABORTED: $e")
    } finally {
        runCatching { client.logout() }
    }
}
