package com.pomori.domain.usecase

import com.pomori.domain.model.SessionType
import com.pomori.domain.repository.ActiveSessionStore
import com.pomori.domain.repository.SessionRepository
import javax.inject.Inject

/**
 * Çalışan odak seansına bir kesinti yazar. Süreyi durdurmaz (pause yoktur); yalnızca sayar.
 * Aktif seans yoksa ya da mola ise hiçbir şey yapmaz.
 */
class RecordInterruption @Inject constructor(
    private val sessions: SessionRepository,
    private val activeStore: ActiveSessionStore,
) {
    suspend operator fun invoke(): Boolean {
        val active = activeStore.current() ?: return false
        if (active.type != SessionType.FOCUS) return false
        return sessions.recordInterruption(active.sessionId)
    }
}
