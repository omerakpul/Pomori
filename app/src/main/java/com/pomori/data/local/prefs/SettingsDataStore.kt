package com.pomori.data.local.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.pomori.di.SettingsPrefs
import com.pomori.domain.model.SettingsDefaults
import com.pomori.domain.repository.SettingsStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Singleton
class SettingsDataStore @Inject constructor(
    @SettingsPrefs private val dataStore: DataStore<Preferences>,
) : SettingsStore {

    override val onboardingDone: Flow<Boolean> =
        dataStore.data.map { it[KEY_ONBOARDING_DONE] ?: false }.distinctUntilChanged()

    override val focusMinutes: Flow<Int> =
        intSetting(KEY_FOCUS_MINUTES, SettingsDefaults.FOCUS_MINUTES, SettingsDefaults.FOCUS_RANGE)

    override val breakMinutes: Flow<Int> =
        intSetting(KEY_BREAK_MINUTES, SettingsDefaults.BREAK_MINUTES, SettingsDefaults.BREAK_RANGE)

    override val longBreakMinutes: Flow<Int> =
        intSetting(KEY_LONG_BREAK_MINUTES, SettingsDefaults.LONG_BREAK_MINUTES, SettingsDefaults.LONG_BREAK_RANGE)

    override val retentionDays: Flow<Int> =
        intSetting(KEY_RETENTION_DAYS, SettingsDefaults.RETENTION_DAYS, SettingsDefaults.RETENTION_RANGE)

    override suspend fun setOnboardingDone() {
        dataStore.edit { it[KEY_ONBOARDING_DONE] = true }
    }

    override suspend fun setFocusMinutes(minutes: Int) =
        write(KEY_FOCUS_MINUTES, minutes, SettingsDefaults.FOCUS_RANGE)

    override suspend fun setBreakMinutes(minutes: Int) =
        write(KEY_BREAK_MINUTES, minutes, SettingsDefaults.BREAK_RANGE)

    override suspend fun setLongBreakMinutes(minutes: Int) =
        write(KEY_LONG_BREAK_MINUTES, minutes, SettingsDefaults.LONG_BREAK_RANGE)

    override suspend fun setRetentionDays(days: Int) =
        write(KEY_RETENTION_DAYS, days, SettingsDefaults.RETENTION_RANGE)

    /** Eksikse varsayılan, aralık dışıysa en yakın sınır: akış her zaman geçerli bir değer verir. */
    private fun intSetting(key: Preferences.Key<Int>, default: Int, range: IntRange): Flow<Int> =
        dataStore.data.map { (it[key] ?: default).coerceIn(range) }.distinctUntilChanged()

    private suspend fun write(key: Preferences.Key<Int>, value: Int, range: IntRange) {
        dataStore.edit { it[key] = value.coerceIn(range) }
    }

    private companion object {
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val KEY_FOCUS_MINUTES = intPreferencesKey("focus_minutes")
        val KEY_BREAK_MINUTES = intPreferencesKey("break_minutes")
        val KEY_LONG_BREAK_MINUTES = intPreferencesKey("long_break_minutes")
        val KEY_RETENTION_DAYS = intPreferencesKey("retention_days")
    }
}
