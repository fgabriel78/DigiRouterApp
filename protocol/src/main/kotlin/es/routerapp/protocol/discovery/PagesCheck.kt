package es.routerapp.protocol.discovery

import es.routerapp.protocol.RouterClient
import es.routerapp.protocol.RouterException
import es.routerapp.protocol.pages.Catalog
import es.routerapp.protocol.pages.FieldType
import es.routerapp.protocol.pages.PageEngine
import java.io.File

/**
 * Read-only check: loads every catalog section from the real router and reports declared fields
 * that are missing from the returned attributes. Never writes. Args: <pwdFile> [user] [host].
 */
fun main(args: Array<String>) {
    val pwd = File(args[0]).readLines().first().trim()
    val user = args.getOrElse(1) { "user" }
    val host = args.getOrElse(2) { "http://192.168.1.1" }
    val client = RouterClient(host)
    client.login(pwd, user)
    val engine = PageEngine(client)
    var problems = 0
    try {
        for (page in Catalog.visiblePages(user).filter { !it.custom }) {
            println("# ${page.title}")
            for (s in page.sections) {
                try {
                    val items = engine.load(s)
                    val keys = items.flatMap { it.values.keys }.toSet()
                    val missing = s.fields.filter { it.type != FieldType.Info || true }.map { it.key }.filter { items.isNotEmpty() && it !in keys }
                    println("  - ${s.id} (${s.oid}): ${items.size} item(s)" + if (missing.isNotEmpty()) "  MISSING $missing" else "")
                    if (missing.isNotEmpty()) problems++
                } catch (e: RouterException) {
                    println("  - ${s.id} (${s.oid}): ERROR ${e.code}")
                    problems++
                }
            }
        }
        println("activeWan=" + engine.activeWanName())
    } finally {
        runCatching { client.logout() }
    }
    println(if (problems == 0) "ALL OK" else "$problems section(s) with problems")
}
