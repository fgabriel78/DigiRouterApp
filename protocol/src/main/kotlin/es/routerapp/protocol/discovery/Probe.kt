package es.routerapp.protocol.discovery

import es.routerapp.protocol.RouterClient
import java.io.File
import kotlin.system.exitProcess

/**
 * READ-ONLY probe: logs in and reads data-model objects with `go` (single object) and `gl`
 * (list) operations only. Results are stored in `tools/discovery-out/probe/<OID>.json`.
 *
 * Args: `<passwordFile> <username> <OID[,OID...]>`
 */
fun main(args: Array<String>) {
    if (args.size < 3) {
        System.err.println("usage: <passwordFile> <username> <OID,OID,...>")
        exitProcess(2)
    }
    val password = File(args[0]).readLines().first().trim()
    val username = args[1]
    val oids = args[2].split(',').map { it.trim() }.filter { it.isNotEmpty() }
    val out = File("tools/discovery-out/probe").apply { mkdirs() }

    val client = RouterClient()
    client.login(password, username)
    client.refreshToken()
    println("Login OK as '$username'")
    try {
        for (oid in oids) {
            val single = runCatching { client.get(oid) }
            val list = runCatching { client.getList(oid) }
            val result = when {
                single.isSuccess && single.getOrNull().toString() != "{}" -> "go" to single.getOrThrow().toString()
                list.isSuccess -> "gl" to list.getOrThrow().toString()
                single.isSuccess -> "go" to single.getOrThrow().toString()
                else -> "err" to "go: ${single.exceptionOrNull()?.message}; gl: ${list.exceptionOrNull()?.message}"
            }
            File(out, "$oid.json").writeText(result.second)
            println("[$oid] ${result.first}: ${result.second.take(500)}")
        }
    } finally {
        client.logout()
        println("Logged out")
    }
}
