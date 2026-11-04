package com.pomori.domain.usecase

import com.pomori.domain.Clock
import com.pomori.domain.model.SettingsDefaults
import com.pomori.domain.repository.SessionRepository
import com.pomori.domain.repository.SettingsStore
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Sıradaki molanın ne olacağı ve Pomodoro döngüsünde nerede olunduğu.
 *
 * @property isLong sıradaki mola uzun mu?
 * @property durationMs sıradaki molanın süresi (kısa ya da uzun, ayardan).
 * @property focusDoneInCycle bu döngüde tamamlanan odak seansı sayısı (0 until [SettingsDefaults.LONG_BREAK_EVERY]).
 */
data class BreakPlan(
    val isLong: Boolean,
    val durationMs: Long,
    val focusDoneInCycle: Int,
)

/**
 * Tamamlanan her 4. odak seansından sonra mola uzundur. Sayım, bugün tamamlanan odak seanslarından türetilir:
 * ayrı bir sayaç saklanmaz, böylece iptal edilen seanslar sayıma girmez ve döngü her gün sıfırdan başlar.
 */
fun isLongBreakDue(completedToday: Int, every: Int = SettingsDefaults.LONG_BREAK_EVERY): Boolean =
    completedToday > 0 && completedToday % every == 0

/** `now`'ın bulunduğu günün başlangıcı (epoch ms), verilen saat dilimine göre. */
fun startOfDay(now: Long, zone: ZoneId): Long =
    Instant.ofEpochMilli(now).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()

class GetBreakPlan @Inject constructor(
    private val sessions: SessionRepository,
    private val settings: SettingsStore,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<BreakPlan> {
        val from = startOfDay(clock.now(), ZoneId.systemDefault())
        return combine(
            sessions.observeCompletedFocusSince(from),
            settings.breakMinutes,
            settings.longBreakMinutes,
        ) { completed, shortMinutes, longMinutes ->
            val count = completed.size
            val isLong = isLongBreakDue(count)
            BreakPlan(
                isLong = isLong,
                durationMs = (if (isLong) longMinutes else shortMinutes) * MS_PER_MINUTE,
                focusDoneInCycle = count % SettingsDefaults.LONG_BREAK_EVERY,
            )
        }
    }

    private companion object {
        const val MS_PER_MINUTE = 60_000L
    }
}
