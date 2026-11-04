package com.pomori.domain.timer

import com.pomori.domain.timer.TimerEvent.Cancel
import com.pomori.domain.timer.TimerEvent.Skip
import com.pomori.domain.timer.TimerEvent.StartBreak
import com.pomori.domain.timer.TimerEvent.StartFocus
import com.pomori.domain.timer.TimerEvent.TimeUp
import com.pomori.domain.timer.TimerState.Break
import com.pomori.domain.timer.TimerState.BreakPending
import com.pomori.domain.timer.TimerState.Focus
import com.pomori.domain.timer.TimerState.Idle

/**
 * Saf Kotlin durum makinesi. Zamanı parametre olarak alır, yan etkisi yoktur.
 *
 * Idle --StartFocus--> Focus --TimeUp--> BreakPending --StartBreak--> Break --TimeUp--> Idle
 * Focus --Cancel--> Idle;  Break / BreakPending --Skip--> Idle
 *
 * Geçersiz olaylar durumu değiştirmez (aynı state döner).
 */
object TimerStateMachine {

    fun reduce(state: TimerState, event: TimerEvent, now: Long): TimerState = when (state) {
        Idle -> when (event) {
            is StartFocus -> Focus(endAt = now + event.durationMs, durationMs = event.durationMs)
            else -> state
        }

        is Focus -> when (event) {
            TimeUp -> if (now >= state.endAt) BreakPending else state
            Cancel -> Idle
            else -> state
        }

        BreakPending -> when (event) {
            is StartBreak -> Break(endAt = now + event.durationMs, durationMs = event.durationMs, isLong = event.isLong)
            Skip -> Idle
            else -> state
        }

        is Break -> when (event) {
            TimeUp -> if (now >= state.endAt) Idle else state
            Skip -> Idle
            else -> state
        }
    }
}
