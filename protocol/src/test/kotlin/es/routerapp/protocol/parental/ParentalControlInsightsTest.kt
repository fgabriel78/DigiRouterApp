package es.routerapp.protocol.parental

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParentalControlInsightsTest {

    private class FakeInsightsRouterRpc : ParentalRouterRpc {
        val calls = mutableListOf<String>()
        val adds = mutableListOf<Pair<String, Map<String, String>>>()
        val ops = mutableListOf<String>()

        override fun get(oid: String, attrs: Map<String, String>, stack: String): JsonElement {
            calls.add("get:$oid:$stack")
            return when (oid) {
                "DEV2_PC_VISITRANK" -> JsonObject(mapOf("dailyTimeUsed" to JsonPrimitive("105")))
                else -> JsonObject(emptyMap())
            }
        }

        override fun getList(oid: String, attrs: Map<String, String>, stack: String): JsonElement = JsonArray(emptyList())

        override fun getSubList(oid: String, attrs: Map<String, String>, stack: String): JsonElement {
            val pstack = attrs["pstack"] ?: stack
            calls.add("getSubList:$oid:$pstack")
            return when (oid) {
                "DEV2_PC_VISITRANK_ENTRY" -> JsonArray(
                    listOf(
                        JsonObject(
                            mapOf(
                                "stack" to JsonPrimitive("1,1,1,0,0,0"),
                                "url" to JsonPrimitive("youtube.com"),
                                "visitCount" to JsonPrimitive("24"),
                                "timeSpent" to JsonPrimitive("45"),
                            )
                        ),
                        JsonObject(
                            mapOf(
                                "stack" to JsonPrimitive("1,1,2,0,0,0"),
                                "url" to JsonPrimitive("wikipedia.org"),
                                "visitCount" to JsonPrimitive("10"),
                                "timeSpent" to JsonPrimitive("30"),
                            )
                        )
                    )
                )
                "DEV2_PC_BLOCKHISTORY_ENTRY" -> JsonArray(
                    listOf(
                        JsonObject(
                            mapOf(
                                "stack" to JsonPrimitive("1,1,1,0,0,0"),
                                "url" to JsonPrimitive("casino.com"),
                                "time" to JsonPrimitive("15:20"),
                            )
                        ),
                        JsonObject(
                            mapOf(
                                "stack" to JsonPrimitive("1,1,2,0,0,0"),
                                "url" to JsonPrimitive("tiktok.com"),
                                "time" to JsonPrimitive("16:05"),
                            )
                        )
                    )
                )
                "DEV2_PC_UNBLOCKEDLIST" -> JsonArray(
                    listOf(
                        JsonObject(
                            mapOf(
                                "stack" to JsonPrimitive("1,1,0,0,0,0"),
                                "url" to JsonPrimitive("tiktok.com"),
                            )
                        )
                    )
                )
                else -> JsonArray(emptyList())
            }
        }

        override fun set(oid: String, attrs: Map<String, String>, stack: String): JsonElement = JsonObject(emptyMap())

        override fun add(oid: String, attrs: Map<String, String>, parentStack: String): JsonElement {
            adds.add(oid to attrs)
            return JsonObject(mapOf("stack" to JsonPrimitive("1,2,0,0,0,0")))
        }

        override fun delete(oid: String, stack: String): JsonElement = JsonObject(emptyMap())

        override fun operate(oid: String, attrs: Map<String, String>, stack: String): JsonElement {
            ops.add(oid)
            return JsonObject(emptyMap())
        }
    }

    @Test
    fun testFetchInsightsWithDateOffset() {
        val rpc = FakeInsightsRouterRpc()
        val engine = ParentalControlEngine(rpc)

        val result = engine.fetchInsights("1,0,0,0,0,0", dateOffset = 1)
        assertTrue(result.isSuccess)
        val insights = result.getOrThrow()

        assertEquals(1, insights.dateOffset)
        assertEquals(105, insights.dailyTimeUsedMinutes)
        assertEquals(2, insights.topVisited.size)
        assertEquals("youtube.com", insights.topVisited[0].url)
        assertEquals(45, insights.topVisited[0].timeSpentMinutes)

        assertEquals(2, insights.blockedHistory.size)
        assertEquals("casino.com", insights.blockedHistory[0].url)
        assertFalse(insights.blockedHistory[0].isUnblocked)

        assertEquals("tiktok.com", insights.blockedHistory[1].url)
        assertTrue(insights.blockedHistory[1].isUnblocked)

        assertTrue(rpc.calls.contains("get:DEV2_PC_VISITRANK:1,1,0,0,0,0"))
        assertTrue(rpc.calls.contains("getSubList:DEV2_PC_VISITRANK_ENTRY:1,1,0,0,0,0"))
    }

    @Test
    fun testUnblockUrl() {
        val rpc = FakeInsightsRouterRpc()
        val engine = ParentalControlEngine(rpc)

        val result = engine.unblockUrl("1,0,0,0,0,0", "roblox.com")
        assertTrue(result.isSuccess)
        assertTrue(rpc.adds.any { it.first == "DEV2_PC_UNBLOCKEDLIST" && it.second["url"] == "roblox.com" })
        assertTrue(rpc.ops.contains("ACT_SAVE_CFG"))
    }

    @Test
    fun testAddBlacklistDomain() {
        val rpc = FakeInsightsRouterRpc()
        val engine = ParentalControlEngine(rpc)

        val result = engine.addBlacklistDomain("1,0,0,0,0,0", "discord.gg")
        assertTrue(result.isSuccess)
        assertTrue(rpc.adds.any { it.first == "DEV2_PC_BLACKLIST" && it.second["url"] == "discord.gg" })
        assertTrue(rpc.ops.contains("ACT_SAVE_CFG"))
    }
}
