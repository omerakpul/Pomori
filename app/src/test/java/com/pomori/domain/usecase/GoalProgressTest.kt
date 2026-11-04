package com.pomori.domain.usecase

import com.pomori.domain.model.DayFocus
import com.pomori.domain.model.FocusSession
import com.pomori.domain.model.SessionStatus
import com.pomori.domain.model.SessionType
import com.pomori.testutil.FakeClock
import com.pomori.testutil.FakeSessionRepository
import com.pomori.testutil.FakeSettingsStore
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalProgressTest {

    private val goalMs = 60 * 60_000L

    private fun days(vararg minutes: Long): List<DayFocus> {
        val last = LocalDate.of(2026, 11, 4)
        return minutes.mapIndexed { i, m -> DayFocus(last.minusDays((minutes.size - 1 - i).toLong()), m * 60_000L) }
    }

    @Test
    fun `streak counts consecutive goal days including today`() {
        assertEquals(3, currentStreak(days(0, 30, 60, 90, 60), goalMs))
    }

    @Test
    fun `an unfinished today does not break the streak`() {
        assertEquals(2, currentStreak(days(10, 60, 70, 20), goalMs))
    }

    @Test
    fun `a missed yesterday ends the streak`() {
        assertEquals(0, currentStreak(days(60, 60, 10, 20), goalMs))
        assertEquals(1, currentStreak(days(60, 10, 60), goalMs))
    }

    @Test
    fun `goal off means no streak`() {
        assertEquals(0, currentStreak(days(60, 60), 0))
    }

    @Test
    fun `progress reports todays focus, goal and streak`() = runTest {
        val now = LocalDate.of(2026, 11, 4).atTime(15, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val repo = FakeSessionRepository()
        var nextId = 1L
        fun add(daysAgo: Long, minutes: Long) {
            val id = nextId++
            repo.rows[id] = FocusSession(
                id = id,
                type = SessionType.FOCUS,
                startedAt = now - daysAgo * 86_400_000L,
                plannedDurationMs = minutes * 60_000L,
                status = SessionStatus.COMPLETED,
            )
        }
        add(1, 60)
        add(2, 45)
        add(2, 15)
        add(0, 30)

        val progress = GetGoalProgress(repo, FakeSettingsStore(dailyGoalMinutes = 60), FakeClock(now))(ZoneOffset.UTC).first()

        assertTrue(progress.enabled)
        assertFalse(progress.reached)
        assertEquals(30 * 60_000L, progress.todayMs)
        assertEquals(0.5f, progress.fraction, 0.001f)
        assertEquals(2, progress.streak)
    }
}
