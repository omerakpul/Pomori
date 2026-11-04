package com.pomori.domain.usecase

import com.pomori.domain.model.FocusSession
import com.pomori.domain.model.SessionStatus
import com.pomori.domain.model.SessionType
import org.junit.Assert.assertEquals
import org.junit.Test

class ModeBreakdownTest {

    private fun session(presetId: Long?, minutes: Long) = FocusSession(
        type = SessionType.FOCUS,
        startedAt = 0,
        plannedDurationMs = minutes * 60_000L,
        status = SessionStatus.COMPLETED,
        presetId = presetId,
    )

    @Test
    fun `a single mode gives no breakdown`() {
        assertEquals(emptyList<Any>(), modeBreakdown(listOf(session(1, 25), session(1, 25))))
    }

    @Test
    fun `groups by mode, sorts by time and puts unknown modes last`() {
        val result = modeBreakdown(
            listOf(
                session(null, 100),
                session(1, 25),
                session(1, 25),
                session(2, 60),
            ),
        )

        assertEquals(listOf<Long?>(2, 1, null), result.map { it.presetId })
        assertEquals(50 * 60_000L, result[1].focusMs)
        assertEquals(2, result[1].sessions)
    }
}
