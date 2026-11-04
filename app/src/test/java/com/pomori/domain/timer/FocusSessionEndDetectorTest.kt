package com.pomori.domain.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FocusSessionEndDetectorTest {

    private val detector = FocusSessionEndDetector()

    @Test
    fun `nothing ends before any session has run`() {
        assertNull(detector.onFocusSessionChanged(null))
    }

    @Test
    fun `a session that starts does not end anything`() {
        assertNull(detector.onFocusSessionChanged(7))
    }

    @Test
    fun `the same session reported again does not end`() {
        detector.onFocusSessionChanged(7)

        assertNull(detector.onFocusSessionChanged(7))
    }

    @Test
    fun `when the running session disappears it is reported as ended`() {
        detector.onFocusSessionChanged(7)

        // İptal ya da mola: artık çalışan odak seansı yok.
        assertEquals(7L, detector.onFocusSessionChanged(null))
    }

    @Test
    fun `an ended session is reported only once`() {
        detector.onFocusSessionChanged(7)
        detector.onFocusSessionChanged(null)

        assertNull(detector.onFocusSessionChanged(null))
    }

    @Test
    fun `switching directly to another session ends the previous one`() {
        detector.onFocusSessionChanged(7)

        assertEquals(7L, detector.onFocusSessionChanged(8))
    }

    @Test
    fun `cancel then a new session ends the first and then only the first`() {
        detector.onFocusSessionChanged(7)

        assertEquals(7L, detector.onFocusSessionChanged(null))
        assertNull(detector.onFocusSessionChanged(8))
        assertEquals(8L, detector.onFocusSessionChanged(null))
    }

    @Test
    fun `a fresh detector that first sees a running session reports nothing`() {
        // Uygulama süreci yeniden başlayıp seans zaten sürerken dinleyici bağlandığında.
        assertNull(detector.onFocusSessionChanged(42))
        assertEquals(42L, detector.onFocusSessionChanged(null))
    }
}
