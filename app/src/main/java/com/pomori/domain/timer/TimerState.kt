package com.pomori.domain.timer

import com.pomori.domain.model.ActiveSession
import com.pomori.domain.model.SessionType

object TimerDefaults {
    const val FOCUS_MS: Long = 25 * 60 * 1000L
    const val BREAK_MS: Long = 5 * 60 * 1000L
}

sealed interface TimerState {
    data object Idle : TimerState

    data class Focus(val endAt: Long, val durationMs: Long) : TimerState

    /** Odak bitti, kullanıcı molayı başlatmayı ya da atlamayı bekliyor. */
    data object BreakPending : TimerState

    data class Break(val endAt: Long, val durationMs: Long) : TimerState
}

sealed interface TimerEvent {
    data class StartFocus(val durationMs: Long) : TimerEvent
    data class StartBreak(val durationMs: Long) : TimerEvent

    /** Süre doldu. Yalnızca `now >= endAt` ise geçişi tetikler. */
    data object TimeUp : TimerEvent

    data object Cancel : TimerEvent
    data object Skip : TimerEvent
}

/** Kalan süre (ms). Çalışan bir seans yoksa 0. */
fun TimerState.remainingMs(now: Long): Long = when (this) {
    is TimerState.Focus -> (endAt - now).coerceAtLeast(0)
    is TimerState.Break -> (endAt - now).coerceAtLeast(0)
    TimerState.Idle, TimerState.BreakPending -> 0
}

/** Kayıtlı aktif seansın, zaman ilerlemesi uygulanmamış ham durumu. */
fun ActiveSession?.toRawState(): TimerState = when {
    this == null -> TimerState.Idle
    type == SessionType.FOCUS -> TimerState.Focus(endAt, plannedDurationMs)
    else -> TimerState.Break(endAt, plannedDurationMs)
}

/**
 * Kayıtlı seansı `now` anına göre çözer. Süreç öldükten sonra uygulama açıldığında
 * da doğru durumu (örn. süresi dolmuş odak -> BreakPending) verir.
 */
fun ActiveSession?.toTimerState(now: Long): TimerState =
    TimerStateMachine.reduce(toRawState(), TimerEvent.TimeUp, now)
