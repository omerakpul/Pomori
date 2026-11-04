package com.pomori.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.pow

/**
 * WCAG 2.x kontrast oranı (1:1 ile 21:1 arası). Normal metin için en az 4,5:1, büyük metin ve
 * arayüz bileşenlerinin sınırları için en az 3:1 gerekir. Renk çiftlerinin bu eşikleri
 * karşıladığını koruyan testler tarafından kullanılır.
 */
internal fun contrastRatio(first: Color, second: Color): Double {
    val a = relativeLuminance(first)
    val b = relativeLuminance(second)
    val lighter = maxOf(a, b)
    val darker = minOf(a, b)
    return (lighter + 0.05) / (darker + 0.05)
}

private fun relativeLuminance(color: Color): Double {
    val argb = color.toArgb()
    val r = linear(((argb shr 16) and 0xFF) / 255.0)
    val g = linear(((argb shr 8) and 0xFF) / 255.0)
    val b = linear((argb and 0xFF) / 255.0)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

private fun linear(channel: Double): Double =
    if (channel <= 0.03928) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)

/** Uygulamada gerçekten birlikte kullanılan bir ön plan/arka plan renk çifti. */
internal data class ColorPair(val name: String, val foreground: Color, val background: Color)

/** Normal metin çiftleri: en az 4,5:1 olmalı. */
internal fun textColorPairs(s: ColorScheme) = listOf(
    ColorPair("onBackground / background", s.onBackground, s.background),
    ColorPair("onSurface / surface", s.onSurface, s.surface),
    ColorPair("onSurfaceVariant / background (ipucu metinleri)", s.onSurfaceVariant, s.background),
    ColorPair("onSurface / card (surfaceContainerHighest)", s.onSurface, s.surfaceContainerHighest),
    ColorPair("onSurfaceVariant / card", s.onSurfaceVariant, s.surfaceContainerHighest),
    ColorPair("onSurface / dialog (surfaceContainerHigh)", s.onSurface, s.surfaceContainerHigh),
    ColorPair("onSurfaceVariant / dialog", s.onSurfaceVariant, s.surfaceContainerHigh),
    ColorPair("primary / background (bölüm başlıkları, metin düğmeleri)", s.primary, s.background),
    ColorPair("primary / card (Kaldır düğmesi)", s.primary, s.surfaceContainerHighest),
    ColorPair("primary / dialog (Kapat düğmesi)", s.primary, s.surfaceContainerHigh),
    ColorPair("onPrimary / primary (dolu düğmeler)", s.onPrimary, s.primary),
    ColorPair("onPrimaryContainer / primaryContainer (sayı rozeti)", s.onPrimaryContainer, s.primaryContainer),
    ColorPair("onTertiaryContainer / tertiaryContainer (uyarı kartı)", s.onTertiaryContainer, s.tertiaryContainer),
    // Uyarı kartındaki düğme `primary` değil kartın içerik rengini (onTertiaryContainer) kullanır: bazı
    // dinamik şemalarda primary / tertiaryContainer 1,12:1 kadar düşüyor (bkz. NotificationAccessCard).
)

/** Anlamlı arayüz parçaları (kenarlıklar, ilerleme halkası, çubuklar): en az 3:1 olmalı. */
internal fun componentColorPairs(s: ColorScheme) = listOf(
    ColorPair("outline / background (kenarlıklı düğmeler, metin alanı)", s.outline, s.background),
    ColorPair("outline / card (kenarlıklı düğmeler kartta)", s.outline, s.surfaceContainerHighest),
    ColorPair("primary / background (ilerleme halkası, çubuklar)", s.primary, s.background),
)

/** Eşiğin altında kalan çiftleri, oranlarıyla okunur satırlar olarak döner (boşsa hepsi geçti). */
internal fun contrastFailures(pairs: List<ColorPair>, minimum: Double): List<String> =
    pairs.map { it to contrastRatio(it.foreground, it.background) }
        .filter { (_, ratio) -> ratio < minimum }
        .map { (pair, ratio) -> "${pair.name}: %.2f:1 (en az $minimum:1)".format(ratio) }
