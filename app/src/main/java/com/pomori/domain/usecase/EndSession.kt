package com.pomori.domain.usecase

import com.pomori.domain.Clock
import com.pomori.domain.model.ActiveSession
import com.pomori.domain.model.SessionStatus
import com.pomori.domain.repository.ActiveSessionStore
import com.pomori.domain.repository.SessionRepository
import com.pomori.domain.timer.TimerEvent
import com.pomori.domain.timer.TimerState
import com.pomori.domain.timer.TimerStateMachine
import com.pomori.domain.timer.toTimerState
import javax.inject.Inject

enum class EndReason {
    /** Süre doldu: seansı tamamlandı olarak kaydet. Süre dolmadıysa hiçbir şey yapmaz. */
    TIME_UP,

    /** Kullanıcı odak seansını iptal etti. */
    CANCEL,

    /** Kullanıcı molayı (ya da bekleyen molayı) atladı. */
    SKIP,
}

/**
 * Seansı bitirir ve durumu kalıcı kayıtlara yansıtır. Idempotent: hem ekran hem servis
 * aynı anda TIME_UP çağırsa bile sonuç aynıdır.
 */
class EndSession @Inject constructor(
    private val sessions: SessionRepository,
    private val activeStore: ActiveSessionStore,
    private val clock: Clock,
) {
    /** İşlemden sonraki sayaç durumunu döner. */
    suspend operator fun invoke(reason: EndReason): TimerState {
        val now = clock.now()
        val active = activeStore.current() ?: return TimerState.Idle
        val resolved = active.toTimerState(now)

        when (reason) {
            EndReason.TIME_UP -> settleTimeUp(active, resolved)

            EndReason.CANCEL -> {
                if (TimerStateMachine.reduce(resolved, TimerEvent.Cancel, now) != resolved) {
                    markEnded(active.sessionId, SessionStatus.CANCELLED, endedAt = now)
                    activeStore.clear()
                }
            }

            EndReason.SKIP -> {
                if (TimerStateMachine.reduce(resolved, TimerEvent.Skip, now) != resolved) {
                    // Bekleyen molada odak seansı zaten tamamlanmıştı; yalnızca çalışan mola iptal olur.
                    if (resolved is TimerState.Break) {
                        markEnded(active.sessionId, SessionStatus.CANCELLED, endedAt = now)
                    }
                    activeStore.clear()
                }
            }
        }
        return activeStore.current().toTimerState(now)
    }

    private suspend fun settleTimeUp(active: ActiveSession, resolved: TimerState) {
        when (resolved) {
            // Odak bitti. Kayıt, kullanıcı molayı başlatana/atlayana kadar BreakPending olarak kalır.
            TimerState.BreakPending ->
                markEnded(active.sessionId, SessionStatus.COMPLETED, endedAt = active.endAt)

            // Mola bitti.
            TimerState.Idle -> {
                markEnded(active.sessionId, SessionStatus.COMPLETED, endedAt = active.endAt)
                activeStore.clear()
            }

            else -> Unit // Süre henüz dolmadı.
        }
    }

    private suspend fun markEnded(sessionId: Long, status: SessionStatus, endedAt: Long) {
        val session = sessions.getById(sessionId) ?: return
        if (session.status != SessionStatus.RUNNING) return // idempotent
        sessions.update(session.copy(status = status, endedAt = endedAt))
    }
}
