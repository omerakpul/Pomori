package com.pomori.ui.timer

import com.pomori.domain.model.SettingsDefaults
import com.pomori.domain.timer.TimerDefaults
import com.pomori.domain.timer.TimerState
import com.pomori.domain.timer.remainingMs

enum class TimerPhase { IDLE, FOCUS, BREAK_PENDING, BREAK }

/** Uzun molaya kadar kaç odak seansı gerektiği; döngü noktalarının sayısı. */
const val CYCLE_LENGTH = SettingsDefaults.LONG_BREAK_EVERY

data class TimerUiState(
    val phase: TimerPhase = TimerPhase.IDLE,
    val remainingMs: Long = TimerDefaults.FOCUS_MS,
    val totalMs: Long = TimerDefaults.FOCUS_MS,
    /** Mola uzun mu? Çalışan molada kayıttan, bekleyen molada sıradaki molanın planından gelir. */
    val isLongBreak: Boolean = false,
    /** Bu Pomodoro döngüsünde tamamlanan odak seansı sayısı (0 until [CYCLE_LENGTH]). */
    val cycleDone: Int = 0,
) {
    /** Döngü noktalarında kaç tanesi dolu gösterilir: uzun mola hak edilmişken (ya da sürerken) hepsi dolu. */
    val cycleFilled: Int
        get() = if (isLongBreak && (phase == TimerPhase.BREAK_PENDING || phase == TimerPhase.BREAK)) CYCLE_LENGTH else cycleDone

    /** 1.0 = süre dolu, 0.0 = bitti. */
    val progress: Float
        get() = if (totalMs <= 0) 0f else (remainingMs.toFloat() / totalMs).coerceIn(0f, 1f)
}

internal fun TimerState.toUiState(now: Long): TimerUiState = when (this) {
    TimerState.Idle -> TimerUiState()
    is TimerState.Focus -> TimerUiState(TimerPhase.FOCUS, remainingMs(now), durationMs)
    TimerState.BreakPending -> TimerUiState(TimerPhase.BREAK_PENDING, 0, TimerDefaults.BREAK_MS)
    is TimerState.Break -> TimerUiState(TimerPhase.BREAK, remainingMs(now), durationMs, isLongBreak = isLong)
}

/** Kalan süreyi "MM:SS" yazar; saniyeyi yukarı yuvarlar (00:00 yalnızca gerçekten bitince). */
fun formatRemaining(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0) + 999) / 1000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
