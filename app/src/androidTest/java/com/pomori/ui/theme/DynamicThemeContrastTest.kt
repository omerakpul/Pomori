package com.pomori.ui.theme

import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android 12+'da kullanılan, duvar kağıdından türeyen dinamik şemaların kontrastı. Şema cihaza ve
 * duvar kağıdına bağlıdır, bu yüzden yalnızca gerçek bir cihazda sınanabilir.
 */
@RunWith(AndroidJUnit4::class)
class DynamicThemeContrastTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun assumeDynamicColor() = assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)

    @Test
    fun dynamicLightScheme_meetsTheTextContrast() {
        assumeDynamicColor()

        val failures = contrastFailures(textColorPairs(dynamicLightColorScheme(context)), 4.5)

        assertTrue("Yetersiz kontrast:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun dynamicDarkScheme_meetsTheTextContrast() {
        assumeDynamicColor()

        val failures = contrastFailures(textColorPairs(dynamicDarkColorScheme(context)), 4.5)

        assertTrue("Yetersiz kontrast:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun dynamicLightScheme_meetsTheComponentContrast() {
        assumeDynamicColor()

        val failures = contrastFailures(componentColorPairs(dynamicLightColorScheme(context)), 3.0)

        assertTrue("Yetersiz kontrast:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun dynamicDarkScheme_meetsTheComponentContrast() {
        assumeDynamicColor()

        val failures = contrastFailures(componentColorPairs(dynamicDarkColorScheme(context)), 3.0)

        assertTrue("Yetersiz kontrast:\n" + failures.joinToString("\n"), failures.isEmpty())
    }
}
