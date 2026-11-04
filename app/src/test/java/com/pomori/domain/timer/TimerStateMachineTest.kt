package com.pomori.domain.timer

import com.pomori.domain.model.ActiveSession
import com.pomori.domain.model.SessionType
import com.pomori.domain.timer.TimerEvent.Cancel
import com.pomori.domain.timer.TimerEvent.Skip
import com.pomori.domain.timer.TimerEvent.StartBreak
import com.pomori.domain.timer.TimerEvent.StartFocus
import com.pomori.domain.timer.TimerEvent.TimeUp
import com.pomori.domain.timer.TimerState.Break
import com.pomori.domain.timer.TimerState.BreakPending
import com.pomori.domain.timer.TimerState.Focus
import com.pomori.domain.timer.TimerState.Idle
import org.junit.Assert.assertEquals
import org.junit.Test

class TimerStateMachineTest {

    private val start = 1_000_000L
    private val focusMs = 25 * 60_000L
    private val breakMs = 5 * 60_000L

    private fun reduce(state: TimerState, event: TimerEvent, now: Long = start) =
        TimerStateMachine.reduce(state, event, now)

    // --- Idle ---

    @Test
    fun `idle starts focus and computes end time from now`() {
        val next = reduce(Idle, StartFocus(focusMs))
        assertEquals(Focus(endAt = start + focusMs, durationMs = focusMs), next)
    }

    @Test
    fun `idle ignores every event except StartFocus`() {
        listOf(TimeUp, Cancel, Skip, StartBreak(breakMs)).forEach {
            assertEquals("event=$it", Idle, reduce(Idle, it))
        }
    }

    // --- Focus ---

    private val focus = Focus(endAt = start + focusMs, durationMs = focusMs)

    @Test
    fun `focus ignores TimeUp before end time`() {
        assertEquals(focus, reduce(focus, TimeUp, now = focus.endAt - 1))
    }

    @Test
    fun `focus moves to BreakPending exactly at end time`() {
        assertEquals(BreakPending, reduce(focus, TimeUp, now = focus.endAt))
    }

    @Test
    fun `focus moves to BreakPending after end time`() {
        assertEquals(BreakPending, reduce(focus, TimeUp, now = focus.endAt + 60_000))
    }

    @Test
    fun `focus cancel returns to idle`() {
        assertEquals(Idle, reduce(focus, Cancel))
    }

    @Test
    fun `focus ignores StartFocus StartBreak and Skip`() {
        listOf(StartFocus(focusMs), StartBreak(breakMs), Skip).forEach {
            assertEquals("event=$it", focus, reduce(focus, it))
        }
    }

    // --- BreakPending ---

    @Test
    fun `break pending starts break with its own duration`() {
        val next = reduce(BreakPending, StartBreak(breakMs))
        assertEquals(Break(endAt = start + breakMs, durationMs = breakMs), next)
    }

    @Test
    fun `break pending skip returns to idle`() {
        assertEquals(Idle, reduce(BreakPending, Skip))
    }

    @Test
    fun `break pending ignores StartFocus Cancel and TimeUp`() {
        listOf(StartFocus(focusMs), Cancel, TimeUp).forEach {
            assertEquals("event=$it", BreakPending, reduce(BreakPending, it))
        }
    }

    // --- Break ---

    private val breakState = Break(endAt = start + breakMs, durationMs = breakMs)

    @Test
    fun `break ignores TimeUp before end time`() {
        assertEquals(breakState, reduce(breakState, TimeUp, now = breakState.endAt - 1))
    }

    @Test
    fun `break returns to idle at end time`() {
        assertEquals(Idle, reduce(breakState, TimeUp, now = breakState.endAt))
    }

    @Test
    fun `break skip returns to idle`() {
        assertEquals(Idle, reduce(breakState, Skip))
    }

    @Test
    fun `break ignores StartFocus StartBreak and Cancel`() {
        listOf(StartFocus(focusMs), StartBreak(breakMs), Cancel).forEach {
            assertEquals("event=$it", breakState, reduce(breakState, it))
        }
    }

    // --- Tam döngü ---

    @Test
    fun `full cycle idle focus break pending break idle`() {
        var now = start
        var state: TimerState = Idle

        state = TimerStateMachine.reduce(state, StartFocus(focusMs), now)
        assertEquals(focus, state)

        now += focusMs
        state = TimerStateMachine.reduce(state, TimeUp, now)
        assertEquals(BreakPending, state)

        state = TimerStateMachine.reduce(state, StartBreak(breakMs), now)
        assertEquals(Break(endAt = now + breakMs, durationMs = breakMs), state)

        now += breakMs
        state = TimerStateMachine.reduce(state, TimeUp, now)
        assertEquals(Idle, state)
    }

    // --- remainingMs ---

    @Test
    fun `remaining time is end minus now and never negative`() {
        assertEquals(10_000L, focus.remainingMs(focus.endAt - 10_000))
        assertEquals(0L, focus.remainingMs(focus.endAt + 5_000))
        assertEquals(0L, Idle.remainingMs(start))
        assertEquals(0L, BreakPending.remainingMs(start))
    }

    // --- Süreç ölümü sonrası geri yükleme ---

    private fun active(type: SessionType, endAt: Long, planned: Long) =
        ActiveSession(sessionId = 7, type = type, endAt = endAt, plannedDurationMs = planned)

    @Test
    fun `no stored session restores to idle`() {
        assertEquals(Idle, (null as ActiveSession?).toTimerState(start))
    }

    @Test
    fun `running focus restores with correct remaining time after process death`() {
        val stored = active(SessionType.FOCUS, endAt = start + focusMs, planned = focusMs)
        val reopenedAt = start + 10 * 60_000L // 10 dk sonra uygulama yeniden açıldı
        val restored = stored.toTimerState(reopenedAt)
        assertEquals(Focus(stored.endAt, focusMs), restored)
        assertEquals(15 * 60_000L, restored.remainingMs(reopenedAt))
    }

    @Test
    fun `expired focus restores as break pending`() {
        val stored = active(SessionType.FOCUS, endAt = start + focusMs, planned = focusMs)
        assertEquals(BreakPending, stored.toTimerState(start + focusMs + 1))
    }

    @Test
    fun `running break restores as break`() {
        val stored = active(SessionType.BREAK, endAt = start + breakMs, planned = breakMs)
        assertEquals(Break(stored.endAt, breakMs), stored.toTimerState(start + 1_000))
    }

    @Test
    fun `expired break restores as idle`() {
        val stored = active(SessionType.BREAK, endAt = start + breakMs, planned = breakMs)
        assertEquals(Idle, stored.toTimerState(start + breakMs))
    }
}
