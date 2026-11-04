package com.pomori.domain.usecase

import app.cash.turbine.test
import com.pomori.data.repository.PresetRepositoryImpl
import com.pomori.data.repository.toEntity
import com.pomori.domain.model.FocusSession
import com.pomori.domain.model.Preset
import com.pomori.domain.model.SessionStatus
import com.pomori.domain.model.SessionType
import com.pomori.testutil.FakeClock
import com.pomori.testutil.FakePresetDao
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
    private val settings = FakeSettingsStore()
    private val presetDao = FakePresetDao(listOf(Preset(1, "Varsayılan", 25, 5, 15, 4).toEntity()))
    private val presets = PresetRepositoryImpl(presetDao)
    private val getBreakPlan = GetBreakPlan(sessions, ObserveSelectedPreset(presets, settings), clock)

    private suspend fun session(
        startedAt: Long = now,
        type: SessionType = SessionType.FOCUS,
        status: SessionStatus = SessionStatus.COMPLETED,
        presetId: Long? = 1L,
    ) = sessions.insert(
        FocusSession(
            type = type,
            startedAt = startedAt,
            plannedDurationMs = 25 * minute,
            status = status,
            presetId = presetId,
        ),
    )

    private suspend fun completedToday(count: Int, presetId: Long? = 1L) = repeat(count) { session(presetId = presetId) }

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

            presets.update(Preset(1, "Varsayılan", 25, 5, 30, 4))
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

    // --- modlar ---

    @Test
    fun `sessions of other presets do not count`() = runTest {
        completedToday(3, presetId = 1L)
        completedToday(5, presetId = 2L)

        getBreakPlan().test {
            val plan = awaitItem()
            assertFalse(plan.isLong)
            assertEquals(3, plan.focusDoneInCycle)
        }
    }

    @Test
    fun `sessions without a preset do not count`() = runTest {
        completedToday(4, presetId = null)

        getBreakPlan().test {
            val plan = awaitItem()
            assertFalse(plan.isLong)
            assertEquals(0, plan.focusDoneInCycle)
        }
    }

    @Test
    fun `the cycle length comes from the preset`() = runTest {
        presetDao.update(Preset(1, "Varsayılan", 25, 5, 15, 2).toEntity())
        completedToday(2)

        getBreakPlan().test {
            val plan = awaitItem()
            assertTrue(plan.isLong)
            assertEquals(2, plan.cycleLength)
            assertEquals(0, plan.focusDoneInCycle)
        }
    }

    @Test
    fun `durations and the plan follow the selected preset`() = runTest {
        presetDao.insert(Preset(0, "Ders", 40, 3, 20, 3).toEntity())
        settings.setSelectedPresetId(2)
        completedToday(3, presetId = 2L)

        getBreakPlan().test {
            val plan = awaitItem()
            assertTrue(plan.isLong)
            assertEquals(20 * minute, plan.durationMs)
            assertEquals(2L, plan.presetId)
            assertEquals(3, plan.cycleLength)
        }
    }
}
