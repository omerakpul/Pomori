package com.pomori.domain.usecase

import com.pomori.domain.Clock
import com.pomori.domain.model.SettingsDefaults
import com.pomori.domain.repository.SessionRepository
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull

/**
 * Sıradaki molanın ne olacağı ve seçili modun Pomodoro döngüsünde nerede olunduğu.
 *
 * @property isLong sıradaki mola uzun mu?
 * @property durationMs sıradaki molanın süresi (modun kısa ya da uzun mola süresi).
 * @property focusDoneInCycle bu döngüde tamamlanan odak seansı sayısı (0 until [cycleLength]).
 * @property cycleLength modun kaç odak seansında bir uzun mola verdiği.
 * @property presetId planın hesaplandığı mod; molanın seansına yazılır.
 */
data class BreakPlan(
    val isLong: Boolean,
    val durationMs: Long,
    val focusDoneInCycle: Int,
    val cycleLength: Int,
    val presetId: Long,
)

/**
 * Seçili modda tamamlanan her N. odak seansından sonra mola uzundur. Sayım, bugün **o modda** tamamlanan odak
 * seanslarından türetilir: ayrı bir sayaç saklanmaz, iptal edilenler sayılmaz, döngü her gün sıfırdan başlar ve
 * "Ders" ile "Kodlama" kendi döngülerini ayrı tutar.
 */
fun isLongBreakDue(completedToday: Int, every: Int = SettingsDefaults.LONG_BREAK_EVERY): Boolean =
    completedToday > 0 && completedToday % every == 0

/** `now`'ın bulunduğu günün başlangıcı (epoch ms), verilen saat dilimine göre. */
fun startOfDay(now: Long, zone: ZoneId): Long =
    Instant.ofEpochMilli(now).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()

class GetBreakPlan @Inject constructor(
    private val sessions: SessionRepository,
    private val selectedPreset: ObserveSelectedPreset,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<BreakPlan> {
        val from = startOfDay(clock.now(), ZoneId.systemDefault())
        return combine(
            sessions.observeCompletedFocusSince(from),
            selectedPreset().filterNotNull(),
        ) { completed, preset ->
            val count = completed.count { it.presetId == preset.id }
            val isLong = isLongBreakDue(count, preset.longBreakEvery)
            BreakPlan(
                isLong = isLong,
                durationMs = (if (isLong) preset.longBreakMinutes else preset.breakMinutes) * MS_PER_MINUTE,
                focusDoneInCycle = count % preset.longBreakEvery,
                cycleLength = preset.longBreakEvery,
                presetId = preset.id,
            )
        }
    }

    private companion object {
        const val MS_PER_MINUTE = 60_000L
    }
}
