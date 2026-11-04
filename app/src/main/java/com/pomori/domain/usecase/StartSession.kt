package com.pomori.domain.usecase

import com.pomori.domain.Clock
import com.pomori.domain.model.ActiveSession
import com.pomori.domain.model.FocusSession
import com.pomori.domain.model.SessionStatus
import com.pomori.domain.model.SessionType
import com.pomori.domain.model.normalizeLabel
import com.pomori.domain.repository.ActiveSessionStore
import com.pomori.domain.repository.SessionRepository
import com.pomori.domain.timer.TimerEvent
import com.pomori.domain.timer.TimerState
import com.pomori.domain.timer.TimerStateMachine
import com.pomori.domain.timer.toTimerState
import javax.inject.Inject

/**
 * Yeni odak ya da mola seansı başlatır. Geçiş durum makinesine göre geçerli değilse
 * (örn. odak sürerken tekrar başlat) hiçbir şey yazmaz ve mevcut durumu döner.
 */
class StartSession @Inject constructor(
    private val sessions: SessionRepository,
    private val activeStore: ActiveSessionStore,
    private val endSession: EndSession,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        type: SessionType,
        durationMs: Long,
        isLongBreak: Boolean = false,
        presetId: Long? = null,
        label: String? = null,
    ): TimerState {
        // Süresi dolmuş ama henüz kapatılmamış bir önceki seans varsa önce onu kapat.
        endSession(EndReason.TIME_UP)

        val now = clock.now()
        val current = activeStore.current().toTimerState(now)
        val event = when (type) {
            SessionType.FOCUS -> TimerEvent.StartFocus(durationMs)
            SessionType.BREAK -> TimerEvent.StartBreak(durationMs, isLongBreak)
        }
        val next = TimerStateMachine.reduce(current, event, now)
        if (next == current) return current

        val endAt = when (next) {
            is TimerState.Focus -> next.endAt
            is TimerState.Break -> next.endAt
            else -> return current
        }
        val id = sessions.insert(
            FocusSession(
                type = type,
                startedAt = now,
                plannedDurationMs = durationMs,
                status = SessionStatus.RUNNING,
                presetId = presetId,
                // Etiket yalnızca odak seansına aittir; mola etiketlenmez.
                label = if (type == SessionType.FOCUS) normalizeLabel(label) else null,
            ),
        )
        activeStore.set(
            ActiveSession(
                sessionId = id,
                type = type,
                endAt = endAt,
                plannedDurationMs = durationMs,
                isLongBreak = isLongBreak && type == SessionType.BREAK,
            ),
        )
        return next
    }
}
