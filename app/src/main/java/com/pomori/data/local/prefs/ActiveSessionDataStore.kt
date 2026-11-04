package com.pomori.data.local.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pomori.di.ActiveSessionPrefs
import com.pomori.domain.model.ActiveSession
import com.pomori.domain.model.SessionType
import com.pomori.domain.repository.ActiveSessionStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Aktif seansın bitiş zamanını diske yazar. Süreç öldürülse bile uygulama açıldığında
 * kalan süre buradaki `active_end_at` değerinden hesaplanır.
 */
@Singleton
class ActiveSessionDataStore @Inject constructor(
    @ActiveSessionPrefs private val dataStore: DataStore<Preferences>,
) : ActiveSessionStore {

    override val active: Flow<ActiveSession?> =
        dataStore.data.map { it.toActiveSession() }.distinctUntilChanged()

    override suspend fun current(): ActiveSession? = active.first()

    override suspend fun set(session: ActiveSession) {
        dataStore.edit {
            it[KEY_ID] = session.sessionId
            it[KEY_END_AT] = session.endAt
            it[KEY_TYPE] = session.type.name
            it[KEY_PLANNED_MS] = session.plannedDurationMs
            it[KEY_LONG_BREAK] = session.isLongBreak
        }
    }

    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private fun Preferences.toActiveSession(): ActiveSession? {
        val id = this[KEY_ID] ?: return null
        if (id == NO_SESSION) return null
        val endAt = this[KEY_END_AT] ?: return null
        val type = this[KEY_TYPE]?.let { name -> SessionType.entries.firstOrNull { it.name == name } }
            ?: return null
        val planned = this[KEY_PLANNED_MS] ?: return null
        return ActiveSession(
            sessionId = id,
            type = type,
            endAt = endAt,
            plannedDurationMs = planned,
            // Alan sonradan eklendi: eski kayıtlarda yok, kısa mola sayılır.
            isLongBreak = this[KEY_LONG_BREAK] ?: false,
        )
    }

    private companion object {
        const val NO_SESSION = -1L
        val KEY_ID = longPreferencesKey("active_session_id")
        val KEY_END_AT = longPreferencesKey("active_end_at")
        val KEY_TYPE = stringPreferencesKey("active_type")
        val KEY_LONG_BREAK = booleanPreferencesKey("active_long_break")
        val KEY_PLANNED_MS = longPreferencesKey("active_planned_ms")
    }
}
