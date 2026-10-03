package es.routerapp.protocol.pages

import es.routerapp.protocol.RouterClient
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Translates declarative [Section]s into data-model operations against a logged-in [RouterClient]. */
class PageEngine(val client: RouterClient) {

    /** Loads every instance of [section] (reads only). */
    fun load(section: Section): List<Instance> {
        val el = if (section.multi) client.getList(section.oid) else client.get(section.oid)
        var items = toInstances(el)
        section.join?.let { j ->
            val remote = toInstances(client.getList(j.oid))
            items = items.map { inst ->
                val match = remote.firstOrNull { it.values[j.remoteKey] == inst.values[j.localKey] }
                if (match == null) inst
                else inst.copy(values = inst.values + match.values.mapKeys { (k, _) -> j.prefix + k })
            }
        }
        section.itemFilter?.let { f -> items = items.filter { f(it.values) } }
        return items
    }

    /**
     * Saves the differences between [original] and [edited]. Returns the attributes actually sent
     * (empty = nothing to do). [section.afterSave] operations run once afterwards.
     */
    fun save(section: Section, original: Instance, edited: Values, all: List<Instance> = listOf(original)): Map<String, String> {
        require(!section.readOnly) { "Section ${section.id} is read-only" }
        val editable = section.fields.filter { it.type != FieldType.Info }.map { it.key }.toSet()
        val changed = linkedMapOf<String, String>()
        for ((k, v) in edited) if (k in editable && original.values[k] != v) changed[k] = v
        if (changed.isEmpty()) return emptyMap()
        section.saveHook?.invoke(original.values, original.values + edited, changed)
        val targets = if (section.applyToAll) all else listOf(original)
        for (t in targets) client.set(section.oid, changed, t.stack)
        for (op in section.afterSave) client.operate(op)
        return changed
    }

    fun add(section: Section, values: Values) {
        val spec = requireNotNull(section.add) { "Section ${section.id} does not support adding" }
        val data = linkedMapOf<String, String>()
        for ((k, v) in spec.defaults) data[k] = resolveDynamic(v)
        data.putAll(values)
        val pstack = spec.parent?.let { p ->
            toInstances(client.getList(p.oid)).firstOrNull { it.values[p.matchKey] == p.matchValue }?.stack
                ?: error("Parent object not found")
        } ?: RouterClient.DEFAULT_STACK
        client.add(section.oid, data, pstack)
        for (op in section.afterSave) client.operate(op)
    }

    fun delete(section: Section, instance: Instance) {
        require(section.deletable) { "Section ${section.id} does not support deleting" }
        client.delete(section.oid, instance.stack)
    }

    fun run(action: PageAction) {
        client.operate(action.oid, action.attrs)
    }

    /** `@activeWan` → name of the connected internet WAN connection (PPPoE/DHCP preferred). */
    private fun resolveDynamic(v: String): String = when (v) {
        "@activeWan" -> activeWanName()
        else -> v
    }

    fun activeWanName(): String {
        val wans = toInstances(client.getList("DEV2_ADT_WAN")).map { it.values }
        val connected = wans.filter { it["connStatusV4"] == "Connected" && it["name"]?.startsWith("usb_") == false }
        return (connected.firstOrNull { it["connType"] in setOf("PPPoE", "DynamicIP", "DHCP") }
            ?: connected.firstOrNull() ?: wans.firstOrNull())?.get("name") ?: ""
    }

    companion object {
        fun toInstances(el: JsonElement): List<Instance> = when (el) {
            is JsonArray -> el.mapNotNull { (it as? JsonObject)?.let(::toInstance) }
            is JsonObject -> if (el.isEmpty()) emptyList() else listOf(toInstance(el))
            else -> emptyList()
        }

        private fun toInstance(o: JsonObject): Instance {
            val map = o.mapValues { (_, v) -> (v as? JsonPrimitive)?.content ?: v.toString() }
            return Instance(map, map["stack"] ?: RouterClient.DEFAULT_STACK)
        }
    }
}
