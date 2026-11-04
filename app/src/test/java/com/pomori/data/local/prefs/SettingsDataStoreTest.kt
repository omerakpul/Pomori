package com.pomori.data.local.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Bellek içi bir DataStore ile: varsayılanlar, okuma/yazma ve aralık kırpma. Gerçek dosyaya yazma
 * DataStore kütüphanesinin işidir (dosyalı testler Windows'ta geçici dosyayı yeniden adlandıramaz).
 */
class SettingsDataStoreTest {

    private class InMemoryDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }

    private val dataStore = InMemoryDataStore()
    private val store = SettingsDataStore(dataStore)

    @Test
    fun `defaults match the data model document`() = runTest {
        assertFalse(store.onboardingDone.first())
        assertEquals(25, store.focusMinutes.first())
        assertEquals(5, store.breakMinutes.first())
        assertEquals(7, store.retentionDays.first())
    }

    @Test
    fun `written values are read back`() = runTest {
        store.setFocusMinutes(50)
        store.setBreakMinutes(10)
        store.setRetentionDays(14)

        assertEquals(50, store.focusMinutes.first())
        assertEquals(10, store.breakMinutes.first())
        assertEquals(14, store.retentionDays.first())
    }

    @Test
    fun `the boundaries of every range are accepted`() = runTest {
        store.setFocusMinutes(1)
        assertEquals(1, store.focusMinutes.first())
        store.setFocusMinutes(120)
        assertEquals(120, store.focusMinutes.first())
        store.setBreakMinutes(60)
        assertEquals(60, store.breakMinutes.first())
        store.setRetentionDays(30)
        assertEquals(30, store.retentionDays.first())
    }

    @Test
    fun `written values outside the range are clamped`() = runTest {
        store.setFocusMinutes(0)
        assertEquals(1, store.focusMinutes.first())
        store.setFocusMinutes(10_000)
        assertEquals(120, store.focusMinutes.first())
        store.setBreakMinutes(-5)
        assertEquals(1, store.breakMinutes.first())
        store.setRetentionDays(365)
        assertEquals(30, store.retentionDays.first())
    }

    @Test
    fun `a corrupt out of range value already stored is clamped when read`() = runTest {
        dataStore.edit { it[intPreferencesKey("focus_minutes")] = 9_999 }
        dataStore.edit { it[intPreferencesKey("retention_days")] = -3 }

        assertEquals(120, store.focusMinutes.first())
        assertEquals(1, store.retentionDays.first())
    }

    @Test
    fun `settings do not disturb each other or the onboarding flag`() = runTest {
        store.setOnboardingDone()
        store.setFocusMinutes(30)

        assertTrue(store.onboardingDone.first())
        assertEquals(30, store.focusMinutes.first())
        assertEquals(5, store.breakMinutes.first())
        assertEquals(7, store.retentionDays.first())
    }

    @Test
    fun `the stored keys are the ones documented in the data model`() = runTest {
        store.setFocusMinutes(40)
        store.setBreakMinutes(8)
        store.setRetentionDays(9)

        val prefs = dataStore.data.first()
        assertEquals(40, prefs[intPreferencesKey("focus_minutes")])
        assertEquals(8, prefs[intPreferencesKey("break_minutes")])
        assertEquals(9, prefs[intPreferencesKey("retention_days")])
    }

    @Test
    fun `the long break defaults to fifteen minutes`() = runTest {
        assertEquals(15, store.longBreakMinutes.first())
    }

    @Test
    fun `a written long break is read back and clamped to the range`() = runTest {
        store.setLongBreakMinutes(25)
        assertEquals(25, store.longBreakMinutes.first())

        store.setLongBreakMinutes(0)
        assertEquals(1, store.longBreakMinutes.first())

        store.setLongBreakMinutes(500)
        assertEquals(120, store.longBreakMinutes.first())
    }

    @Test
    fun `no preset is selected until one is chosen`() = runTest {
        assertEquals(null, store.selectedPresetId.first())
    }

    @Test
    fun `the selected preset is stored and read back`() = runTest {
        store.setSelectedPresetId(7)

        assertEquals(7L, store.selectedPresetId.first())
    }

    @Test
    fun `restoring notifications is on by default`() = runTest {
        assertTrue(store.restoreNotifications.first())
    }

    @Test
    fun `the restore setting is stored and read back`() = runTest {
        store.setRestoreNotifications(false)
        assertFalse(store.restoreNotifications.first())

        store.setRestoreNotifications(true)
        assertTrue(store.restoreNotifications.first())
    }
}
