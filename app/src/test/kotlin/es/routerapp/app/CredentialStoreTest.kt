package es.routerapp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialStoreTest {

    @Test
    fun `store starts empty when no initial password provided`() {
        val store = FakeCredentialStore()
        assertNull(store.getSavedPassword())
        assertFalse(store.hasSavedPassword())
    }

    @Test
    fun `savePassword stores password and hasSavedPassword returns true`() {
        val store = FakeCredentialStore()
        store.savePassword("admin123")
        assertEquals("admin123", store.getSavedPassword())
        assertTrue(store.hasSavedPassword())
    }

    @Test
    fun `savePassword overwrites existing password`() {
        val store = FakeCredentialStore("old_pass")
        assertEquals("old_pass", store.getSavedPassword())
        store.savePassword("new_pass")
        assertEquals("new_pass", store.getSavedPassword())
    }

    @Test
    fun `clear removes stored password`() {
        val store = FakeCredentialStore("secret")
        assertTrue(store.hasSavedPassword())
        store.clear()
        assertNull(store.getSavedPassword())
        assertFalse(store.hasSavedPassword())
    }
}
