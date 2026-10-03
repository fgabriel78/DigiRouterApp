package es.routerapp.protocol

import es.routerapp.protocol.i18n.I18n
import es.routerapp.protocol.pages.Catalog
import es.routerapp.protocol.pages.Field
import es.routerapp.protocol.pages.FieldType
import es.routerapp.protocol.pages.Group
import es.routerapp.protocol.pages.Section
import es.routerapp.protocol.pages.validate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Every user-visible text of the catalog must have a Spanish translation (or be a technical term). */
class CatalogI18nTest {

    /** Technical labels that are the same in every language. */
    private val untranslated = setOf(
        "Wi-Fi", "Port triggering", "WPS", "UPnP", "ALG", "DMZ", "LED", "Radio", "DynDNS", "No-IP", "TCP", "UDP",
        "WPA2-Personal", "WPA2/WPA3-Personal", "WPA3-Personal", "WPA/WPA2-Personal",
        "802.11 b/g/n", "802.11 b/g/n/ax (Wi-Fi 6)", "802.11 b/g/n/ax/be (Wi-Fi 7)",
        "802.11 a/n/ac", "802.11 a/n/ac/ax (Wi-Fi 6)", "802.11 a/n/ac/ax/be (Wi-Fi 7)",
        "20 MHz", "40 MHz", "80 MHz", "160 MHz",
        "Beamforming", "MU-MIMO", "OFDMA", "Target Wake Time (TWT)", "BSS Color", "WMM", "IGMP snooping", "Band steering",
        "PPTP", "L2TP", "IPSec", "FTP", "TFTP", "H.323", "RTSP", "SIP", "Samba",
    )

    private val sampleValues = listOf(
        mapOf("band" to "2.4GHz", "description" to "web", "externalPort" to "80", "applicationName" to "app"),
        mapOf("band" to "5GHz", "description" to "", "externalPort" to "", "applicationName" to ""),
        mapOf("band" to "6GHz"),
        emptyMap(),
    )

    private fun fieldTexts(f: Field, out: MutableList<String>) {
        out += f.label
        f.help?.let { out += it }
        val t = f.type
        if (t is FieldType.Choice) sampleValues.forEach { v -> t.options(v).forEach { out += it.label } }
    }

    private fun sectionTexts(s: Section, out: MutableList<String>) {
        out += s.title
        s.note?.let { out += it }
        s.saveWarning?.let { out += it }
        s.fields.forEach { fieldTexts(it, out) }
        s.add?.let { a -> out += a.title; a.fields.forEach { fieldTexts(it, out) } }
        // Item titles are translated when built (calling them exercises their tr() calls).
        sampleValues.forEach { s.itemTitle(it) }
    }

    @Test
    fun everyCatalogTextHasASpanishTranslation() {
        val previous = I18n.languageProvider
        val missing = linkedSetOf<String>()
        try {
            I18n.languageProvider = { "es" }
            I18n.onMissing = { missing += it }
            val texts = mutableListOf<String>()
            Group.entries.forEach { texts += it.title }
            for (p in Catalog.pages) {
                texts += p.title
                p.description?.let { texts += it }
                p.sections.forEach { sectionTexts(it, texts) }
                p.actions.forEach { texts += it.title; texts += it.confirm }
            }
            // Validation messages
            val tooShort = Field("k", "k", FieldType.Text(5, 10))
            tooShort.validate("a", emptyMap())
            Field("k", "k", FieldType.Text(0, 1)).validate("abc", emptyMap())
            Field("k", "k", FieldType.Number(1, 2)).validate("9", emptyMap())
            Field("k", "k", FieldType.Ipv4).validate("x", emptyMap())
            Field("k", "k", FieldType.Mac).validate("x", emptyMap())
            // Errors raised by save hooks
            Catalog.byId("ddns")!!.sections.first().saveHook!!.let { hook ->
                runCatching { hook(mapOf("enable" to "0"), mapOf("enable" to "0"), mutableMapOf("userDomain" to "x")) }
            }
            // Resolve every collected text through the translator
            texts.forEach { I18n.tr(it) }
        } finally {
            I18n.languageProvider = previous
            I18n.onMissing = null
        }
        val data = Regex("""^(GMT[+-]\d\d:\d\d|\d+)$""")
        val real = missing.filter { it !in untranslated && !data.matches(it) }
        assertTrue(real.isEmpty(), "Texts without Spanish translation: $real")
    }

    @Test
    fun englishReturnsTheSourceTextAndFillsPlaceholders() {
        val previous = I18n.languageProvider
        try {
            I18n.languageProvider = { "en" }
            assertEquals("Main Wi-Fi", I18n.tr("Main Wi-Fi"))
            assertEquals("Value between 1 and 5", I18n.tr("Value between {0} and {1}", 1, 5))
            I18n.languageProvider = { "es" }
            assertEquals("Valor entre 1 y 5", I18n.tr("Value between {0} and {1}", 1, 5))
            I18n.languageProvider = { "fr" } // unsupported language falls back to English
            assertEquals("Main Wi-Fi", I18n.tr("Main Wi-Fi"))
        } finally {
            I18n.languageProvider = previous
        }
    }

    @Test
    fun spanishDictionaryHasNoUnusedEntriesFromTyposInPlaceholders() {
        // Every placeholder used in a source text must also appear in its translation.
        val re = Regex("""\{\d+}""")
        es.routerapp.protocol.i18n.SpanishTexts.table.forEach { (en, es) ->
            assertEquals(re.findAll(en).map { it.value }.toSet(), re.findAll(es).map { it.value }.toSet(), "Placeholder mismatch: $en")
        }
    }
}
