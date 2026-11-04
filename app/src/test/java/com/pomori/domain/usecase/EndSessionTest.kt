package com.pomori.domain.usecase

import com.pomori.domain.model.SessionStatus
import com.pomori.domain.model.SessionType
import com.pomori.domain.timer.TimerState
import com.pomori.testutil.FakeActiveSessionStore
import com.pomori.testutil.FakeClock
import com.pomori.testutil.FakeSessionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class EndSessionTest {

    private val start = 1_000_000L
    private val focusMs = 25 * 60_000L
    private val breakMs = 5 * 60_000L

    private val clock = FakeClock(nowMs = start)
    private val repo = FakeSessionRepository()
    private val store = FakeActiveSessionStore()
    private val endSession = EndSession(repo, store, clock)
    private val startSession = StartSession(repo, store, endSession, clock)

    @Test
    fun `cancel marks focus cancelled and clears the active session`() = runTest {
        startSession(SessionType.FOCUS, focusMs)
        clock.advanceBy(60_000)

        val state = endSession(EndReason.CANCEL)

        assertEquals(TimerState.Idle, state)
        val row = repo.rows.values.single()
        assertEquals(SessionStatus.CANCELLED, row.status)
        assertEquals(start + 60_000, row.endedAt)
        assertNull(store.current())
    }

    @Test
    fun `time up before end time changes nothing`() = runTest {
        startSession(SessionType.FOCUS, focusMs)
        clock.advanceBy(focusMs - 1)

        val state = endSession(EndReason.TIME_UP)

        assertEquals(TimerState.Focus(endAt = start + focusMs, durationMs = focusMs), state)
        assertEquals(SessionStatus.RUNNING, repo.rows.values.single().status)
    }

    @Test
    fun `time up after focus completes it and keeps break pending`() = runTest {
        startSession(SessionType.FOCUS, focusMs)
        clock.advanceBy(focusMs + 5_000)

        val state = endSession(EndReason.TIME_UP)

        assertEquals(TimerState.BreakPending, state)
        val row = repo.rows.values.single()
        assertEquals(SessionStatus.COMPLETED, row.status)
        assertEquals(start + focusMs, row.endedAt) // planlanan bitiş, gecikmeli çağrı zamanı değil
        assertNotNull(store.current()) // mola kararı bekleniyor
    }

    @Test
    fun `time up is idempotent`() = runTest {
        startSession(SessionType.FOCUS, focusMs)
        clock.advanceBy(focusMs)

        endSession(EndReason.TIME_UP)
        clock.advanceBy(30_000)
        val state = endSession(EndReason.TIME_UP)

        assertEquals(TimerState.BreakPending, state)
        val row = repo.rows.values.single()
        assertEquals(SessionStatus.COMPLETED, row.status)
        assertEquals(start + focusMs, row.endedAt)
    }

    @Test
    fun `time up after break completes it and clears the active session`() = runTest {
        startSession(SessionType.FOCUS, focusMs)
        clock.advanceBy(focusMs)
        startSession(SessionType.BREAK, breakMs)
        clock.advanceBy(breakMs)

        val state = endSession(EndReason.TIME_UP)

        assertEquals(TimerState.Idle, state)
        assertEquals(SessionStatus.COMPLETED, repo.rows.values.last().status)
        assertNull(store.current())
    }

    @Test
    fun `skip during break cancels the break row`() = runTest {
        startSession(SessionType.FOCUS, focusMs)
        clock.advanceBy(focusMs)
        startSession(SessionType.BREAK, breakMs)
        clock.advanceBy(60_000)

        val state = endSession(EndReason.SKIP)

        assertEquals(TimerState.Idle, state)
        assertEquals(SessionStatus.CANCELLED, repo.rows.values.last().status)
        assertNull(store.current())
    }

    @Test
    fun `skip while break is pending keeps focus completed and clears the session`() = runTest {
        startSession(SessionType.FOCUS, focusMs)
        clock.advanceBy(focusMs)
        endSession(EndReason.TIME_UP)

        val state = endSession(EndReason.SKIP)

        assertEquals(TimerState.Idle, state)
        assertEquals(SessionStatus.COMPLETED, repo.rows.values.single().status)
        assertNull(store.current())
    }

    @Test
    fun `cancel is ignored when focus already finished`() = runTest {
        startSession(SessionType.FOCUS, focusMs)
        clock.advanceBy(focusMs)
        endSession(EndReason.TIME_UP)

        val state = endSession(EndReason.CANCEL)

        assertEquals(TimerState.BreakPending, state)
        assertEquals(SessionStatus.COMPLETED, repo.rows.values.single().status)
    }

    @Test
    fun `ending with no active session is a no-op`() = runTest {
        EndReason.entries.forEach {
            assertEquals(TimerState.Idle, endSession(it))
        }
        assertEquals(0, repo.rows.size)
    }
}
