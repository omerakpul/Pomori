package com.pomori.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.pomori.R

/**
 * Manrope (OFL lisansı, bkz. docs/licenses.md): geometrik ve yuvarlak, Roboto'ya göre çok daha karakterli.
 * Tek bir değişken font dosyası; her ağırlık aynı dosyadan eksen ayarıyla üretilir (API 26+, minSdk 26).
 */
@OptIn(ExperimentalTextApi::class)
val Manrope = FontFamily(
    listOf(200, 300, 400, 500, 600, 700, 800).map { weight ->
        Font(
            resId = R.font.manrope,
            weight = FontWeight(weight),
            variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
        )
    },
)

private val Base = Typography()

private fun TextStyle.manrope(weight: FontWeight, spacing: TextUnit = this.letterSpacing): TextStyle =
    copy(fontFamily = Manrope, fontWeight = weight, letterSpacing = spacing)

val Typography = Typography(
    displayLarge = Base.displayLarge.manrope(FontWeight.ExtraLight, (-1).sp),
    displayMedium = Base.displayMedium.manrope(FontWeight.ExtraLight, (-0.5).sp),
    displaySmall = Base.displaySmall.manrope(FontWeight.Light),
    headlineLarge = Base.headlineLarge.manrope(FontWeight.ExtraBold, (-0.5).sp),
    headlineMedium = Base.headlineMedium.manrope(FontWeight.ExtraBold, (-0.5).sp),
    headlineSmall = Base.headlineSmall.manrope(FontWeight.Bold, (-0.25).sp),
    titleLarge = Base.titleLarge.manrope(FontWeight.Bold, (-0.25).sp),
    titleMedium = Base.titleMedium.manrope(FontWeight.Bold, 0.sp),
    titleSmall = Base.titleSmall.manrope(FontWeight.Bold, 0.1.sp),
    bodyLarge = Base.bodyLarge.manrope(FontWeight.Medium, 0.sp),
    bodyMedium = Base.bodyMedium.manrope(FontWeight.Medium, 0.sp),
    bodySmall = Base.bodySmall.manrope(FontWeight.Medium, 0.1.sp),
    labelLarge = Base.labelLarge.manrope(FontWeight.Bold, 0.1.sp),
    labelMedium = Base.labelMedium.manrope(FontWeight.Bold, 0.5.sp),
    labelSmall = Base.labelSmall.manrope(FontWeight.Bold, 0.5.sp),
)

/** Sayaç rakamları: çok ince ve büyük; "tnum" ile eşit genişlikte, saniye değişince kaymaz. */
val TimerDigitsStyle = TextStyle(
    fontFamily = Manrope,
    fontSize = 72.sp,
    lineHeight = 80.sp,
    fontWeight = FontWeight.ExtraLight,
    letterSpacing = (-2).sp,
    fontFeatureSettings = "tnum",
)

/** İstatistiklerdeki büyük sayılar. */
val StatNumberStyle = TextStyle(
    fontFamily = Manrope,
    fontSize = 44.sp,
    lineHeight = 52.sp,
    fontWeight = FontWeight.ExtraLight,
    letterSpacing = (-1).sp,
    fontFeatureSettings = "tnum",
)
