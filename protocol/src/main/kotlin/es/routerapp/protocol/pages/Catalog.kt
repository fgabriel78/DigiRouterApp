package es.routerapp.protocol.pages

import es.routerapp.protocol.i18n.tr

/**
 * Catalog of every configuration screen implemented in the app. Attribute names come from live reads
 * of the TP-Link XGB430v Pro (DIGI). Only pages visible to the `user` role are included; admin-only
 * pages (WAN, QoS, VPN, firmware, logs...) are intentionally absent.
 *
 * All user-visible texts are English source texts. The UI translates them with [tr] when it draws them
 * (see `SpanishTexts`), so a text added here must also be added to the dictionaries.
 */
object Catalog {

    // ---------- helpers ----------
    private fun sw(key: String, label: String, help: String? = null, visibleIf: ((Values) -> Boolean)? = null) =
        Field(key, label, FieldType.Switch, help, visibleIf)

    private fun txt(key: String, label: String, min: Int = 0, max: Int = 64, help: String? = null, visibleIf: ((Values) -> Boolean)? = null) =
        Field(key, label, FieldType.Text(min, max), help, visibleIf)

    private fun pwd(key: String, label: String = "Password", min: Int = 8, max: Int = 63, visibleIf: ((Values) -> Boolean)? = null) =
        Field(key, label, FieldType.Secret(min, max), null, visibleIf)

    private fun num(key: String, label: String, min: Long, max: Long, unit: String? = null, help: String? = null, visibleIf: ((Values) -> Boolean)? = null) =
        Field(key, label, FieldType.Number(min, max, unit), help, visibleIf)

    private fun ip(key: String, label: String, help: String? = null, visibleIf: ((Values) -> Boolean)? = null) =
        Field(key, label, FieldType.Ipv4, help, visibleIf)

    private fun info(key: String, label: String) = Field(key, label, FieldType.Info)

    private fun choice(key: String, label: String, vararg o: Pair<String, String>, visibleIf: ((Values) -> Boolean)? = null) =
        Field(key, label, FieldType.Choice { o.map { (v, l) -> Option(v, l) } }, null, visibleIf)

    private fun choiceBy(key: String, label: String, visibleIf: ((Values) -> Boolean)? = null, f: (Values) -> List<Option>) =
        Field(key, label, FieldType.Choice(f), null, visibleIf)

    private val is24: (Values) -> Boolean = { it["band"]?.startsWith("2") == true }

    /** Title of a per-band card; evaluated when drawn, so it follows the current language. */
    private fun bandTitle(v: Values) = when {
        v["band"]?.startsWith("2") == true -> tr("2.4 GHz band")
        v["band"]?.startsWith("5") == true -> tr("5 GHz band")
        v["band"]?.startsWith("6") == true -> tr("6 GHz band")
        else -> v["band"] ?: tr("Band")
    }

    private val securityModes = arrayOf(
        "None" to "No password (open)",
        "WPA2-Personal" to "WPA2-Personal",
        "WPA2-WPA3-Personal" to "WPA2/WPA3-Personal",
        "WPA3-Personal" to "WPA3-Personal",
        "WPA-WPA2-Personal" to "WPA/WPA2-Personal",
        "OWE" to "OWE (encrypted open)",
    )

    /** When switching from an open network to an encrypted one, the web UI also forces AES. */
    private fun encryptionHook(modeKey: String, encKey: String): (Values, Values, MutableMap<String, String>) -> Unit =
        { original, _, changed ->
            val newMode = changed[modeKey]
            if (newMode != null && original[modeKey] == "None" && newMode != "None") changed[encKey] = "AES"
        }

    private fun channels(v: Values): List<Option> {
        val list = if (is24(v)) (1..13).toList()
        else listOf(36, 40, 44, 48, 52, 56, 60, 64, 100, 104, 108, 112, 116, 132, 136, 140)
        return list.map { Option(it.toString(), it.toString()) }
    }

    private val timeZones: List<Option> = run {
        val offs = listOf(-12.0, -11.0, -10.0, -9.0, -8.0, -7.0, -6.0, -5.0, -4.0, -3.5, -3.0, -2.0, -1.0, 0.0, 1.0, 2.0, 3.0, 3.5,
            4.0, 4.5, 5.0, 5.5, 5.75, 6.0, 7.0, 8.0, 9.0, 9.5, 10.0, 11.0, 12.0, 13.0, 14.0)
        offs.map {
            val sign = if (it < 0) "-" else "+"
            val a = kotlin.math.abs(it)
            val h = a.toInt()
            val m = ((a - h) * 60).toInt()
            val s = "$sign%02d:%02d".format(h, m)
            Option(s, "GMT$s")
        }
    }

    // ---------- Wi-Fi ----------
    private val wifiMain = Page(
        id = "wifi", title = "Main Wi-Fi", group = Group.WIFI,
        description = "Name, password and security of each band",
        sections = listOf(
            Section(
                id = "primary", title = "Wi-Fi networks", oid = "DEV2_ADT_WIFI_COMMON", multi = true, itemTitle = ::bandTitle,
                fields = listOf(
                    sw("primaryEnable", "Enabled"),
                    txt("primarySSID", "Network name (SSID)", 1, 32),
                    sw("primarySSIDAdvertise", "Broadcast SSID (visible)"),
                    choice("primaryModeEnabled", "Security", *securityModes),
                    pwd("primaryPSK", visibleIf = { it["primaryModeEnabled"] !in setOf("None", "OWE") }),
                    info("primaryStatus", "Status"),
                ),
                saveHook = encryptionHook("primaryModeEnabled", "primaryWPAWPA2EncryptionMode"),
                saveWarning = "If you change the name or password of the network you are connected to, you will lose the connection to the router.",
            ),
            Section(
                id = "mlo", title = "Wi-Fi 7 MLO (same network on both bands)", oid = "DEV2_ADT_WIFI_COMMON", multi = true,
                applyToAll = true,
                fields = listOf(
                    sw("mloEnable", "Enable MLO"),
                    txt("mloSSID", "Network name (SSID)", 1, 32, visibleIf = { it["mloEnable"] == "1" }),
                    sw("mloSSIDAdvertise", "Broadcast SSID", visibleIf = { it["mloEnable"] == "1" }),
                    choice("mloModeEnabled", "Security", *securityModes.filter { it.first != "None" && it.first != "OWE" }.toTypedArray(),
                        visibleIf = { it["mloEnable"] == "1" }),
                    pwd("mloPSK", visibleIf = { it["mloEnable"] == "1" }),
                ),
                afterSave = listOf("ACT_WIFI_RELOAD_MLO"),
                saveWarning = "MLO changes restart the Wi-Fi radio and may disconnect you for a few seconds.",
            ),
        ),
    )

    private val wifiRadio = Page(
        id = "wifi-radio", title = "Radio settings", group = Group.WIFI,
        description = "Standard, channel width, channel and power",
        sections = listOf(
            Section(
                id = "radio", title = "Radio", oid = "DEV2_ADT_WIFI_COMMON", multi = true, itemTitle = ::bandTitle,
                fields = listOf(
                    choiceBy("standard", "Mode") {
                        if (is24(it)) listOf(Option("bgn", "802.11 b/g/n"), Option("bgnax", "802.11 b/g/n/ax (Wi-Fi 6)"),
                            Option("bgnaxbe", "802.11 b/g/n/ax/be (Wi-Fi 7)"))
                        else listOf(Option("anac", "802.11 a/n/ac"), Option("anacax", "802.11 a/n/ac/ax (Wi-Fi 6)"),
                            Option("anacaxbe", "802.11 a/n/ac/ax/be (Wi-Fi 7)"))
                    },
                    choiceBy("bandwidth", "Channel width") {
                        val l = if (is24(it)) listOf("Auto", "20MHz", "40MHz") else listOf("Auto", "20MHz", "40MHz", "80MHz", "160MHz")
                        l.map { v -> Option(v, if (v == "Auto") "Automatic" else v.replace("MHz", " MHz")) }
                    },
                    sw("autoChannel", "Automatic channel"),
                    choiceBy("channel", "Channel", visibleIf = { it["autoChannel"] == "0" }, f = ::channels),
                    choice("transmitPower", "Transmit power", "25" to "Low", "50" to "Medium", "100" to "High"),
                ),
                saveWarning = "Changing the radio configuration may cut the Wi-Fi connection for a few seconds.",
            ),
        ),
    )

    private val wifiAdvanced = Page(
        id = "wifi-adv", title = "Advanced Wi-Fi", group = Group.WIFI,
        description = "Beamforming, MU-MIMO, OFDMA, TWT, intervals and band steering",
        sections = listOf(
            Section(
                id = "adv", title = "Advanced options", oid = "DEV2_ADT_WIFI_COMMON", multi = true, itemTitle = ::bandTitle,
                fields = listOf(
                    sw("primaryTxBFEnable", "Beamforming"),
                    sw("primaryMUMIMOEnable", "MU-MIMO"),
                    sw("primaryOFDMAEnable", "OFDMA"),
                    sw("primaryTWTEnable", "Target Wake Time (TWT)"),
                    sw("primaryBSSColorEnable", "BSS Color"),
                    sw("primaryWMMEnable", "WMM"),
                    sw("primaryIsolationEnable", "Client isolation (AP isolation)"),
                    num("beaconInterval", "Beacon interval", 40, 1000, "ms"),
                    num("DTIMPeriod", "DTIM interval", 1, 255),
                    num("RTSThreshold", "RTS threshold", 1, 2347),
                    num("fragmentThreshold", "Fragmentation threshold", 256, 2346),
                ),
            ),
            Section(
                id = "steering", title = "Band steering", oid = "DEV2_WIFI_BANDSTEERING",
                fields = listOf(sw("enable", "Band steering (move clients to 5 GHz)")),
            ),
        ),
    )

    private val wps = Page(
        id = "wps", title = "WPS", group = Group.WIFI,
        sections = listOf(
            Section(
                id = "wps", title = "WPS", oid = "DEV2_ADT_WIFI_COMMON", multi = true, itemTitle = ::bandTitle,
                fields = listOf(sw("WPSEnable", "Enable WPS"), info("WPSState", "Status")),
            ),
        ),
    )

    private val guest = Page(
        id = "guest", title = "Guest network", group = Group.WIFI,
        description = "Isolated Wi-Fi for visitors",
        sections = listOf(
            Section(
                id = "guest", title = "Guests", oid = "DEV2_ADT_WIFI_COMMON", multi = true, itemTitle = ::bandTitle,
                fields = listOf(
                    sw("guestEnable", "Enabled"),
                    txt("guestSSID", "Network name (SSID)", 1, 32),
                    sw("guestSSIDAdvertise", "Broadcast SSID"),
                    choice("guestModeEnabled", "Security", *securityModes.filter { it.first != "OWE" }.toTypedArray()),
                    pwd("guestPSK", visibleIf = { it["guestModeEnabled"] != "None" }),
                    sw("guestIsolationEnable", "Isolate guests from each other"),
                    sw("guestLANAccessEnable", "Allow access to the local network"),
                ),
                saveHook = encryptionHook("guestModeEnabled", "guestWPAWPA2EncryptionMode"),
            ),
        ),
    )

    private fun mssid(n: Int) = Section(
        id = "mssid$n", title = "Additional network $n", oid = "DEV2_ADT_WIFI_COMMON", multi = true, itemTitle = ::bandTitle,
        fields = listOf(
            sw("mssid${n}Enable", "Enabled"),
            txt("mssid${n}SSID", "Network name (SSID)", 1, 32),
            sw("mssid${n}SSIDAdvertise", "Broadcast SSID"),
            choice("mssid${n}ModeEnabled", "Security", *securityModes.filter { it.first != "OWE" }.toTypedArray()),
            pwd("mssid${n}PSK", visibleIf = { it["mssid${n}ModeEnabled"] != "None" }),
            sw("mssid${n}IsolationEnable", "Isolate clients"),
            sw("mssid${n}LANAccessEnable", "Allow access to the local network"),
        ),
        saveHook = encryptionHook("mssid${n}ModeEnabled", "mssid${n}WPAWPA2EncryptionMode"),
    )

    private val multiSsid = Page(
        id = "mssid", title = "Additional networks (multi-SSID)", group = Group.WIFI,
        sections = listOf(mssid(1), mssid(2)),
    )

    // ---------- Local network ----------
    private val lan = Page(
        id = "lan", title = "LAN and DHCP", group = Group.NETWORK,
        description = "Router address, range and DHCP server options",
        sections = listOf(
            Section(
                id = "lan", title = "Local network", oid = "DEV2_ADT_LAN", multi = true, itemTitle = { tr("Local network") },
                fields = listOf(
                    ip("IPAddress", "Router IP address"),
                    ip("IPSubnetMask", "Subnet mask"),
                    sw("IGMPSnoopEnabled", "IGMP snooping"),
                    sw("DHCPv4Enable", "DHCP server"),
                    ip("DHCPv4MinIPAddress", "Start IP", visibleIf = { it["DHCPv4Enable"] == "1" }),
                    ip("DHCPv4MaxIPAddress", "End IP", visibleIf = { it["DHCPv4Enable"] == "1" }),
                    num("DHCPv4LeaseTime", "Lease time", 120, 864000, "s", visibleIf = { it["DHCPv4Enable"] == "1" }),
                    ip("DHCPv4IPRouters", "Default gateway", visibleIf = { it["DHCPv4Enable"] == "1" }),
                    txt("DHCPv4DomainName", "Domain name", 0, 64, visibleIf = { it["DHCPv4Enable"] == "1" }),
                    txt("DHCPv4DNSServers", "DNS servers (comma separated)", 0, 64, visibleIf = { it["DHCPv4Enable"] == "1" }),
                ),
                saveWarning = "Changing the IP or the DHCP range may leave you without access to the router until your device renews its connection.",
            ),
        ),
    )

    private val dhcpReservations = Page(
        id = "dhcp-static", title = "DHCP reservations", group = Group.NETWORK,
        description = "Always assign the same IP to a device",
        sections = listOf(
            Section(
                id = "static", title = "Reservations", oid = "DEV2_DHCPV4_POOL_STATICADDR", multi = true,
                itemTitle = { "${it["yiaddr"]}  ·  ${it["chaddr"]}" },
                fields = listOf(
                    sw("enable", "Enabled"),
                    Field("chaddr", "MAC address", FieldType.Mac),
                    ip("yiaddr", "Reserved IP address"),
                ),
                deletable = true,
                add = AddSpec(
                    title = "New reservation",
                    fields = listOf(Field("chaddr", "MAC address", FieldType.Mac), ip("yiaddr", "Reserved IP address")),
                    defaults = mapOf("enable" to "1"),
                    parent = ParentRef("DEV2_DHCPV4_SERVER_POOL", "X_TP_MainPool", "1"),
                ),
            ),
        ),
    )

    /**
     * The router only accepts DDNS changes as a "login" (enable=1, login=1 and all account fields in one
     * write) and "logout" (login=0); editing fields while disabled is rejected (error 5225).
     */
    private fun ddnsHook(): (Values, Values, MutableMap<String, String>) -> Unit = { original, edited, changed ->
        if (edited["enable"] == "1") {
            changed.clear()
            changed["enable"] = "1"
            for (k in listOf("userName", "password", "userDomain")) changed[k] = edited[k].orEmpty()
            changed["login"] = "1"
        } else if (original["enable"] == "1") {
            changed.clear()
            changed["login"] = "0"
        } else {
            error(tr("Enable the service to save the account data"))
        }
    }

    private fun ddnsSection(id: String, title: String, oid: String) = Section(
        id = id, title = title, oid = oid,
        fields = listOf(
            sw("enable", "Connect"),
            txt("userName", "Username", 1, 64),
            pwd("password", min = 1, max = 64),
            txt("userDomain", "Domain", 1, 64),
            info("state", "Status"),
        ),
        saveHook = ddnsHook(),
        saveWarning = "The router will try to connect to the service. If you already use another dynamic DNS provider, disconnect it first.",
    )

    private val ddns = Page(
        id = "ddns", title = "Dynamic DNS", group = Group.NETWORK,
        sections = listOf(
            ddnsSection("dyndns", "DynDNS", "DEV2_DYN_DNS_CFG"),
            ddnsSection("noip", "No-IP", "DEV2_NOIP_DNS_CFG"),
        ),
    )

    // ---------- Ports and NAT ----------
    private val protocols = arrayOf("TCP" to "TCP", "UDP" to "UDP", "TCP or UDP" to "TCP and UDP")

    private val virtualServers = Page(
        id = "virtual-servers", title = "Virtual servers", group = Group.NAT,
        description = "Forward ports to devices on your network",
        sections = listOf(
            Section(
                id = "vs", title = "Rules", oid = "DEV2_PORTMAPPING", multi = true,
                itemTitle = {
                    val d = it["description"].orEmpty()
                    if (d.isBlank()) tr("Rule") else tr("{0} ({1})", d, it["externalPort"].orEmpty())
                },
                fields = listOf(
                    sw("enable", "Enabled"),
                    txt("description", "Description", 0, 32),
                    num("externalPort", "External port", 1, 65535),
                    num("internalPort", "Internal port", 1, 65535),
                    ip("internalClient", "Internal IP"),
                    choice("protocol", "Protocol", *protocols),
                ),
                deletable = true,
                add = AddSpec(
                    title = "New rule",
                    fields = listOf(
                        txt("description", "Description", 0, 32),
                        num("externalPort", "External port", 1, 65535),
                        num("internalPort", "Internal port", 1, 65535),
                        ip("internalClient", "Internal IP"),
                        choice("protocol", "Protocol", *protocols),
                    ),
                    defaults = mapOf(
                        "enable" to "1", "X_TP_AddrType" to "0", "externalPortEndRange" to "0",
                        "X_TP_InternalPortEndRange" to "0", "X_TP_ConnName" to "@activeWan", "protocol" to "TCP",
                    ),
                ),
            ),
        ),
    )

    private val portTrigger = Page(
        id = "port-trigger", title = "Port triggering", group = Group.NAT,
        sections = listOf(
            Section(
                id = "pt", title = "Rules", oid = "DEV2_PORTTRIGGERING", multi = true,
                itemTitle = { it["applicationName"].orEmpty().ifBlank { tr("Rule") } },
                fields = listOf(
                    sw("enable", "Enabled"),
                    txt("applicationName", "Application", 1, 32),
                    num("triggerPort", "Trigger port", 1, 65535),
                    choice("triggerProtocol", "Trigger protocol", *protocols),
                    txt("openPort", "Ports to open", 1, 64, help = "A single port or a range (e.g. 5000-5010)"),
                    choice("openProtocol", "Open protocol", *protocols),
                ),
                deletable = true,
                add = AddSpec(
                    title = "New rule",
                    fields = listOf(
                        txt("applicationName", "Application", 1, 32),
                        num("triggerPort", "Trigger port", 1, 65535),
                        choice("triggerProtocol", "Trigger protocol", *protocols),
                        txt("openPort", "Ports to open", 1, 64, help = "A single port or a range (e.g. 5000-5010)"),
                        choice("openProtocol", "Open protocol", *protocols),
                    ),
                    defaults = mapOf("enable" to "1", "X_TP_ConnName" to "@activeWan", "triggerProtocol" to "TCP", "openProtocol" to "TCP"),
                ),
            ),
        ),
    )

    private val dmz = Page(
        id = "dmz", title = "DMZ", group = Group.NAT,
        description = "Exposes a whole device to the Internet",
        sections = listOf(
            Section(
                id = "dmz", title = "DMZ host", oid = "DEV2_DMZ_HOST_CFG",
                fields = listOf(sw("enable", "Enable DMZ"), ip("IPAddress", "Host IP", visibleIf = { it["enable"] == "1" })),
                saveWarning = "A host in the DMZ is exposed to the Internet with no port filtering.",
            ),
        ),
    )

    private val upnp = Page(
        id = "upnp", title = "UPnP", group = Group.NAT,
        sections = listOf(
            Section(id = "upnp", title = "UPnP", oid = "DEV2_UPNP_CFG", fields = listOf(sw("enable", "Enable UPnP"))),
            Section(
                id = "upnpmap", title = "Active mappings", oid = "DEV2_UPNP_PORTMAPPING", multi = true, readOnly = true, showAll = true,
                fields = emptyList(), itemTitle = { it["description"] ?: tr("Mapping") },
            ),
        ),
    )

    private val alg = Page(
        id = "alg", title = "ALG", group = Group.NAT,
        description = "Application-level gateways",
        sections = listOf(
            Section(
                id = "alg", title = "ALG", oid = "DEV2_ALG_CFG",
                fields = listOf(
                    sw("pptpAlg", "PPTP"), sw("l2tpAlg", "L2TP"), sw("ipSecAlg", "IPSec"), sw("ftpAlg", "FTP"),
                    sw("tftpAlg", "TFTP"), sw("h323Alg", "H.323"), sw("rtspAlg", "RTSP"), sw("sipAlg", "SIP"),
                ),
            ),
        ),
    )

    // ---------- Security ----------
    private val security = Page(
        id = "ddos", title = "DoS protection", group = Group.SECURITY,
        sections = listOf(
            Section(
                id = "ddos", title = "Protection against DoS attacks", oid = "DEV2_DDOS_CFG",
                fields = listOf(
                    sw("enable", "Enable protection"),
                    sw("enableIcmpFilter", "ICMP flood filter", visibleIf = { it["enable"] == "1" }),
                    sw("enableUdpFilter", "UDP flood filter", visibleIf = { it["enable"] == "1" }),
                    sw("enableSynFilter", "SYN flood filter", visibleIf = { it["enable"] == "1" }),
                    sw("forbidLanPing", "Ignore ping from LAN"),
                ),
            ),
        ),
    )

    private val arp = Page(
        id = "arp", title = "IP-MAC binding (ARP)", group = Group.SECURITY,
        sections = listOf(
            Section(id = "arp", title = "IP-MAC binding (ARP)", oid = "DEV2_ARP_BIND", fields = listOf(sw("enable", "Enable ARP binding"))),
        ),
    )

    // ---------- Storage ----------
    private val storage = Page(
        id = "storage", title = "File servers", group = Group.STORAGE,
        description = "Samba, FTP and DLNA for USB drives",
        sections = listOf(
            Section(
                id = "smb", title = "Samba (network folders)", oid = "DEV2_SMB_SERVICE",
                fields = listOf(sw("enable", "Enable"), txt("serverName", "Server name", 1, 15),
                    sw("shareAll", "Share everything"), sw("anonymous", "Anonymous access")),
            ),
            Section(
                id = "ftp", title = "FTP server", oid = "DEV2_FTP_SERVER",
                fields = listOf(sw("enable", "Enable"), txt("serverName", "Server name", 1, 15),
                    num("portNumber", "Port", 1, 65535), sw("shareAll", "Share everything"),
                    sw("anonymous", "Anonymous access"), sw("accessFromInternet", "Allow access from the Internet")),
                saveWarning = "Allowing FTP from the Internet exposes your files outside your network.",
            ),
            Section(
                id = "dlna", title = "DLNA media server", oid = "DEV2_DLNA_MEDIA_SERVER",
                fields = listOf(sw("serverState", "Enable"), txt("serverName", "Server name", 1, 32),
                    sw("shareAll", "Share everything")),
            ),
        ),
    )

    // ---------- System ----------
    private val time = Page(
        id = "time", title = "Date and time", group = Group.SYSTEM,
        sections = listOf(
            Section(
                id = "time", title = "System time", oid = "DEV2_TIME",
                fields = listOf(
                    info("currentLocalTime", "Current time"),
                    info("status", "NTP status"),
                    sw("enable", "Synchronize with NTP"),
                    txt("NTPServer1", "NTP server 1", 0, 64),
                    txt("NTPServer2", "NTP server 2", 0, 64),
                    choiceBy("localTimeZone", "Time zone") { timeZones },
                    sw("daylightSavingsUsed", "Automatic daylight saving time"),
                ),
            ),
        ),
    )

    private val led = Page(
        id = "led", title = "LED", group = Group.SYSTEM,
        sections = listOf(
            Section(
                id = "led", title = "Router LEDs", oid = "DEV2_LED_SCHEDULE_CFG",
                fields = listOf(
                    sw("masterEnable", "LEDs on"),
                    sw("enable", "Schedule night shutdown", visibleIf = { it["masterEnable"] == "1" }),
                    Field("startTime", "Turn off at", FieldType.TimeOfDay, visibleIf = { it["masterEnable"] == "1" && it["enable"] == "1" }),
                    Field("endTime", "Turn on at", FieldType.TimeOfDay, visibleIf = { it["masterEnable"] == "1" && it["enable"] == "1" }),
                ),
            ),
        ),
    )

    private val reboot = Page(
        id = "reboot", title = "Reboot", group = Group.SYSTEM,
        sections = listOf(
            Section(
                id = "sched", title = "Scheduled reboot", oid = "DEV2_REBOOT_SCHEDULE_CFG",
                fields = listOf(
                    sw("enable", "Reboot automatically"),
                    num("hours", "Hour", 0, 23, visibleIf = { it["enable"] == "1" }),
                    num("minutes", "Minute", 0, 59, visibleIf = { it["enable"] == "1" }),
                ),
            ),
        ),
        actions = listOf(
            PageAction("Reboot the router now", "ACT_REBOOT",
                "The router will restart and you will lose the connection for 1-2 minutes. Continue?", danger = true),
        ),
    )

    private val stats = Page(
        id = "stats", title = "Traffic statistics", group = Group.SYSTEM,
        sections = listOf(
            Section(id = "stat", title = "Statistics", oid = "DEV2_STAT_CFG",
                fields = listOf(sw("enable", "Enable statistics"), num("interval", "Interval", 5, 60, "s"))),
        ),
    )

    // ---------- Status ----------
    private val dashboard = Page(id = "dashboard", title = "Summary", group = Group.STATUS,
        description = "Router, Internet and Wi-Fi status", custom = true)
    private val devices = Page(id = "devices", title = "Connected devices", group = Group.STATUS,
        description = "Wired and Wi-Fi clients", custom = true)

    val pages: List<Page> = listOf(
        dashboard, devices,
        wifiMain, wifiRadio, wifiAdvanced, wps, guest, multiSsid,
        lan, dhcpReservations, ddns,
        virtualServers, portTrigger, dmz, upnp, alg,
        security, arp,
        storage,
        time, led, reboot, stats,
    )

    fun visiblePages(username: String): List<Page> =
        pages.filter { !it.adminOnly || username.equals("admin", ignoreCase = true) }

    fun byId(id: String): Page? = pages.firstOrNull { it.id == id }
}
