package com.pomori.domain.usecase

import com.pomori.domain.Clock
import com.pomori.domain.model.GoalProgress
import com.pomori.domain.repository.SessionRepository
import com.pomori.domain.repository.SettingsStore
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Bugünkü odak süresi, günlük hedef ve seri. Seri için son [HISTORY_DAYS] gün taranır. */
class GetGoalProgress @Inject constructor(
    private val sessions: SessionRepository,
    private val settings: SettingsStore,
    private val clock: Clock,
) {
    operator fun invoke(zone: ZoneId = ZoneId.systemDefault()): Flow<GoalProgress> {
        val now = clock.now()
        val from = now - HISTORY_DAYS * MS_PER_DAY
        return combine(sessions.observeCompletedFocusSince(from), settings.dailyGoalMinutes) { completed, goal ->
            val days = dailyFocus(completed, now, zone, HISTORY_DAYS)
            GoalProgress(
                goalMinutes = goal,
                todayMs = days.last().focusMs,
                streak = currentStreak(days, goal * 60_000L),
            )
        }
    }

    companion object {
        const val HISTORY_DAYS = 365
        private const val MS_PER_DAY = 24 * 60 * 60 * 1000L
    }
}
