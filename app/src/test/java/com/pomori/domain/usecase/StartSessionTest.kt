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

class StartSessionTest {

    private val focusMs = 25 * 60_000L
    private val breakMs = 5 * 60_000L

    private val clock = FakeClock(nowMs = 1_000_000L)
    private val repo = FakeSessionRepository()
    private val store = FakeActiveSessionStore()
    private val endSession = EndSession(repo, store, clock)
    private val startSession = StartSession(repo, store, endSession, clock)

    @Test
    fun `starting focus saves a running row and the end time`() = runTest {
        val state = startSession(SessionType.FOCUS, focusMs)

        assertEquals(TimerState.Focus(endAt = 1_000_000L + focusMs, durationMs = focusMs), state)
        val row = repo.rows.values.single()
        assertEquals(SessionType.FOCUS, row.type)
        assertEquals(SessionStatus.RUNNING, row.status)
        assertEquals(1_000_000L, row.startedAt)
        assertEquals(focusMs, row.plannedDurationMs)
        val active = store.current()!!
        assertEquals(row.id, active.sessionId)
        assertEquals(1_000_000L + focusMs, active.endAt)
    }

    @Test
    fun `starting focus while one is running changes nothing`() = runTest {
        startSession(SessionType.FOCUS, focusMs)
        clock.advanceBy(60_000)

        val state = startSession(SessionType.FOCUS, focusMs)

        assertEquals(1, repo.rows.size)
        assertEquals(TimerState.Focus(endAt = 1_000_000L + focusMs, durationMs = focusMs), state)
    }

    @Test
    fun `starting a break without a finished focus is ignored`() = runTest {
        val state = startSession(SessionType.BREAK, breakMs)

        assertEquals(TimerState.Idle, state)
        assertEquals(0, repo.rows.size)
        assertNull(store.current())
    }

    @Test
    fun `break can start after focus time is up and focus row is completed`() = runTest {
        startSession(SessionType.FOCUS, focusMs)
        clock.advanceBy(focusMs)

        val state = startSession(SessionType.BREAK, breakMs)

        assertEquals(TimerState.Break(endAt = clock.now() + breakMs, durationMs = breakMs), state)
        val (focusRow, breakRow) = repo.rows.values.toList()
        assertEquals(SessionStatus.COMPLETED, focusRow.status)
        assertEquals(1_000_000L + focusMs, focusRow.endedAt)
        assertEquals(SessionType.BREAK, breakRow.type)
        assertEquals(SessionStatus.RUNNING, breakRow.status)
    }

    @Test
    fun `new focus after an expired break closes the old break row first`() = runTest {
        startSession(SessionType.FOCUS, focusMs)
        clock.advanceBy(focusMs)
        startSession(SessionType.BREAK, breakMs)
        clock.advanceBy(breakMs + 10_000) // mola bitti ama kimse kapatmadı

        val state = startSession(SessionType.FOCUS, focusMs)

        assertEquals(TimerState.Focus(endAt = clock.now() + focusMs, durationMs = focusMs), state)
        val rows = repo.rows.values.toList()
        assertEquals(3, rows.size)
        assertEquals(SessionStatus.COMPLETED, rows[1].status)
        assertEquals(SessionStatus.RUNNING, rows[2].status)
        assertNotNull(store.current())
    }
}
