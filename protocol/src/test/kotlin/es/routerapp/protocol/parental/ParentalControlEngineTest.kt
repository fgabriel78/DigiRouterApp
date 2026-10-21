package es.routerapp.protocol.parental

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParentalControlEngineTest {

    private open class FakeParentalRouterRpc : ParentalRouterRpc {
        val calls = mutableListOf<String>()
        val sets = mutableListOf<Pair<String, Map<String, String>>>()
        val adds = mutableListOf<Pair<String, Map<String, String>>>()
        val deletes = mutableListOf<Pair<String, String>>()
        val ops = mutableListOf<String>()

        override fun get(oid: String, attrs: Map<String, String>, stack: String): JsonElement {
            calls.add("get:$oid:$stack")
            return when (oid) {
                "DEV2_PC_WEEKDAY" -> JsonObject(
                    mapOf(
                        "timeLimitEnable" to JsonPrimitive("1"),
                        "timeLimit" to JsonPrimitive("90"),
                        "bedTimeEnable" to JsonPrimitive("1"),
                        "bedTimeBegin" to JsonPrimitive("1320"),
                        "bedTimeEnd" to JsonPrimitive("360"),
                        "index" to JsonPrimitive("31"),
                    )
                )
                "DEV2_PC_WEEKEND" -> JsonObject(
                    mapOf(
                        "timeLimitEnable" to JsonPrimitive("0"),
                        "timeLimit" to JsonPrimitive("180"),
                        "bedTimeEnable" to JsonPrimitive("1"),
                        "bedTimeBegin" to JsonPrimitive("1380"),
                        "bedTimeEnd" to JsonPrimitive("480"),
                        "index" to JsonPrimitive("96"),
                    )
                )
                "DEV2_PC_FILTERCONTENT" -> JsonObject(
                    mapOf(
                        "adultContentEnable" to JsonPrimitive("1"),
                        "gambleEnable" to JsonPrimitive("1"),
                        "socialNetworkingEnable" to JsonPrimitive("1"),
                    )
                )
                else -> JsonObject(emptyMap())
            }
        }

        override fun getList(oid: String, attrs: Map<String, String>, stack: String): JsonElement {
            calls.add("getList:$oid:$stack")
            return when (oid) {
                "DEV2_PC_PROFILE" -> JsonArray(
                    listOf(
                        JsonObject(
                            mapOf(
                                "stack" to JsonPrimitive("1,0,0,0,0,0"),
                                "name" to JsonPrimitive("Leo"),
                                "filterLevel" to JsonPrimitive("1"),
                                "status" to JsonPrimitive("1"),
                            )
                        )
                    )
                )
                else -> JsonArray(emptyList())
            }
        }

        override fun getSubList(oid: String, attrs: Map<String, String>, stack: String): JsonElement {
            calls.add("getSubList:$oid:${attrs["pstack"]}")
            return when (oid) {
                "DEV2_PC_PROFILE_DEVICE" -> JsonArray(
                    listOf(
                        JsonObject(
                            mapOf(
                                "stack" to JsonPrimitive("1,1,0,0,0,0"),
                                "deviceMac" to JsonPrimitive("AA:BB:CC:DD:EE:01"),
                                "entryName" to JsonPrimitive("Leo iPad"),
                                "clientType" to JsonPrimitive("Tablet"),
                            )
                        )
                    )
                )
                "DEV2_PC_BLACKLIST" -> JsonArray(
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

        override fun set(oid: String, attrs: Map<String, String>, stack: String): JsonElement {
            sets.add(oid to attrs)
            return JsonObject(emptyMap())
        }

        override fun add(oid: String, attrs: Map<String, String>, parentStack: String): JsonElement {
            adds.add(oid to attrs)
            return JsonObject(mapOf("stack" to JsonPrimitive("2,0,0,0,0,0")))
        }

        override fun delete(oid: String, stack: String): JsonElement {
            deletes.add(oid to stack)
            return JsonObject(emptyMap())
        }

        override fun operate(oid: String, attrs: Map<String, String>, stack: String): JsonElement {
            ops.add(oid)
            return JsonObject(emptyMap())
        }
    }

    @Test
    fun testLoadProfiles() {
        val fakeRpc = FakeParentalRouterRpc()
        val engine = ParentalControlEngine(fakeRpc)

        val result = engine.loadProfiles()
        assertTrue(result.isSuccess)
        val profiles = result.getOrThrow()
        assertEquals(1, profiles.size)

        val leo = profiles.first()
        assertEquals("Leo", leo.name)
        assertEquals("1,0,0,0,0,0", leo.stack)
        assertEquals(ProfileStatus.ACTIVE, leo.status)
        assertEquals(FilterLevel.PRE_TEEN, leo.filterLevel)
        assertEquals(1, leo.devices.size)
        assertEquals("AA:BB:CC:DD:EE:01", leo.devices.first().deviceMac)
        assertTrue(leo.weekdayLimit.timeLimitEnable)
        assertEquals(90, leo.weekdayLimit.timeLimitMinutes)
        assertEquals(1, leo.blackList.size)
        assertEquals("tiktok.com", leo.blackList.first().url)
    }

    @Test
    fun testTogglePause() {
        val fakeRpc = FakeParentalRouterRpc()
        val engine = ParentalControlEngine(fakeRpc)
        val profile = ParentalProfile(stack = "1,0,0,0,0,0", name = "Leo", status = ProfileStatus.ACTIVE)

        val result = engine.togglePause(profile)
        assertTrue(result.isSuccess)
        assertEquals(ProfileStatus.PAUSED, result.getOrThrow())
        assertTrue(fakeRpc.sets.any { it.first == "DEV2_PC_PROFILE" && it.second["status"] == "0" })
        assertTrue(fakeRpc.ops.contains("ACT_SAVE_CFG"))
    }

    @Test
    fun testDeleteProfile() {
        val fakeRpc = FakeParentalRouterRpc()
        val engine = ParentalControlEngine(fakeRpc)

        val result = engine.deleteProfile("1,0,0,0,0,0")
        assertTrue(result.isSuccess)
        assertTrue(fakeRpc.deletes.any { it.first == "DEV2_PC_PROFILE" && it.second == "1,0,0,0,0,0" })
        assertTrue(fakeRpc.ops.contains("ACT_SAVE_CFG"))
    }

    @Test
    fun testSaveProfileNewStandard() {
        val fakeRpc = FakeParentalRouterRpc()
        val engine = ParentalControlEngine(fakeRpc)
        val profile = ParentalProfile(
            name = "  Sofía  ",
            filterLevel = FilterLevel.PRE_TEEN,
            status = ProfileStatus.ACTIVE,
        )

        val result = engine.saveProfile(profile, isNew = true)
        assertTrue(result.isSuccess)
        val saved = result.getOrThrow()
        assertEquals("Sofía", saved.name)
        assertEquals("2,0,0,0,0,0", saved.stack)
        assertTrue(fakeRpc.adds.any { it.first == "DEV2_PC_PROFILE" && it.second["name"] == "Sofía" && it.second["filterLevel"] == "1" })
        assertTrue(fakeRpc.ops.contains("ACT_SAVE_CFG"))
    }

    @Test
    fun testSaveProfileFallbackOn4734() {
        val fakeRpc = object : FakeParentalRouterRpc() {
            var failCompoundAdd = true
            override fun add(oid: String, attrs: Map<String, String>, parentStack: String): JsonElement {
                if (oid == "DEV2_PC_PROFILE" && failCompoundAdd && attrs.containsKey("filterLevel")) {
                    failCompoundAdd = false
                    throw es.routerapp.protocol.RouterException(4734, "Router error 4734 for ao DEV2_PC_PROFILE")
                }
                return super.add(oid, attrs, parentStack)
            }
        }
        val engine = ParentalControlEngine(fakeRpc)
        val profile = ParentalProfile(
            name = "Leo",
            filterLevel = FilterLevel.CHILD,
            status = ProfileStatus.ACTIVE,
        )

        val result = engine.saveProfile(profile, isNew = true)
        assertTrue(result.isSuccess)
        val saved = result.getOrThrow()
        assertEquals("Leo", saved.name)
        // Verify fallback succeeded with name only, followed by set for filterLevel and status
        assertTrue(fakeRpc.adds.any { it.first == "DEV2_PC_PROFILE" && it.second.keys == setOf("name") })
        assertTrue(fakeRpc.sets.any { it.first == "DEV2_PC_PROFILE" && it.second["filterLevel"] == "0" && it.second["status"] == "1" })
        assertTrue(fakeRpc.ops.contains("ACT_SAVE_CFG"))
    }

    @Test
    fun testSaveProfileNameValidation() {
        val fakeRpc = FakeParentalRouterRpc()
        val engine = ParentalControlEngine(fakeRpc)

        val blankResult = engine.saveProfile(ParentalProfile(name = "   "), isNew = true)
        assertTrue(blankResult.isFailure)

        val invalidCharsResult = engine.saveProfile(ParentalProfile(name = "Leo:iPad?"), isNew = true)
        assertTrue(invalidCharsResult.isFailure)
    }
}
