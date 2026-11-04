package com.pomori.domain.usecase

import com.pomori.domain.model.DayFocus

/**
 * Bugünden geriye, hedefi tutturulan ardışık gün sayısı.
 *
 * - [days] eskiden yeniye sıralıdır ve son öğe bugündür ([dailyFocus] çıktısı).
 * - Bugün henüz hedefe ulaşmadıysa seri bozulmaz: gün bitmedi, dünden saymaya başlanır.
 *   Bugün ulaşıldıysa bugün de sayılır.
 * - [goalMs] 0 ya da negatifse hedef kapalıdır: seri 0.
 */
fun currentStreak(days: List<DayFocus>, goalMs: Long): Int {
    if (goalMs <= 0 || days.isEmpty()) return 0
    var index = days.lastIndex
    if (days[index].focusMs < goalMs) index--
    var streak = 0
    while (index >= 0 && days[index].focusMs >= goalMs) {
        streak++
        index--
    }
    return streak
}
