package com.pomori.domain.usecase

import com.pomori.domain.model.SessionType
import com.pomori.testutil.FakeActiveSessionStore
import com.pomori.testutil.FakeClock
import com.pomori.testutil.FakeSessionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordInterruptionTest {

    private val focusMs = 25 * 60_000L
    private val clock = FakeClock(nowMs = 1_000_000L)
    private val repo = FakeSessionRepository()
    private val store = FakeActiveSessionStore()
    private val endSession = EndSession(repo, store, clock)
    private val startSession = StartSession(repo, store, endSession, clock)
    private val record = RecordInterruption(repo, store)

    @Test
    fun `counts interruptions on the running focus session without touching the timer`() = runTest {
        startSession(SessionType.FOCUS, focusMs)
        val endAt = store.current()!!.endAt

        assertTrue(record())
        assertTrue(record())

        assertEquals(2, repo.rows.values.single().interruptions)
        assertEquals(endAt, store.current()!!.endAt)
    }

    @Test
    fun `does nothing when no session is running`() = runTest {
        assertFalse(record())
    }

    @Test
    fun `does not count during a break`() = runTest {
        startSession(SessionType.FOCUS, focusMs)
        clock.advanceBy(focusMs)
        startSession(SessionType.BREAK, 5 * 60_000L)

        assertFalse(record())
        assertEquals(0, repo.rows.values.maxByOrNull { it.id }!!.interruptions)
    }

    @Test
    fun `counts survive the session ending`() = runTest {
        startSession(SessionType.FOCUS, focusMs)
        record()
        clock.advanceBy(focusMs)
        endSession(EndReason.TIME_UP)

        assertFalse(record())
        assertEquals(1, repo.rows.values.single().interruptions)
    }
}
