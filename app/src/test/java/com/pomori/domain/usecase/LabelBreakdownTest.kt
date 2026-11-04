package com.pomori.domain.usecase

import com.pomori.domain.model.FocusSession
import com.pomori.domain.model.SessionStatus
import com.pomori.domain.model.SessionType
import com.pomori.domain.model.normalizeLabel
import com.pomori.testutil.FakeActiveSessionStore
import com.pomori.testutil.FakeClock
import com.pomori.testutil.FakeSessionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LabelBreakdownTest {

    private fun session(label: String?, minutes: Long) = FocusSession(
        type = SessionType.FOCUS,
        startedAt = 0,
        plannedDurationMs = minutes * 60_000L,
        status = SessionStatus.COMPLETED,
        label = label,
    )

    @Test
    fun `no labels at all gives no breakdown`() {
        assertEquals(emptyList<Any>(), labelBreakdown(listOf(session(null, 25), session(null, 25))))
    }

    @Test
    fun `groups case insensitively, sorts by time and puts unlabelled last`() {
        val result = labelBreakdown(
            listOf(
                session(null, 100),
                session("Ders", 25),
                session("ders", 25),
                session("Kod", 60),
            ),
        )

        assertEquals(listOf("Kod", "Ders", null), result.map { it.label })
        assertEquals(50 * 60_000L, result[1].focusMs)
        assertEquals(2, result[1].sessions)
    }

    @Test
    fun `normalizeLabel trims, drops blanks and caps the length`() {
        assertEquals("Ders", normalizeLabel("  Ders  "))
        assertNull(normalizeLabel("   "))
        assertNull(normalizeLabel(null))
        assertEquals(40, normalizeLabel("a".repeat(100))!!.length)
    }

    @Test
    fun `start session stores the label only on focus sessions`() = runTest {
        val clock = FakeClock(nowMs = 1_000L)
        val repo = FakeSessionRepository()
        val store = FakeActiveSessionStore()
        val end = EndSession(repo, store, clock)
        val start = StartSession(repo, store, end, clock)

        start(SessionType.FOCUS, 25 * 60_000L, label = "  Ders ")
        clock.advanceBy(25 * 60_000L)
        start(SessionType.BREAK, 5 * 60_000L, label = "Ders")

        val rows = repo.rows.values.toList()
        assertEquals("Ders", rows[0].label)
        assertNull(rows[1].label)
    }
}
