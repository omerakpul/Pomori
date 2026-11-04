package com.pomori.domain.usecase

import com.pomori.domain.Clock
import com.pomori.domain.model.WeeklyStats
import com.pomori.domain.repository.NotificationRepository
import com.pomori.domain.repository.SessionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Son 7 günün istatistiği: tamamlanan odak seansları ve en çok bildirim yakalanan uygulamalar.
 * Pencere, akış toplanmaya başladığı andan geriye doğru hesaplanır; ekran yeniden açıldığında
 * güncellenir.
 */
class GetWeeklyStats @Inject constructor(
    private val sessions: SessionRepository,
    private val notifications: NotificationRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<WeeklyStats> {
        val from = clock.now() - WINDOW_MS
        return combine(
            sessions.observeCompletedFocusSince(from),
            notifications.observeTopDistractors(from, TOP_DISTRACTORS),
        ) { completed, distractors ->
            WeeklyStats(
                // Tamamlanan seansta süre dolmuştur: planlanan süre gerçekleşen süredir.
                totalFocusMs = completed.sumOf { it.plannedDurationMs },
                completedSessions = completed.size,
                topDistractors = distractors,
            )
        }
    }

    companion object {
        const val DAYS = 7
        const val TOP_DISTRACTORS = 5
        private const val WINDOW_MS = DAYS * 24 * 60 * 60 * 1000L
    }
}
