package es.digirouter.app

import es.routerapp.protocol.RouterClient
import es.routerapp.protocol.parental.FilterLevel
import es.routerapp.protocol.parental.ParentalControlEngine
import es.routerapp.protocol.parental.ParentalProfile
import es.routerapp.protocol.parental.ParentalRouterRpc
import es.routerapp.protocol.parental.ProfileStatus
import es.routerapp.protocol.parental.VisitedDomain
import es.routerapp.protocol.parental.WebInsights
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParentalControlViewModelTest {

    private class FakeRpc : ParentalRouterRpc {
        override fun get(oid: String, attrs: Map<String, String>, stack: String): JsonElement = JsonObject(emptyMap())
        override fun getList(oid: String, attrs: Map<String, String>, stack: String): JsonElement = JsonArray(
            listOf(
                JsonObject(
                    mapOf(
                        "stack" to JsonPrimitive("1,0,0,0,0,0"),
                        "name" to JsonPrimitive("Child1"),
                        "filterLevel" to JsonPrimitive("0"),
                        "status" to JsonPrimitive("1"),
                    )
                )
            )
        )
        override fun getSubList(oid: String, attrs: Map<String, String>, stack: String): JsonElement = JsonArray(emptyList())
        override fun set(oid: String, attrs: Map<String, String>, stack: String): JsonElement = JsonObject(emptyMap())
        override fun add(oid: String, attrs: Map<String, String>, parentStack: String): JsonElement = JsonObject(emptyMap())
        override fun delete(oid: String, stack: String): JsonElement = JsonObject(emptyMap())
        override fun operate(oid: String, attrs: Map<String, String>, stack: String): JsonElement = JsonObject(emptyMap())
    }

    @Test
    fun `initial load sets Error when client is null`() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val vm = ParentalControlViewModel(
            clientProvider = { null },
            coroutineScope = testScope,
            ioDispatcher = Dispatchers.Unconfined,
        )
        val state = vm.state.value
        assertEquals("Router not connected", state.errorMessage)
    }

    @Test
    fun `togglePause toggles status in ui state`() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val fakeClient = RouterClient("http://127.0.0.1")
        val fakeEngine = ParentalControlEngine(FakeRpc())

        val vm = ParentalControlViewModel(
            clientProvider = { fakeClient },
            engineProvider = { fakeEngine },
            coroutineScope = testScope,
            ioDispatcher = Dispatchers.Unconfined,
        )

        val profile = ParentalProfile(stack = "1,0,0,0,0,0", name = "Child1", status = ProfileStatus.ACTIVE)
        vm.togglePause(profile)

        val state = vm.state.value
        val updated = state.profiles.firstOrNull { it.stack == "1,0,0,0,0,0" }
        assertNotNull(updated)
        assertEquals(ProfileStatus.PAUSED, updated?.status)
    }

    @Test
    fun `clearError clears error string`() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val vm = ParentalControlViewModel(
            clientProvider = { null },
            coroutineScope = testScope,
            ioDispatcher = Dispatchers.Unconfined,
        )
        assertNotNull(vm.state.value.errorMessage)
        vm.clearError()
        assertNull(vm.state.value.errorMessage)
    }
}
