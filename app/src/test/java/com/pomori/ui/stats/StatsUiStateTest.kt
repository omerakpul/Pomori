package com.pomori.ui.stats

import org.junit.Assert.assertEquals
import org.junit.Test

class StatsUiStateTest {

    private val minute = 60_000L

    private fun parts(minutes: Long): Pair<Int, Int> {
        val state = StatsUiState(isLoading = false, totalFocusMs = minutes * minute)
        return state.focusHours to state.focusMinutes
    }

    @Test
    fun `zero time is zero hours and zero minutes`() {
        assertEquals(0 to 0, parts(0))
    }

    @Test
    fun `under an hour has no hours`() {
        assertEquals(0 to 25, parts(25))
        assertEquals(0 to 59, parts(59))
    }

    @Test
    fun `exactly an hour rolls over`() {
        assertEquals(1 to 0, parts(60))
    }

    @Test
    fun `hours and minutes are split`() {
        assertEquals(2 to 35, parts(155))
    }

    @Test
    fun `long totals keep counting hours`() {
        assertEquals(100 to 0, parts(100 * 60))
        assertEquals(100 to 1, parts(100 * 60 + 1))
    }

    @Test
    fun `seconds below a minute are dropped`() {
        val state = StatsUiState(isLoading = false, totalFocusMs = 25 * minute + 59_999)

        assertEquals(0 to 25, state.focusHours to state.focusMinutes)
    }
}
