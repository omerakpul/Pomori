package com.pomori.domain.usecase

import app.cash.turbine.test
import com.pomori.domain.model.FocusSession
import com.pomori.domain.model.SessionStatus
import com.pomori.domain.model.SessionType
import com.pomori.testutil.FakeClock
import com.pomori.testutil.FakeSessionRepository
import com.pomori.testutil.FakeSettingsStore
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GetBreakPlanTest {

    private val minute = 60_000L
    private val day = 24 * 60 * minute

    // Öğlen: hangi saat diliminde olursak olalım "bugün" aynı güne düşer.
    private val now = 100 * day + 12 * 60 * minute
    private val clock = FakeClock(nowMs = now)
    private val sessions = FakeSessionRepository()
    private val settings = FakeSettingsStore(breakMinutes = 5, longBreakMinutes = 15)
    private val getBreakPlan = GetBreakPlan(sessions, settings, clock)

    private suspend fun session(
        startedAt: Long = now,
        type: SessionType = SessionType.FOCUS,
        status: SessionStatus = SessionStatus.COMPLETED,
    ) = sessions.insert(
        FocusSession(type = type, startedAt = startedAt, plannedDurationMs = 25 * minute, status = status),
    )

    private suspend fun completedToday(count: Int) = repeat(count) { session() }

    @Test
    fun `without any session the break is short and the cycle is empty`() = runTest {
        getBreakPlan().test {
            val plan = awaitItem()
            assertFalse(plan.isLong)
            assertEquals(5 * minute, plan.durationMs)
            assertEquals(0, plan.focusDoneInCycle)
        }
    }

    @Test
    fun `the break stays short until the fourth completed session`() = runTest {
        completedToday(3)

        getBreakPlan().test {
            val plan = awaitItem()
            assertFalse(plan.isLong)
            assertEquals(3, plan.focusDoneInCycle)
        }
    }

    @Test
    fun `the fourth completed session earns a long break with the long duration`() = runTest {
        completedToday(4)

        getBreakPlan().test {
            val plan = awaitItem()
            assertTrue(plan.isLong)
            assertEquals(15 * minute, plan.durationMs)
            // Döngü tamamlandı: noktalar yeni döngü için sıfırlanır.
            assertEquals(0, plan.focusDoneInCycle)
        }
    }

    @Test
    fun `the cycle starts over after the long break`() = runTest {
        completedToday(5)

        getBreakPlan().test {
            val plan = awaitItem()
            assertFalse(plan.isLong)
            assertEquals(1, plan.focusDoneInCycle)
        }
    }

    @Test
    fun `the eighth session earns another long break`() = runTest {
        completedToday(8)

        getBreakPlan().test { assertTrue(awaitItem().isLong) }
    }

    @Test
    fun `cancelled sessions and breaks do not count`() = runTest {
        completedToday(3)
        session(status = SessionStatus.CANCELLED)
        session(status = SessionStatus.RUNNING)
        session(type = SessionType.BREAK)

        getBreakPlan().test {
            val plan = awaitItem()
            assertFalse(plan.isLong)
            assertEquals(3, plan.focusDoneInCycle)
        }
    }

    @Test
    fun `sessions from earlier days do not count`() = runTest {
        completedToday(2)
        repeat(5) { session(startedAt = now - 2 * day) }

        getBreakPlan().test { assertEquals(2, awaitItem().focusDoneInCycle) }
    }

    @Test
    fun `the plan follows the settings`() = runTest {
        completedToday(4)

        getBreakPlan().test {
            assertEquals(15 * minute, awaitItem().durationMs)

            settings.setLongBreakMinutes(30)
            assertEquals(30 * minute, awaitItem().durationMs)
        }
    }

    // --- saf yardımcılar ---

    @Test
    fun `a long break is due only on multiples of four above zero`() {
        assertFalse(isLongBreakDue(0))
        assertFalse(isLongBreakDue(1))
        assertFalse(isLongBreakDue(3))
        assertTrue(isLongBreakDue(4))
        assertFalse(isLongBreakDue(5))
        assertTrue(isLongBreakDue(8))
    }

    @Test
    fun `the cycle length can be changed`() {
        assertTrue(isLongBreakDue(3, every = 3))
        assertFalse(isLongBreakDue(4, every = 3))
    }

    @Test
    fun `start of day follows the given zone`() {
        val noonUtc = 100 * day + 12 * 60 * minute
        assertEquals(100 * day, startOfDay(noonUtc, ZoneOffset.UTC))
        // UTC+3: aynı an yerel olarak 15:00, gün başlangıcı UTC'de bir önceki 21:00.
        assertEquals(100 * day - 3 * 60 * minute, startOfDay(noonUtc, ZoneOffset.ofHours(3)))
    }
}
