package es.routerapp.app

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Utilities for formatting and rendering standard Wi-Fi QR barcodes.
 * Conforms to the standard WIFI: URI format:
 * WIFI:S:<SSID>;T:<AUTH>;P:<PASSWORD>;H:<HIDDEN>;;
 */
object WifiQr {

    /**
     * Escapes special characters (\, ;, :, ,, ", =) per the Wi-Fi barcode format specification.
     */
    fun escapeWifiString(input: String): String = buildString {
        for (ch in input) {
            if (ch in "\\;:,=\"") {
                append('\\')
            }
            append(ch)
        }
    }

    /**
     * Maps the router's security mode string to the standard Wi-Fi barcode authentication token.
     * Android and iOS natively recognize WPA, WEP, or nopass.
     */
    fun mapSecurityModeToQrAuth(securityMode: String): String = when {
        securityMode.equals("None", ignoreCase = true) || securityMode.equals("OWE", ignoreCase = true) -> "nopass"
        securityMode.contains("WEP", ignoreCase = true) -> "WEP"
        else -> "WPA"
    }

    /**
     * Formats a complete, standard Wi-Fi QR barcode string.
     */
    fun formatWifiQrString(
        ssid: String,
        psk: String,
        securityMode: String,
        hidden: Boolean = false,
    ): String {
        val auth = mapSecurityModeToQrAuth(securityMode)
        val escapedSsid = escapeWifiString(ssid)
        return buildString {
            append("WIFI:")
            append("S:").append(escapedSsid).append(";")
            append("T:").append(auth).append(";")
            if (auth != "nopass" && psk.isNotEmpty()) {
                val escapedPsk = escapeWifiString(psk)
                append("P:").append(escapedPsk).append(";")
            }
            if (hidden) {
                append("H:true;")
            }
            append(";")
        }
    }

    /**
     * Encodes a string into a 2D boolean QR matrix using ZXing.
     * Pure JVM-compatible, ideal for direct unit testing without Android bitmap mocking.
     */
    fun generateQrMatrix(
        content: String,
        size: Int = 256,
        margin: Int = 1,
    ): Array<BooleanArray> {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to margin,
        )
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        return Array(height) { y ->
            BooleanArray(width) { x -> bitMatrix.get(x, y) }
        }
    }

    /**
     * Renders a QR code to an Android ARGB_8888 Bitmap.
     */
    fun generateQrBitmap(
        content: String,
        sizePx: Int = 512,
        margin: Int = 1,
    ): Bitmap {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to margin,
        )
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)
        val black = 0xFF000000.toInt()
        val white = 0xFFFFFFFF.toInt()
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) black else white
            }
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    /**
     * Convenience function to obtain a Compose ImageBitmap for direct canvas or Image rendering.
     */
    fun generateQrImageBitmap(
        content: String,
        sizePx: Int = 512,
        margin: Int = 1,
    ): ImageBitmap {
        return generateQrBitmap(content, sizePx, margin).asImageBitmap()
    }

    /**
     * Extracts Wi-Fi credentials and status from generic Section edits in configuration screens.
     */
    fun extractWifiSectionData(sectionId: String, values: Map<String, String>): WifiSectionData? {
        return when {
            sectionId == "primary" -> {
                val ssid = values["primarySSID"].orEmpty()
                if (ssid.isEmpty()) null
                else WifiSectionData(
                    ssid = ssid,
                    psk = values["primaryPSK"].orEmpty(),
                    securityMode = values["primaryModeEnabled"] ?: "WPA2-Personal",
                    hidden = values["primarySSIDAdvertise"] == "0",
                    enabled = values["primaryEnable"] == "1",
                    category = WifiNetworkCategory.PRIMARY,
                    bands = listOfNotNull(values["band"]),
                )
            }
            sectionId == "mlo" -> {
                val ssid = values["mloSSID"].orEmpty()
                if (ssid.isEmpty()) null
                else WifiSectionData(
                    ssid = ssid,
                    psk = values["mloPSK"].orEmpty(),
                    securityMode = values["mloModeEnabled"] ?: "WPA2-WPA3-Personal",
                    hidden = values["mloSSIDAdvertise"] == "0",
                    enabled = values["mloEnable"] == "1",
                    category = WifiNetworkCategory.MLO,
                    bands = listOfNotNull(values["band"]),
                )
            }
            sectionId == "guest" -> {
                val ssid = values["guestSSID"].orEmpty()
                if (ssid.isEmpty()) null
                else WifiSectionData(
                    ssid = ssid,
                    psk = values["guestPSK"].orEmpty(),
                    securityMode = values["guestModeEnabled"] ?: "WPA2-Personal",
                    hidden = values["guestSSIDAdvertise"] == "0",
                    enabled = values["guestEnable"] == "1",
                    category = WifiNetworkCategory.GUEST,
                    bands = listOfNotNull(values["band"]),
                )
            }
            sectionId.startsWith("mssid") -> {
                val n = sectionId.removePrefix("mssid").toIntOrNull() ?: 1
                val ssid = values["mssid${n}SSID"].orEmpty()
                if (ssid.isEmpty()) null
                else WifiSectionData(
                    ssid = ssid,
                    psk = values["mssid${n}PSK"].orEmpty(),
                    securityMode = values["mssid${n}ModeEnabled"] ?: "WPA2-Personal",
                    hidden = values["mssid${n}SSIDAdvertise"] == "0",
                    enabled = values["mssid${n}Enable"] == "1",
                    category = WifiNetworkCategory.ADDITIONAL,
                    additionalIndex = n,
                    bands = listOfNotNull(values["band"]),
                )
            }
            else -> null
        }
    }
}

data class WifiSectionData(
    val ssid: String,
    val psk: String,
    val securityMode: String,
    val hidden: Boolean,
    val enabled: Boolean,
    val category: WifiNetworkCategory,
    val additionalIndex: Int? = null,
    val bands: List<String> = emptyList(),
)

