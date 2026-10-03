package es.routerapp.protocol.discovery

import es.routerapp.protocol.RouterClient
import java.io.File
import kotlin.system.exitProcess

/**
 * READ-ONLY discovery tool. Logs in and downloads the router's authenticated web UI (landing page,
 * menu, page scripts) into `tools/discovery-out/` so that the list of features can be inventoried.
 *
 * It only performs HTTP GET requests (and the login/logout handshake). It never issues
 * set/add/delete/operate calls.
 *
 * Password source (never printed, never stored): env `ROUTER_PASSWORD`, or first line of the file
 * given as argument 1 (default `%USERPROFILE%\router_pwd.txt`).
 *
 * Run: `gradlew :protocol:run --args="C:\path\router_pwd.txt"`
 */
fun main(args: Array<String>) {
    val password = System.getenv("ROUTER_PASSWORD")?.takeIf { it.isNotBlank() }
        ?: File(args.firstOrNull() ?: (System.getProperty("user.home") + File.separator + "router_pwd.txt"))
            .takeIf { it.exists() }?.readLines()?.firstOrNull()?.trim()
        ?: run {
            System.err.println("No password: set ROUTER_PASSWORD or provide a password file.")
            exitProcess(2)
        }
    val out = File("tools/discovery-out").apply { mkdirs() }
    val username = System.getenv("ROUTER_USER")?.takeIf { it.isNotBlank() } ?: args.getOrNull(1) ?: "user"
    val client = RouterClient()

    val busy = client.busy()
    println("Router busy status: $busy")
    if (busy.isLogined) println("NOTE: another admin session is active; logging in will close it.")

    client.login(password, username)
    println("Login OK as '$username'")
    try {
        fun save(name: String, text: String) =
            File(out, name.trim('/').replace(Regex("[^A-Za-z0-9._-]"), "_")).writeText(text)

        val landing = client.rawGet("/")
        save("landing.html", landing)
        val token = client.refreshToken()
        println("Landing page saved (${landing.length} chars); token ${if (token != null) "found" else "NOT found"}")

        val menu = runCatching { client.rawGet("/frame/menu.cgi", decrypt = true) }
        menu.onSuccess { save("menu.txt", it); println("menu.cgi saved (${it.length} chars)") }
            .onFailure { println("menu.cgi failed: ${it.message}") }

        // Everything the web UI loads: landing scripts, frame fragments, every page of the menu list
        // (served from ./main/) and the helper scripts named in that list.
        val menuNames = Regex("\"([A-Za-z0-9_.\\-]+\\.(?:htm|js))\"").findAll(menu.getOrDefault(""))
            .map { it.groupValues[1] }.distinct().toList()
        val fromLanding = Regex("""(?:src|href)\s*=\s*["']([^"'#]+\.(?:js|htm|html))["']""", RegexOption.IGNORE_CASE)
            .findAll(landing).map { it.groupValues[1] }
            .filter { !it.startsWith("http") }
            .map { "/" + it.removePrefix("../").removePrefix("./").removePrefix("/") }.toList()
        val paths = (fromLanding +
            listOf("/frame/top.htm", "/frame/menu.htm", "/frame/bot.htm") +
            menuNames.filter { it.endsWith(".htm") }.map { "/main/$it" } +
            menuNames.filter { it.endsWith(".js") }.map { "/js/$it" } +
            listOf("/js/voice.js", "/js/ispVoIP.js", "/js/ispMgr.js", "/js/isp3g.js", "/js/isp.js", "/js/section.js", "/js/section-addition.js"))
            .distinct()
        println("${paths.size} resources to fetch")
        var ok = 0
        for (path in paths) {
            runCatching { client.rawGet(path) }
                .onSuccess { save(path, it); ok++ }
                .onFailure { println("  skip $path: ${it.message}") }
        }
        println("Saved $ok/${paths.size}. Output in ${out.absolutePath}")
    } finally {
        client.logout()
        println("Logged out")
    }
}
