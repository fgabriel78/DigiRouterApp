package es.routerapp.protocol.discovery

import es.routerapp.protocol.RouterClient
import es.routerapp.protocol.pages.Catalog
import es.routerapp.protocol.pages.PageEngine
import java.io.File

/**
 * Reversible write test: renames the guest SSID of the first band, reads it back, then restores
 * the original value (always, via finally) and verifies. Args: <pwdFile> [user] [host].
 */
fun main(args: Array<String>) {
    val pwd = File(args[0]).readLines().first().trim()
    val user = args.getOrElse(1) { "user" }
    val host = args.getOrElse(2) { "http://192.168.1.1" }
    val client = RouterClient(host)
    client.login(pwd, user)
    val engine = PageEngine(client)
    val section = Catalog.byId("guest")!!.sections.first()
    val key = "guestSSID"
    try {
        val before = engine.load(section).first()
        val original = before.values[key].orEmpty()
        println("band=${before.values["band"]} stack=${before.stack} guestEnable=${before.values["guestEnable"]} $key='$original'")
        val temp = "RT-test"
        var restored = false
        try {
            val sent = engine.save(section, before, before.values + (key to temp))
            println("WRITE sent=$sent")
            val mid = engine.load(section).first().values[key]
            println("READBACK after write: '$mid' -> " + if (mid == temp) "OK" else "MISMATCH")
        } finally {
            val cur = engine.load(section).first()
            val sent = engine.save(section, cur, cur.values + (key to original))
            println("REVERT sent=$sent")
            val after = engine.load(section).first().values[key]
            restored = after == original
            println("READBACK after revert: '$after' -> " + if (restored) "RESTORED" else "NOT RESTORED")
        }
    } finally {
        runCatching { client.logout() }
    }
}
