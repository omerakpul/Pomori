package com.pomori.domain.usecase

import com.pomori.domain.model.DayFocus
import com.pomori.domain.model.FocusSession
import java.time.Instant
import java.time.ZoneId

/**
 * Tamamlanan odak seanslarını, `now`'ın bulunduğu gün dahil son [days] takvim gününe dağıtır.
 *
 * - Sonuç eskiden yeniye sıralıdır ve her zaman tam [days] öğe içerir; seans olmayan gün 0 ms ile yer alır
 *   (grafikte boş bir gün de görünmeli).
 * - Gün sınırları `zone`'a göredir: gece 23:50'de başlayan seans o günün sayılır.
 * - Pencerenin dışında kalan seanslar yok sayılır.
 *
 * Saf bir fonksiyondur: saat dilimi parametre olduğu için testler cihazın saat diliminden bağımsız çalışır.
 */
fun dailyFocus(sessions: List<FocusSession>, now: Long, zone: ZoneId, days: Int = 7): List<DayFocus> {
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val byDate = sessions
        .groupBy { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() }
        .mapValues { (_, list) -> list.sumOf { it.plannedDurationMs } }
    return (days - 1 downTo 0).map { back ->
        val date = today.minusDays(back.toLong())
        DayFocus(date = date, focusMs = byDate[date] ?: 0L)
    }
}
