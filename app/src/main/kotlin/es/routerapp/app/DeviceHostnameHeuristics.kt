package es.routerapp.app

/**
 * Heuristically inferred device metadata from hostname.
 */
data class HeuristicDeviceInfo(
    val vendor: String? = null,
    val category: DeviceCategory? = null,
)

/**
 * Offline rule-based pattern matching engine that infers manufacturer and
 * device category from DHCP client hostnames.
 */
object DeviceHostnameHeuristics {

    private val IPHONE_REGEX = Regex("""(?i).*\biphone\b.*|(?i).*iphone.*""")
    private val IPAD_REGEX = Regex("""(?i).*\bipad\b.*|(?i).*ipad.*""")
    private val IPOD_REGEX = Regex("""(?i).*\bipod\b.*|(?i).*ipod.*""")
    private val MAC_REGEX = Regex("""(?i).*(macbook|imac|mac[-_]?mini|mac[-_]?pro|macstudio).*""")
    private val APPLE_WATCH_REGEX = Regex("""(?i).*(apple[-_ ]?watch).*""")
    private val APPLE_TV_REGEX = Regex("""(?i).*(apple[-_ ]?tv).*""")

    private val SAMSUNG_TAB_REGEX = Regex("""(?i).*(galaxy[-_ ]?tab|sm-t[0-9]{3}).*""")
    private val SAMSUNG_PHONE_REGEX = Regex("""(?i).*(galaxy[-_ ]?(s|a|z|note|fold|flip)[0-9]*|sm-[a-z][0-9]{3}|galaxy).*""")
    private val SAMSUNG_TV_REGEX = Regex("""(?i).*(samsung[-_ ]?tv|qled).*""")

    private val PIXEL_TAB_REGEX = Regex("""(?i).*(pixel[-_ ]?tablet).*""")
    private val PIXEL_PHONE_REGEX = Regex("""(?i).*(pixel).*""")
    private val GOOGLE_HOME_REGEX = Regex("""(?i).*(nest[-_ ]|google[-_ ]?home).*""")
    private val CHROMECAST_REGEX = Regex("""(?i).*(chromecast).*""")

    private val XIAOMI_TAB_REGEX = Regex("""(?i).*(pad[-_ ]?[0-9]|mi[-_ ]?pad).*""")
    private val XIAOMI_PHONE_REGEX = Regex("""(?i).*(redmi|poco|xiaomi|mi[-_ ][0-9a-z]+).*""")

    private val AMAZON_ECHO_REGEX = Regex("""(?i).*(echo[-_ ]|echo\b).*""")
    private val AMAZON_FIRE_TV_REGEX = Regex("""(?i).*(fire[-_ ]?tv|fire[-_ ]?stick).*""")
    private val AMAZON_KINDLE_REGEX = Regex("""(?i).*(kindle|fire[-_ ]?hd|fire[-_ ]?tab).*""")

    private val NINTENDO_REGEX = Regex("""(?i).*(switch|nintendo).*""")
    private val PLAYSTATION_REGEX = Regex("""(?i).*(playstation|ps3|ps4|ps5).*""")
    private val XBOX_REGEX = Regex("""(?i).*(xbox).*""")

    private val SONY_TV_REGEX = Regex("""(?i).*(bravia).*""")
    private val LG_TV_REGEX = Regex("""(?i).*(webos|lg[-_ ]?tv).*""")
    private val ROKU_REGEX = Regex("""(?i).*(roku).*""")

    private val WINDOWS_PC_REGEX = Regex("""(?i)^(desktop|laptop)-[a-z0-9]+$|(?i).*(desktop-|laptop-|win-|pc-|[-_]pc$).*""")
    private val SURFACE_REGEX = Regex("""(?i).*(surface).*""")

    private val IOT_REGEX = Regex("""(?i).*(shelly|sonoff|esp[-_]|esp8266|esp32|tasmota|tuya|philips[-_ ]?hue|hue[-_ ]?bridge).*""")
    private val PRINTER_REGEX = Regex("""(?i).*(printer|epson|brother|canon[-_]|deskjet|laserjet|officejet).*""")

    private val GENERIC_ANDROID_REGEX = Regex("""(?i)^android-[a-f0-9]+$""")

    fun infer(hostname: String?): HeuristicDeviceInfo {
        if (hostname.isNullOrBlank()) return HeuristicDeviceInfo()
        val h = hostname.trim()

        // 1. Apple products
        if (APPLE_TV_REGEX.matches(h)) return HeuristicDeviceInfo(vendor = "Apple", category = DeviceCategory.TV)
        if (APPLE_WATCH_REGEX.matches(h)) return HeuristicDeviceInfo(vendor = "Apple", category = DeviceCategory.IOT)
        if (IPHONE_REGEX.matches(h)) return HeuristicDeviceInfo(vendor = "Apple", category = DeviceCategory.PHONE)
        if (IPAD_REGEX.matches(h)) return HeuristicDeviceInfo(vendor = "Apple", category = DeviceCategory.TABLET)
        if (IPOD_REGEX.matches(h)) return HeuristicDeviceInfo(vendor = "Apple", category = DeviceCategory.PHONE)
        if (MAC_REGEX.matches(h)) return HeuristicDeviceInfo(vendor = "Apple", category = DeviceCategory.COMPUTER)

        // 2. Samsung products
        if (SAMSUNG_TV_REGEX.matches(h)) return HeuristicDeviceInfo(vendor = "Samsung", category = DeviceCategory.TV)
        if (SAMSUNG_TAB_REGEX.matches(h)) return HeuristicDeviceInfo(vendor = "Samsung", category = DeviceCategory.TABLET)
        if (SAMSUNG_PHONE_REGEX.matches(h)) return HeuristicDeviceInfo(vendor = "Samsung", category = DeviceCategory.PHONE)
        if (h.contains("samsung", ignoreCase = true)) return HeuristicDeviceInfo(vendor = "Samsung", category = null)

        // 3. Google products
        if (CHROMECAST_REGEX.matches(h)) return HeuristicDeviceInfo(vendor = "Google", category = DeviceCategory.TV)
        if (GOOGLE_HOME_REGEX.matches(h)) return HeuristicDeviceInfo(vendor = "Google", category = DeviceCategory.IOT)
        if (PIXEL_TAB_REGEX.matches(h)) return HeuristicDeviceInfo(vendor = "Google", category = DeviceCategory.TABLET)
        if (PIXEL_PHONE_REGEX.matches(h)) return HeuristicDeviceInfo(vendor = "Google", category = DeviceCategory.PHONE)

        // 4. Xiaomi / Redmi / POCO
        if (XIAOMI_TAB_REGEX.matches(h)) return HeuristicDeviceInfo(vendor = "Xiaomi", category = DeviceCategory.TABLET)
        if (XIAOMI_PHONE_REGEX.matches(h)) return HeuristicDeviceInfo(vendor = "Xiaomi", category = DeviceCategory.PHONE)

        // 5. Amazon products
        if (AMAZON_FIRE_TV_REGEX.matches(h)) return HeuristicDeviceInfo(vendor = "Amazon", category = DeviceCategory.TV)
        if (AMAZON_ECHO_REGEX.matches(h)) return HeuristicDeviceInfo(vendor = "Amazon", category = DeviceCategory.IOT)
        if (AMAZON_KINDLE_REGEX.matches(h)) return HeuristicDeviceInfo(vendor = "Amazon", category = DeviceCategory.TABLET)

        // 6. Consoles & Entertainment
        if (NINTENDO_REGEX.matches(h)) return HeuristicDeviceInfo(vendor = "Nintendo", category = DeviceCategory.CONSOLE)
        if (PLAYSTATION_REGEX.matches(h)) return HeuristicDeviceInfo(vendor = "Sony", category = DeviceCategory.CONSOLE)
        if (XBOX_REGEX.matches(h)) return HeuristicDeviceInfo(vendor = "Microsoft", category = DeviceCategory.CONSOLE)

        // 7. Smart TVs / Streaming
        if (SONY_TV_REGEX.matches(h)) return HeuristicDeviceInfo(vendor = "Sony", category = DeviceCategory.TV)
        if (LG_TV_REGEX.matches(h)) return HeuristicDeviceInfo(vendor = "LG", category = DeviceCategory.TV)
        if (ROKU_REGEX.matches(h)) return HeuristicDeviceInfo(vendor = "Roku", category = DeviceCategory.TV)

        // 8. Computers
        if (SURFACE_REGEX.matches(h)) return HeuristicDeviceInfo(vendor = "Microsoft", category = DeviceCategory.COMPUTER)
        if (WINDOWS_PC_REGEX.matches(h)) return HeuristicDeviceInfo(vendor = null, category = DeviceCategory.COMPUTER)

        // 9. Smart Home / IoT
        if (IOT_REGEX.matches(h)) {
            val vendor = when {
                h.contains("shelly", ignoreCase = true) -> "Shelly"
                h.contains("sonoff", ignoreCase = true) -> "Sonoff"
                h.contains("esp", ignoreCase = true) -> "Espressif"
                h.contains("tuya", ignoreCase = true) -> "Tuya"
                h.contains("hue", ignoreCase = true) -> "Philips"
                else -> null
            }
            return HeuristicDeviceInfo(vendor = vendor, category = DeviceCategory.IOT)
        }

        // 10. Printers
        if (PRINTER_REGEX.matches(h)) {
            val vendor = when {
                h.contains("epson", ignoreCase = true) -> "Epson"
                h.contains("brother", ignoreCase = true) -> "Brother"
                h.contains("canon", ignoreCase = true) -> "Canon"
                h.contains("deskjet", ignoreCase = true) ||
                    h.contains("laserjet", ignoreCase = true) ||
                    h.contains("officejet", ignoreCase = true) -> "HP"
                else -> null
            }
            return HeuristicDeviceInfo(vendor = vendor, category = DeviceCategory.PRINTER)
        }

        // 11. Generic Android
        if (GENERIC_ANDROID_REGEX.matches(h)) {
            return HeuristicDeviceInfo(vendor = null, category = DeviceCategory.PHONE)
        }

        return HeuristicDeviceInfo()
    }
}
