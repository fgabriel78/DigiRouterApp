package es.routerapp.protocol.pages

import es.routerapp.protocol.i18n.tr

/**
 * Declarative description of the router's configuration screens. The UI renders any [Page]
 * generically and [PageEngine] translates it to data-model operations (`go/gl/so/ao/do/op`).
 */

data class Option(val value: String, val label: String)

typealias Values = Map<String, String>

sealed interface FieldType {
    data object Switch : FieldType
    data class Text(val minLen: Int = 0, val maxLen: Int = 255) : FieldType
    data class Secret(val minLen: Int = 0, val maxLen: Int = 128) : FieldType
    data class Number(val min: Long, val max: Long, val unit: String? = null) : FieldType
    data class Choice(val options: (Values) -> List<Option>) : FieldType
    data object Ipv4 : FieldType
    data object Mac : FieldType
    /** Minutes since midnight, shown as HH:MM. */
    data object TimeOfDay : FieldType
    data object Info : FieldType
}

data class Field(
    val key: String,
    val label: String,
    val type: FieldType,
    val help: String? = null,
    val visibleIf: ((Values) -> Boolean)? = null,
)

/** Merges attributes of another list object (matched by one key) into each instance, prefixing keys. */
data class Join(val oid: String, val localKey: String, val remoteKey: String, val prefix: String)

/** Parent instance that must be referenced (`pstack`) when adding a child object. */
data class ParentRef(val oid: String, val matchKey: String, val matchValue: String)

data class AddSpec(
    val title: String,
    val fields: List<Field>,
    /** Values always sent. A value starting with "@" is resolved by [PageEngine] (see `resolveDynamic`). */
    val defaults: Values = emptyMap(),
    val parent: ParentRef? = null,
)

data class Section(
    val id: String,
    val title: String,
    val oid: String,
    val fields: List<Field>,
    val multi: Boolean = false,
    val itemTitle: (Values) -> String = { it["band"] ?: "" },
    val itemFilter: ((Values) -> Boolean)? = null,
    val join: Join? = null,
    /** Show a single card (first instance) and write the changes to every instance. */
    val applyToAll: Boolean = false,
    val readOnly: Boolean = false,
    /** When [fields] is empty, show every attribute returned by the router (read-only). */
    val showAll: Boolean = false,
    /** `ACT_*` operations executed after a successful save. */
    val afterSave: List<String> = emptyList(),
    val saveWarning: String? = null,
    val saveHook: ((original: Values, edited: Values, changed: MutableMap<String, String>) -> Unit)? = null,
    val add: AddSpec? = null,
    val deletable: Boolean = false,
    val note: String? = null,
)

data class PageAction(
    val title: String,
    val oid: String,
    val confirm: String,
    val attrs: Values = emptyMap(),
    val danger: Boolean = false,
)

/** [title] is English source text; translate it with `tr(group.title)` when displaying. */
enum class Group(val title: String) {
    STATUS("Status"),
    WIFI("Wi-Fi"),
    NETWORK("Local network"),
    NAT("Ports and NAT"),
    SECURITY("Security"),
    STORAGE("USB and storage"),
    SYSTEM("System"),
}

data class Page(
    val id: String,
    val title: String,
    val group: Group,
    val description: String? = null,
    val adminOnly: Boolean = false,
    val sections: List<Section> = emptyList(),
    val actions: List<PageAction> = emptyList(),
    /** Rendered by a dedicated screen instead of the generic form renderer. */
    val custom: Boolean = false,
)

/** One object instance returned by the router. */
data class Instance(val values: Values, val stack: String)

fun Field.validate(raw: String, all: Values): String? {
    val v = raw.trim()
    return when (val t = type) {
        is FieldType.Text -> when {
            v.length < t.minLen -> tr("Minimum {0} characters", t.minLen)
            v.length > t.maxLen -> tr("Maximum {0} characters", t.maxLen)
            else -> null
        }
        is FieldType.Secret -> when {
            v.length < t.minLen -> tr("Minimum {0} characters", t.minLen)
            v.length > t.maxLen -> tr("Maximum {0} characters", t.maxLen)
            else -> null
        }
        is FieldType.Number -> {
            val n = v.toLongOrNull()
            if (n == null || n < t.min || n > t.max) tr("Value between {0} and {1}", t.min, t.max) else null
        }
        FieldType.Ipv4 -> if (IPV4.matches(v)) null else tr("Invalid IPv4 address")
        FieldType.Mac -> if (MAC.matches(v)) null else tr("Invalid MAC address (AA:BB:CC:DD:EE:FF)")
        else -> null
    }
}

private val IPV4 = Regex("""^((25[0-5]|2[0-4]\d|1?\d?\d)\.){3}(25[0-5]|2[0-4]\d|1?\d?\d)$""")
private val MAC = Regex("""^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$""")
