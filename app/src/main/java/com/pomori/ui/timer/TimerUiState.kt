package com.pomori.ui.timer

import com.pomori.domain.timer.TimerDefaults
import com.pomori.domain.timer.TimerState
import com.pomori.domain.timer.remainingMs

enum class TimerPhase { IDLE, FOCUS, BREAK_PENDING, BREAK }

data class TimerUiState(
    val phase: TimerPhase = TimerPhase.IDLE,
    val remainingMs: Long = TimerDefaults.FOCUS_MS,
    val totalMs: Long = TimerDefaults.FOCUS_MS,
) {
    /** 1.0 = süre dolu, 0.0 = bitti. */
    val progress: Float
        get() = if (totalMs <= 0) 0f else (remainingMs.toFloat() / totalMs).coerceIn(0f, 1f)
}

internal fun TimerState.toUiState(now: Long): TimerUiState = when (this) {
    TimerState.Idle -> TimerUiState()
    is TimerState.Focus -> TimerUiState(TimerPhase.FOCUS, remainingMs(now), durationMs)
    TimerState.BreakPending -> TimerUiState(TimerPhase.BREAK_PENDING, 0, TimerDefaults.BREAK_MS)
    is TimerState.Break -> TimerUiState(TimerPhase.BREAK, remainingMs(now), durationMs)
}

/** Kalan süreyi "MM:SS" yazar; saniyeyi yukarı yuvarlar (00:00 yalnızca gerçekten bitince). */
fun formatRemaining(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0) + 999) / 1000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
