package es.routerapp.protocol.discovery

import es.routerapp.protocol.RouterClient
import es.routerapp.protocol.pages.Catalog
import es.routerapp.protocol.pages.PageEngine
import java.io.File

/**
 * Reversible add/delete test on port forwarding: adds a DISABLED rule "RT-test" (port 59999 → an unused
 * LAN IP), verifies it, deletes it and verifies it is gone. Args: <pwdFile> [user] [host].
 */
fun main(args: Array<String>) {
    val pwd = File(args[0]).readLines().first().trim()
    val user = args.getOrElse(1) { "user" }
    val host = args.getOrElse(2) { "http://192.168.1.1" }
    val client = RouterClient(host)
    client.login(pwd, user)
    val engine = PageEngine(client)
    val section = Catalog.byId("virtual-servers")!!.sections.first()
    fun mine() = engine.load(section).filter { it.values["description"] == "RT-test" }
    try {
        val before = engine.load(section).size
        println("rules before=$before (leftover test rules=${mine().size})")
        try {
            engine.add(section, mapOf(
                "description" to "RT-test", "externalPort" to "59999", "internalPort" to "59999",
                "internalClient" to "192.168.1.250", "protocol" to "TCP", "enable" to "0",
            ))
            val found = mine()
            println("ADD -> found=${found.size}; attrs=" + found.firstOrNull()?.values?.filterKeys {
                it in setOf("enable", "description", "externalPort", "internalPort", "internalClient", "protocol", "X_TP_ConnName", "stack")
            })
        } finally {
            mine().forEach { engine.delete(section, it) }
            val after = engine.load(section).size
            println("DELETE -> rules after=$after, test rules left=${mine().size} -> " + if (after == before && mine().isEmpty()) "CLEAN" else "NOT CLEAN")
        }
    } finally {
        runCatching { client.logout() }
    }
}
