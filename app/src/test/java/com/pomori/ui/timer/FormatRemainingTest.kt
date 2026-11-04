package com.pomori.ui.timer

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatRemainingTest {
    @Test
    fun `formats full focus duration`() = assertEquals("25:00", formatRemaining(25 * 60_000L))

    @Test
    fun `rounds partial seconds up so zero shows only at the very end`() {
        assertEquals("00:01", formatRemaining(1))
        assertEquals("00:01", formatRemaining(1_000))
        assertEquals("00:02", formatRemaining(1_001))
        assertEquals("00:00", formatRemaining(0))
    }

    @Test
    fun `negative values show zero`() = assertEquals("00:00", formatRemaining(-5_000))

    @Test
    fun `pads minutes and seconds`() = assertEquals("04:05", formatRemaining(4 * 60_000L + 5_000))
}
