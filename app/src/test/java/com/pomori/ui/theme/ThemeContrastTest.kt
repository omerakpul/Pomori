package com.pomori.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Yedek (dinamik renk olmayan, Android 8-11) şemaların WCAG kontrastını korur. Android 12+'da
 * duvar kağıdından gelen dinamik şema kullanılır; onu cihazda ThemeContrastInstrumentedTest sınar.
 */
class ThemeContrastTest {

    private fun assertAll(scheme: ColorScheme, pairs: (ColorScheme) -> List<ColorPair>, minimum: Double) {
        val failures = contrastFailures(pairs(scheme), minimum)
        assertTrue("Yetersiz kontrast:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun `light fallback scheme meets 4_5 to 1 for text`() = assertAll(LightColorScheme, ::textColorPairs, 4.5)

    @Test
    fun `dark fallback scheme meets 4_5 to 1 for text`() = assertAll(DarkColorScheme, ::textColorPairs, 4.5)

    @Test
    fun `light fallback scheme meets 3 to 1 for interface components`() =
        assertAll(LightColorScheme, ::componentColorPairs, 3.0)

    @Test
    fun `dark fallback scheme meets 3 to 1 for interface components`() =
        assertAll(DarkColorScheme, ::componentColorPairs, 3.0)

    // --- hesaplamanın kendisi ---

    @Test
    fun `black on white is 21 to 1`() {
        assertEquals(21.0, contrastRatio(Color.Black, Color.White), 0.001)
    }

    @Test
    fun `identical colors are 1 to 1`() {
        assertEquals(1.0, contrastRatio(Color(0xFF6650A4), Color(0xFF6650A4)), 0.0001)
    }

    @Test
    fun `the ratio does not depend on the order`() {
        val a = Color(0xFF6650A4)
        val b = Color(0xFFFFFBFE)
        assertEquals(contrastRatio(a, b), contrastRatio(b, a), 0.0001)
    }

    @Test
    fun `known reference values`() {
        // WCAG'ın bilinen örneği: #777777 beyaz üzerinde yaklaşık 4,48:1.
        assertEquals(4.48, contrastRatio(Color(0xFF777777), Color.White), 0.01)
        // #767676 beyaz üzerinde AA'yı (4,5:1) tam geçen en açık gridir.
        assertTrue(contrastRatio(Color(0xFF767676), Color.White) >= 4.5)
    }
}
