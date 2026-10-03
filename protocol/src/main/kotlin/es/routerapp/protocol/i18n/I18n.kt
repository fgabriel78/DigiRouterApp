package es.routerapp.protocol.i18n

import java.util.Locale

/**
 * Minimal translation layer for the texts that live in the protocol module (the screen catalog and the
 * validation messages). The source text is **English** and doubles as the lookup key; languages other
 * than English provide a dictionary mapping the English text to its translation. A text without a
 * translation is shown in English, so a missing entry never breaks the UI.
 *
 * Placeholders are written `{0}`, `{1}`… and filled with the extra arguments of [tr].
 *
 * To add a language, create a dictionary like [SpanishTexts] and register it in [tables].
 */
object I18n {
    /** Language code in use (`es`, `en`…); follows the default locale, which Android keeps in sync with the app locale. */
    @Volatile
    var languageProvider: () -> String = { Locale.getDefault().language }

    /** Called with every text that has no translation in the active language (used by tests). */
    @Volatile
    var onMissing: ((String) -> Unit)? = null

    private val tables: Map<String, Map<String, String>> = mapOf(
        "es" to SpanishTexts.table,
    )

    fun tr(text: String, vararg args: Any): String {
        val lang = languageProvider()
        val table = tables[lang]
        val template = table?.get(text)
        if (template == null && table != null) onMissing?.invoke(text)
        var out = template ?: text
        args.forEachIndexed { i, a -> out = out.replace("{$i}", a.toString()) }
        return out
    }

    fun supportedLanguages(): Set<String> = tables.keys + "en"
}

/** Shorthand for [I18n.tr]. */
fun tr(text: String, vararg args: Any): String = I18n.tr(text, *args)
