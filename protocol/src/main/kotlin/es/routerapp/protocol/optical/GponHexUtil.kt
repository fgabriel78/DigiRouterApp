package es.routerapp.protocol.optical

/**
 * Utility for GPON/XGS-PON Serial Number encoding conversions and SFP export snippets.
 *
 * GPON Serial Numbers commonly appear in two representations:
 * 1. 12-character ASCII+HEX: 4-character ASCII vendor ID (e.g. "HWTC", "TPLG", "ZTEG") + 8 hex digits.
 * 2. 16-character pure HEX: 4-character vendor ID encoded as 8 hex digits + 8 hex digits.
 */
object GponHexUtil {

    private val HEX_REGEX_16 = Regex("^[0-9A-Fa-f]{16}$")
    private val ASCII_HEX_REGEX_12 = Regex("^[a-zA-Z0-9]{4}[0-9A-Fa-f]{8}$")

    /**
     * Converts a 12-character ASCII+HEX serial number (e.g. "HWTC12345678")
     * into a 16-character pure HEX string (e.g. "4857544312345678").
     * Returns null if [snAscii] is invalid.
     */
    fun asciiToHex(snAscii: String): String? {
        val trimmed = snAscii.trim()
        if (!ASCII_HEX_REGEX_12.matches(trimmed)) return null
        val vendorAscii = trimmed.substring(0, 4)
        val serialHex = trimmed.substring(4, 12).uppercase()
        val vendorHex = vendorAscii.map { char ->
            char.code.toString(16).padStart(2, '0').uppercase()
        }.joinToString("")
        return vendorHex + serialHex
    }

    /**
     * Converts a 16-character pure HEX serial number (e.g. "4857544312345678")
     * into a 12-character ASCII+HEX string (e.g. "HWTC12345678").
     * Returns null if [snHex] is invalid or vendor code bytes are unprintable ASCII.
     */
    fun hexToAscii(snHex: String): String? {
        val trimmed = snHex.trim()
        if (!HEX_REGEX_16.matches(trimmed)) return null
        val vendorHex = trimmed.substring(0, 8)
        val serialHex = trimmed.substring(8, 16).uppercase()

        val vendorChars = CharArray(4)
        for (i in 0 until 4) {
            val byteStr = vendorHex.substring(i * 2, i * 2 + 2)
            val code = byteStr.toIntOrNull(16) ?: return null
            if (code !in 32..126) return null
            vendorChars[i] = code.toChar()
        }
        return String(vendorChars) + serialHex
    }

    /**
     * Resolves both representations (ASCII+HEX and pure HEX) from an arbitrary GPON SN string.
     * Returns Pair(asciiFormat, hexFormat), or null if the string cannot be parsed.
     */
    fun normalize(sn: String): Pair<String, String>? {
        val trimmed = sn.trim()
        return when {
            trimmed.length == 12 && ASCII_HEX_REGEX_12.matches(trimmed) -> {
                val hex = asciiToHex(trimmed) ?: return null
                Pair(trimmed.substring(0, 4) + trimmed.substring(4).uppercase(), hex)
            }
            trimmed.length == 16 && HEX_REGEX_16.matches(trimmed) -> {
                val ascii = hexToAscii(trimmed)
                if (ascii != null) {
                    Pair(ascii, trimmed.uppercase())
                } else {
                    Pair(trimmed.uppercase(), trimmed.uppercase())
                }
            }
            else -> null
        }
    }

    /**
     * Generates a standardized configuration snippet suitable for third-party SFP ONT sticks
     * (e.g., DFP-34X-2C2, Huawei MA5671A, CarlitoxxPro, OpenWrt, pfSense).
     */
    fun generateSfpExportSnippet(
        snAscii: String,
        snHex: String,
        ploamPassword: String,
        oltModeName: String,
    ): String {
        return buildString {
            appendLine("# ============================================================")
            appendLine("# DIGI FTTH GPON/XGS-PON ONT REGISTRATION BACKUP")
            appendLine("# ============================================================")
            appendLine("GPON_SN_ASCII=\"$snAscii\"")
            appendLine("GPON_SN_HEX=\"$snHex\"")
            appendLine("PLOAM_PASSWORD=\"$ploamPassword\"")
            appendLine("OLT_MODE=\"$oltModeName\"")
            appendLine()
            appendLine("# Linux / OpenWrt / SFP Stick (CarlitoxxPro) Commands:")
            appendLine("# fw_setenv gpon_sn $snHex")
            appendLine("# fw_setenv gpon_ploam $ploamPassword")
            appendLine("# uci set network.wan.username='<pppoe_user>'")
            appendLine("# uci commit network")
        }
    }
}
