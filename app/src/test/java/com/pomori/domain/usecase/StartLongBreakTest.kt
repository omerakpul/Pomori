package com.pomori.domain.usecase

import com.pomori.domain.model.ActiveSession
import com.pomori.domain.model.SessionType
import com.pomori.domain.timer.TimerEvent
import com.pomori.domain.timer.TimerState
import com.pomori.domain.timer.TimerStateMachine
import com.pomori.domain.timer.toTimerState
import com.pomori.testutil.FakeActiveSessionStore
import com.pomori.testutil.FakeClock
import com.pomori.testutil.FakeSessionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Uzun molanın seans kaydına ve durum makinesine doğru taşındığını sınar. */
class StartLongBreakTest {

    private val start = 1_000_000L
    private val focusMs = 25 * 60_000L
    private val longMs = 15 * 60_000L

    private val clock = FakeClock(nowMs = start)
    private val repo = FakeSessionRepository()
    private val store = FakeActiveSessionStore()
    private val endSession = EndSession(repo, store, clock)
    private val startSession = StartSession(repo, store, endSession, clock)

    private suspend fun finishFocus() {
        startSession(SessionType.FOCUS, focusMs)
        clock.advanceBy(focusMs)
    }

    @Test
    fun `a long break is stored as long and runs for the long duration`() = runTest {
        finishFocus()

        val state = startSession(SessionType.BREAK, longMs, isLongBreak = true)

        assertEquals(TimerState.Break(endAt = clock.now() + longMs, durationMs = longMs, isLong = true), state)
        assertTrue(store.current()!!.isLongBreak)
    }

    @Test
    fun `a normal break is not marked long`() = runTest {
        finishFocus()

        val state = startSession(SessionType.BREAK, 5 * 60_000L)

        assertFalse((state as TimerState.Break).isLong)
        assertFalse(store.current()!!.isLongBreak)
    }

    @Test
    fun `the long flag is ignored for focus sessions`() = runTest {
        startSession(SessionType.FOCUS, focusMs, isLongBreak = true)

        assertFalse(store.current()!!.isLongBreak)
    }

    @Test
    fun `a stored long break is restored as long after the process dies`() {
        val stored = ActiveSession(
            sessionId = 1,
            type = SessionType.BREAK,
            endAt = start + longMs,
            plannedDurationMs = longMs,
            isLongBreak = true,
        )

        val state = stored.toTimerState(now = start + 1)

        assertEquals(TimerState.Break(endAt = start + longMs, durationMs = longMs, isLong = true), state)
    }

    @Test
    fun `records without the flag are restored as normal breaks`() {
        val stored = ActiveSession(sessionId = 1, type = SessionType.BREAK, endAt = start + 1_000, plannedDurationMs = 1_000)

        assertFalse((stored.toTimerState(now = start) as TimerState.Break).isLong)
    }

    @Test
    fun `the state machine carries the long flag into the break`() {
        val next = TimerStateMachine.reduce(TimerState.BreakPending, TimerEvent.StartBreak(longMs, isLong = true), start)

        assertEquals(TimerState.Break(endAt = start + longMs, durationMs = longMs, isLong = true), next)
    }

    @Test
    fun `the preset id is saved with the session`() = runTest {
        startSession(SessionType.FOCUS, focusMs, presetId = 7)

        assertEquals(7L, repo.rows.values.single().presetId)
    }

    @Test
    fun `the preset id is optional`() = runTest {
        startSession(SessionType.FOCUS, focusMs)

        assertEquals(null, repo.rows.values.single().presetId)
    }
}
