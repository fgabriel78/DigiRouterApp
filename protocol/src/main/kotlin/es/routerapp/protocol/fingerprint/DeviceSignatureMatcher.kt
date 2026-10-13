package es.routerapp.protocol.fingerprint

object DeviceSignatureMatcher {

    fun match(
        ssdp: SsdpResponse? = null,
        mdns: MdnsPacket? = null,
        httpDesc: HttpDescriptor? = null,
        fallbackHostname: String? = null,
    ): FingerprintMatch? {
        val server = ssdp?.server ?: httpDesc?.serverHeader ?: ""
        val st = ssdp?.st ?: ""
        val usn = ssdp?.usn ?: ""
        val friendlyName = httpDesc?.friendlyName ?: ""
        val modelName = httpDesc?.modelName ?: ""
        val modelNumber = httpDesc?.modelNumber ?: ""
        val manufacturer = httpDesc?.manufacturer ?: ""
        val combinedHttp = "$friendlyName $modelName $modelNumber $manufacturer"
        val combinedSsdp = "$server $st $usn"

        // 1. PlayStation (PS5 / PS4)
        if (combinedHttp.contains("PlayStation 5", ignoreCase = true) ||
            combinedSsdp.contains("PRX", ignoreCase = true) ||
            combinedSsdp.contains("PlayStation 5", ignoreCase = true)
        ) {
            return FingerprintMatch(
                exactModel = "PlayStation 5",
                vendor = "Sony",
                category = FingerprintCategory.CONSOLE,
                discoveryMethod = if (httpDesc != null) "UPnP" else "SSDP",
            )
        }
        if (combinedHttp.contains("PlayStation 4", ignoreCase = true) ||
            combinedSsdp.contains("PlayStation 4", ignoreCase = true)
        ) {
            return FingerprintMatch(
                exactModel = "PlayStation 4",
                vendor = "Sony",
                category = FingerprintCategory.CONSOLE,
                discoveryMethod = if (httpDesc != null) "UPnP" else "SSDP",
            )
        }

        // 2. Xbox
        if (combinedSsdp.contains("Xbox", ignoreCase = true) || combinedHttp.contains("Xbox", ignoreCase = true)) {
            val model = if (combinedHttp.contains("Series X", ignoreCase = true)) "Xbox Series X"
            else if (combinedHttp.contains("Series S", ignoreCase = true)) "Xbox Series S"
            else if (combinedHttp.contains("XboxOne", ignoreCase = true) || combinedSsdp.contains("XboxOne", ignoreCase = true)) "Xbox One"
            else "Xbox"
            return FingerprintMatch(
                exactModel = model,
                vendor = "Microsoft",
                category = FingerprintCategory.CONSOLE,
                discoveryMethod = if (httpDesc != null) "UPnP" else "SSDP",
            )
        }

        // 3. Nintendo Switch
        if (fallbackHostname?.contains("switch", ignoreCase = true) == true &&
            fallbackHostname.contains("nintendo", ignoreCase = true)
        ) {
            return FingerprintMatch(
                exactModel = "Nintendo Switch",
                vendor = "Nintendo",
                category = FingerprintCategory.CONSOLE,
                discoveryMethod = "Hostname",
            )
        }

        // 4. LG Smart TV (webOS)
        if (combinedSsdp.contains("webOS", ignoreCase = true) ||
            combinedSsdp.contains("lge-com", ignoreCase = true) ||
            combinedHttp.contains("LG Electronics", ignoreCase = true) ||
            combinedHttp.contains("webOS", ignoreCase = true)
        ) {
            val cleanModel = when {
                modelNumber.isNotBlank() && (modelNumber.startsWith("OLED", ignoreCase = true) || modelNumber.startsWith("QNED", ignoreCase = true)) -> {
                    "LG " + modelNumber.take(8)
                }
                friendlyName.startsWith("LG", ignoreCase = true) -> friendlyName
                modelName.isNotBlank() && modelName != "LG TV" -> "LG $modelName"
                else -> "LG Smart TV (webOS)"
            }
            return FingerprintMatch(
                exactModel = cleanModel,
                vendor = "LG",
                category = FingerprintCategory.TV,
                discoveryMethod = if (httpDesc != null) "UPnP" else "SSDP",
            )
        }

        // 5. Samsung Smart TV (Tizen)
        if (combinedSsdp.contains("Tizen", ignoreCase = true) ||
            combinedSsdp.contains("Sec-Product-Name", ignoreCase = true) ||
            (combinedHttp.contains("Samsung", ignoreCase = true) && combinedHttp.contains("TV", ignoreCase = true))
        ) {
            val cleanModel = if (friendlyName.isNotBlank() && friendlyName.contains("Samsung", ignoreCase = true)) {
                friendlyName
            } else {
                "Samsung Smart TV (Tizen)"
            }
            return FingerprintMatch(
                exactModel = cleanModel,
                vendor = "Samsung",
                category = FingerprintCategory.TV,
                discoveryMethod = if (httpDesc != null) "UPnP" else "SSDP",
            )
        }

        // 6. Sony Bravia
        if (combinedHttp.contains("BRAVIA", ignoreCase = true) || combinedSsdp.contains("BRAVIA", ignoreCase = true)) {
            val cleanModel = if (friendlyName.isNotBlank()) friendlyName else "Sony BRAVIA TV"
            return FingerprintMatch(
                exactModel = cleanModel,
                vendor = "Sony",
                category = FingerprintCategory.TV,
                discoveryMethod = if (httpDesc != null) "UPnP" else "SSDP",
            )
        }

        // 7. Philips Hue Bridge
        if (combinedHttp.contains("Philips hue", ignoreCase = true) ||
            modelNumber.equals("BSB002", ignoreCase = true) ||
            mdns?.records?.any { it.name.contains("_hue._tcp", ignoreCase = true) || it.ptrTarget?.contains("_hue._tcp", ignoreCase = true) == true } == true
        ) {
            return FingerprintMatch(
                exactModel = "Philips Hue Bridge",
                vendor = "Philips",
                category = FingerprintCategory.IOT,
                discoveryMethod = if (httpDesc != null) "HTTP" else "mDNS",
            )
        }

        // 8. Google Cast & Nest devices
        val hasGoogleCastMdns = mdns?.records?.any {
            it.name.contains("_googlecast._tcp", ignoreCase = true) ||
                it.ptrTarget?.contains("_googlecast._tcp", ignoreCase = true) == true
        } == true
        val mdnsModel = mdns?.findModelIdentifier()
        if (hasGoogleCastMdns || combinedHttp.contains("Chromecast", ignoreCase = true) || combinedHttp.contains("Google Nest", ignoreCase = true)) {
            val model = mdnsModel ?: modelName.takeIf { it.isNotBlank() } ?: friendlyName.takeIf { it.isNotBlank() } ?: "Google Cast"
            val isTv = model.contains("Chromecast", ignoreCase = true) || model.contains("TV", ignoreCase = true)
            return FingerprintMatch(
                exactModel = model,
                vendor = "Google",
                category = if (isTv) FingerprintCategory.TV else FingerprintCategory.IOT,
                discoveryMethod = if (mdns != null) "mDNS" else "HTTP",
            )
        }

        // 9. Apple Devices (AirPlay / mDNS TXT models)
        val appleModel = mdns?.allTxtAttributes()?.get("model") ?: mdns?.allTxtAttributes()?.get("am")
        if (appleModel != null || mdns?.records?.any { it.name.contains("_airplay._tcp", ignoreCase = true) || it.ptrTarget?.contains("_airplay._tcp", ignoreCase = true) == true } == true) {
            val (name, cat) = resolveAppleModel(appleModel)
            return FingerprintMatch(
                exactModel = name,
                vendor = "Apple",
                category = cat,
                discoveryMethod = "mDNS",
            )
        }

        // 10. Sonos
        if (combinedHttp.contains("Sonos", ignoreCase = true) || combinedSsdp.contains("Sonos", ignoreCase = true)) {
            val model = if (modelName.isNotBlank() && !modelName.equals("Sonos", ignoreCase = true)) "Sonos $modelName" else "Sonos Speaker"
            return FingerprintMatch(
                exactModel = model,
                vendor = "Sonos",
                category = FingerprintCategory.IOT,
                discoveryMethod = if (httpDesc != null) "UPnP" else "SSDP",
            )
        }

        // 11. Roku
        if (combinedSsdp.contains("ROKU", ignoreCase = true) || combinedHttp.contains("Roku", ignoreCase = true)) {
            val model = if (friendlyName.isNotBlank()) friendlyName else "Roku Streaming Device"
            return FingerprintMatch(
                exactModel = model,
                vendor = "Roku",
                category = FingerprintCategory.TV,
                discoveryMethod = if (httpDesc != null) "UPnP" else "SSDP",
            )
        }

        // 12. Amazon Fire TV & Echo
        if (combinedSsdp.contains("Amazon-Echo", ignoreCase = true)) {
            return FingerprintMatch(
                exactModel = "Amazon Echo",
                vendor = "Amazon",
                category = FingerprintCategory.IOT,
                discoveryMethod = "SSDP",
            )
        }
        if (combinedSsdp.contains("Fire TV", ignoreCase = true) || combinedSsdp.contains("AFT", ignoreCase = true) || combinedHttp.contains("Fire TV", ignoreCase = true)) {
            return FingerprintMatch(
                exactModel = "Amazon Fire TV",
                vendor = "Amazon",
                category = FingerprintCategory.TV,
                discoveryMethod = if (httpDesc != null) "UPnP" else "SSDP",
            )
        }

        // 13. Shelly & Smart IoT
        if (combinedHttp.contains("Shelly", ignoreCase = true) || httpDesc?.extraProperties?.containsKey("shelly_type") == true) {
            val type = httpDesc?.extraProperties?.get("shelly_type") ?: modelName
            val model = if (type.isNotBlank()) "Shelly $type" else "Shelly IoT Device"
            return FingerprintMatch(
                exactModel = model,
                vendor = "Shelly",
                category = FingerprintCategory.IOT,
                discoveryMethod = "HTTP",
            )
        }

        // 14. Network Printers
        val isPrinter = mdns?.records?.any {
            it.name.contains("_ipp._tcp", ignoreCase = true) ||
                it.name.contains("_printer._tcp", ignoreCase = true) ||
                it.ptrTarget?.contains("_ipp._tcp", ignoreCase = true) == true
        } == true || combinedHttp.contains("printer", ignoreCase = true)

        if (isPrinter) {
            val vendor = if (manufacturer.isNotBlank()) manufacturer else "Network"
            val model = if (friendlyName.isNotBlank()) friendlyName else "$vendor Printer"
            return FingerprintMatch(
                exactModel = model,
                vendor = vendor,
                category = FingerprintCategory.PRINTER,
                discoveryMethod = if (mdns != null) "mDNS" else "HTTP",
            )
        }

        return null
    }

    private fun resolveAppleModel(modelId: String?): Pair<String, FingerprintCategory> {
        if (modelId.isNullOrBlank()) return "Apple Device" to FingerprintCategory.OTHER
        val m = modelId.trim()
        return when {
            m.startsWith("AppleTV", ignoreCase = true) -> "Apple TV" to FingerprintCategory.TV
            m.startsWith("AudioAccessory", ignoreCase = true) -> "Apple HomePod" to FingerprintCategory.IOT
            m.startsWith("MacBookPro", ignoreCase = true) -> "MacBook Pro" to FingerprintCategory.COMPUTER
            m.startsWith("MacBookAir", ignoreCase = true) -> "MacBook Air" to FingerprintCategory.COMPUTER
            m.startsWith("MacBook", ignoreCase = true) -> "MacBook" to FingerprintCategory.COMPUTER
            m.startsWith("Macmini", ignoreCase = true) -> "Mac mini" to FingerprintCategory.COMPUTER
            m.startsWith("MacStudio", ignoreCase = true) -> "Mac Studio" to FingerprintCategory.COMPUTER
            m.startsWith("MacPro", ignoreCase = true) -> "Mac Pro" to FingerprintCategory.COMPUTER
            m.startsWith("iMac", ignoreCase = true) -> "iMac" to FingerprintCategory.COMPUTER
            m.startsWith("iPhone", ignoreCase = true) -> "iPhone" to FingerprintCategory.PHONE
            m.startsWith("iPad", ignoreCase = true) -> "iPad" to FingerprintCategory.TABLET
            else -> "Apple $m" to FingerprintCategory.OTHER
        }
    }
}
