package es.routerapp.protocol.discovery

import es.routerapp.protocol.RouterClient
import es.routerapp.protocol.RouterException
import es.routerapp.protocol.pages.ConnectedDevices
import es.routerapp.protocol.pages.PageEngine
import java.io.File

/** Read-only: runs each read the dashboard/devices screens do and reports which one fails. */
fun main(args: Array<String>) {
    val pwd = File(args[0]).readLines().first().trim()
    val client = RouterClient(args.getOrElse(2) { "http://192.168.1.1" })
    client.login(pwd, args.getOrElse(1) { "user" })
    val engine = PageEngine(client)
    fun step(name: String, f: () -> Any) =
        println(name + ": " + try { "OK " + f() } catch (e: RouterException) { "ERROR ${e.code}" } catch (e: Throwable) { "FAIL $e" })
    try {
        step("go DEV2_DEV_INFO") { PageEngine.toInstances(client.get("DEV2_DEV_INFO")).size }
        step("gl DEV2_ADT_WAN") { PageEngine.toInstances(client.getList("DEV2_ADT_WAN")).size }
        step("activeWan") { engine.activeWanName() }
        step("go DEV2_GPON_INTF_STATS") { PageEngine.toInstances(client.get("DEV2_GPON_INTF_STATS")).size }
        step("gl DEV2_GPON_INTF_STATS") { PageEngine.toInstances(client.getList("DEV2_GPON_INTF_STATS")).size }
        step("gl DEV2_HOST_ENTRY (no longer used by the app; 71011 on FW 3.2.1)") { PageEngine.toInstances(client.getList("DEV2_HOST_ENTRY")).size }
        step("ConnectedDevices.load (Devices screen + Summary counter)") { ConnectedDevices.load(client).let { l -> "${l.size} devices, ${l.count { it.active }} active" } }
        step("gl DEV2_ADT_WIFI_COMMON") { PageEngine.toInstances(client.getList("DEV2_ADT_WIFI_COMMON")).size }
        step("gl DEV2_WIFI_APDEV_ASSOCDEV") { PageEngine.toInstances(client.getList("DEV2_WIFI_APDEV_ASSOCDEV")).size }
    } finally {
        runCatching { client.logout() }
    }
}
