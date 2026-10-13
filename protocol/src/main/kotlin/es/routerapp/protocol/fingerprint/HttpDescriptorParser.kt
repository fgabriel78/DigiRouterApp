package es.routerapp.protocol.fingerprint

data class HttpDescriptor(
    val friendlyName: String? = null,
    val manufacturer: String? = null,
    val modelName: String? = null,
    val modelNumber: String? = null,
    val modelDescription: String? = null,
    val serverHeader: String? = null,
    val extraProperties: Map<String, String> = emptyMap(),
)

object HttpDescriptorParser {

    private val FRIENDLY_NAME_REGEX = Regex("""<friendlyName>(.*?)</friendlyName>""", RegexOption.IGNORE_CASE)
    private val MANUFACTURER_REGEX = Regex("""<manufacturer>(.*?)</manufacturer>""", RegexOption.IGNORE_CASE)
    private val MODEL_NAME_REGEX = Regex("""<modelName>(.*?)</modelName>""", RegexOption.IGNORE_CASE)
    private val MODEL_NUMBER_REGEX = Regex("""<modelNumber>(.*?)</modelNumber>""", RegexOption.IGNORE_CASE)
    private val MODEL_DESC_REGEX = Regex("""<modelDescription>(.*?)</modelDescription>""", RegexOption.IGNORE_CASE)

    // Common JSON fields in IoT & media devices
    private val JSON_NAME_REGEX = Regex(""""name"\s*:\s*"([^"]+)"""", RegexOption.IGNORE_CASE)
    private val JSON_MODEL_NAME_REGEX = Regex(""""model_name"\s*:\s*"([^"]+)"""", RegexOption.IGNORE_CASE)
    private val JSON_MANUFACTURER_REGEX = Regex(""""manufacturer"\s*:\s*"([^"]+)"""", RegexOption.IGNORE_CASE)
    private val JSON_SHELLY_TYPE_REGEX = Regex(""""type"\s*:\s*"([^"]+)"""", RegexOption.IGNORE_CASE)
    private val JSON_SHELLY_APP_REGEX = Regex(""""app"\s*:\s*"([^"]+)"""", RegexOption.IGNORE_CASE)

    fun parseUpnpXml(xml: String, serverHeader: String? = null): HttpDescriptor {
        fun extractTag(regex: Regex): String? {
            val match = regex.find(xml)?.groupValues?.get(1)?.trim() ?: return null
            return cleanXml(match)
        }

        return HttpDescriptor(
            friendlyName = extractTag(FRIENDLY_NAME_REGEX),
            manufacturer = extractTag(MANUFACTURER_REGEX),
            modelName = extractTag(MODEL_NAME_REGEX),
            modelNumber = extractTag(MODEL_NUMBER_REGEX),
            modelDescription = extractTag(MODEL_DESC_REGEX),
            serverHeader = serverHeader?.trim(),
        )
    }

    fun parseJsonMetadata(json: String, serverHeader: String? = null): HttpDescriptor {
        val name = JSON_NAME_REGEX.find(json)?.groupValues?.get(1)?.trim()
        val model = JSON_MODEL_NAME_REGEX.find(json)?.groupValues?.get(1)?.trim()
        val manufacturer = JSON_MANUFACTURER_REGEX.find(json)?.groupValues?.get(1)?.trim()
        val shellyType = JSON_SHELLY_TYPE_REGEX.find(json)?.groupValues?.get(1)?.trim()
        val shellyApp = JSON_SHELLY_APP_REGEX.find(json)?.groupValues?.get(1)?.trim()

        val extras = mutableMapOf<String, String>()
        if (shellyType != null) extras["shelly_type"] = shellyType
        if (shellyApp != null) extras["shelly_app"] = shellyApp

        val resolvedModel = model ?: shellyType ?: shellyApp

        return HttpDescriptor(
            friendlyName = name,
            manufacturer = manufacturer ?: if (shellyType != null || shellyApp != null) "Shelly" else null,
            modelName = resolvedModel,
            serverHeader = serverHeader?.trim(),
            extraProperties = extras,
        )
    }

    fun parseAuto(body: String, serverHeader: String? = null): HttpDescriptor {
        val trimmed = body.trim()
        return when {
            trimmed.startsWith("<") -> parseUpnpXml(trimmed, serverHeader)
            trimmed.startsWith("{") -> parseJsonMetadata(trimmed, serverHeader)
            else -> HttpDescriptor(serverHeader = serverHeader)
        }
    }

    private fun cleanXml(value: String): String {
        var clean = value
        if (clean.startsWith("<![CDATA[") && clean.endsWith("]]>")) {
            clean = clean.substring(9, clean.length - 3)
        }
        return clean
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .trim()
    }
}
