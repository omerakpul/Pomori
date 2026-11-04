package com.pomori.domain.usecase

import com.pomori.domain.model.FocusSession
import com.pomori.domain.model.SessionStatus
import com.pomori.domain.model.SessionType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyFocusTest {

    private val minute = 60_000L
    private val utc: ZoneId = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 11, 4) // çarşamba

    private fun at(date: LocalDate, hour: Int, min: Int = 0, zone: ZoneId = utc): Long =
        LocalDateTime.of(date.year, date.month, date.dayOfMonth, hour, min).atZone(zone).toInstant().toEpochMilli()

    private fun session(startedAt: Long, minutes: Long = 25) = FocusSession(
        type = SessionType.FOCUS,
        startedAt = startedAt,
        plannedDurationMs = minutes * minute,
        status = SessionStatus.COMPLETED,
    )

    private val now = at(today, 18)

    @Test
    fun `there are always seven days ordered from oldest to newest`() {
        val result = dailyFocus(emptyList(), now, utc)

        assertEquals(7, result.size)
        assertEquals(today.minusDays(6), result.first().date)
        assertEquals(today, result.last().date)
        assertEquals((0L..6L).map { today.minusDays(6 - it) }, result.map { it.date })
    }

    @Test
    fun `days without sessions are present with zero focus`() {
        val result = dailyFocus(emptyList(), now, utc)

        assertEquals(List(7) { 0L }, result.map { it.focusMs })
    }

    @Test
    fun `sessions of the same day are added up`() {
        val result = dailyFocus(
            listOf(session(at(today, 9)), session(at(today, 14), minutes = 50)),
            now,
            utc,
        )

        assertEquals(75 * minute, result.last().focusMs)
    }

    @Test
    fun `each session lands on its own day`() {
        val result = dailyFocus(
            listOf(session(at(today.minusDays(1), 10)), session(at(today.minusDays(3), 10), minutes = 50)),
            now,
            utc,
        )

        assertEquals(25 * minute, result[5].focusMs)
        assertEquals(50 * minute, result[3].focusMs)
        assertEquals(0L, result.last().focusMs)
    }

    @Test
    fun `a session that starts late in the evening belongs to that day`() {
        val result = dailyFocus(listOf(session(at(today.minusDays(1), 23, 50))), now, utc)

        assertEquals(25 * minute, result[5].focusMs)
        assertEquals(0L, result.last().focusMs)
    }

    @Test
    fun `day boundaries follow the zone`() {
        val istanbul = ZoneId.of("Europe/Istanbul") // UTC+3
        // Dün 22:30 UTC = bugün 01:30 İstanbul
        val startedAt = at(today.minusDays(1), 22, 30)

        val inUtc = dailyFocus(listOf(session(startedAt)), now, utc)
        val inIstanbul = dailyFocus(listOf(session(startedAt)), at(today, 18, zone = istanbul), istanbul)

        assertEquals(25 * minute, inUtc[5].focusMs) // UTC'de dün
        assertEquals(25 * minute, inIstanbul[6].focusMs) // İstanbul'da bugün
    }

    @Test
    fun `sessions outside the seven days are ignored`() {
        val result = dailyFocus(
            listOf(session(at(today.minusDays(7), 12)), session(at(today.minusDays(30), 12)), session(at(today.plusDays(1), 12))),
            now,
            utc,
        )

        assertEquals(0L, result.sumOf { it.focusMs })
    }

    @Test
    fun `the oldest day of the window is included`() {
        val result = dailyFocus(listOf(session(at(today.minusDays(6), 0, 5))), now, utc)

        assertEquals(25 * minute, result.first().focusMs)
    }
}
