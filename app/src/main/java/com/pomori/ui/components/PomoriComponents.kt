package com.pomori.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pomori.R

/**
 * Ekranın arkasında, faz renginde yumuşak bir ışıma. Dolgu ya da kart yerine renk bu şekilde "havada" durur;
 * uygulamanın en belirgin görsel imzası. Koyu zeminde güçlü, açık zeminde çok daha hafif.
 */
@Composable
fun Modifier.ambientGlow(color: Color, centerY: Float = 0.36f): Modifier {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val strength = if (dark) 0.30f else 0.16f
    return drawBehind {
        drawRect(
            Brush.radialGradient(
                colors = listOf(color.copy(alpha = strength), Color.Transparent),
                center = Offset(size.width / 2f, size.height * centerY),
                radius = size.width * 1.0f,
            ),
        )
    }
}

/** Alt ekranların üstü: geri oku ve büyük başlık. */
@Composable
fun ScreenHeader(
    title: String,
    onBack: () -> Unit,
    backTag: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val configuration = LocalConfiguration.current
    val landscape = configuration.screenWidthDp > configuration.screenHeightDp
    val backButton: @Composable () -> Unit = {
        IconButton(onClick = onBack, modifier = Modifier.testTag(backTag)) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.action_back),
            )
        }
    }
    val titles: @Composable (Modifier) -> Unit = { titleModifier ->
        Column(modifier = titleModifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = if (landscape) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() },
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    if (landscape) {
        // Yatayda dikey alan kıt: geri düğmesi ve başlık aynı satırda durur, liste daha çok yer bulur.
        Row(
            modifier = modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp, end = 20.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            backButton()
            titles(Modifier.padding(start = 4.dp))
        }
    } else {
        Column(modifier = modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp, end = 20.dp)) {
            backButton()
            titles(Modifier.padding(start = 12.dp, top = 8.dp, bottom = 12.dp))
        }
    }
}

/** Bölüm başlığı: küçük, sönük ve açık harf aralıklı. Renkli başlık "şablon" havası veriyordu. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .padding(start = 4.dp, top = 20.dp, bottom = 6.dp)
            .semantics { heading() },
    )
}

/** Düz panel: dolgu yok denecek kadar hafif, 1 dp ince çizgili. Gölge ve tonlu kart yok. */
@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(content = content)
    }
}

/** Renkli simge: yuvarlak köşeli kare, zemini simge renginin soluk bir tonu. Süs; anlamı metin taşır. */
@Composable
fun IconBadge(
    @DrawableRes icon: Int,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 22.dp,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .background(tint.copy(alpha = 0.14f), RoundedCornerShape(size * 0.32f)),
    ) {
        Icon(painter = painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/**
 * Uygulama ya da kişi için baş harf damgası. Gerçek simge PackageManager'a erişim ister; baş harf ekranı
 * bağımsız ve test edilebilir tutar. Renk isme göre sabit seçilir, liste her açılışta aynı görünür.
 */
@Composable
fun InitialAvatar(name: String, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val scheme = MaterialTheme.colorScheme
    val palette = listOf(scheme.primary, scheme.secondary, scheme.tertiary)
    val tint = palette[Math.floorMod(name.hashCode(), palette.size)]
    val initial = name.trim().firstOrNull()?.uppercase() ?: "?"
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .background(tint.copy(alpha = 0.14f), RoundedCornerShape(size * 0.32f)),
    ) {
        Text(text = initial, style = MaterialTheme.typography.titleMedium, color = tint, textAlign = TextAlign.Center)
    }
}

/** Boş durumlar için ortalanmış simge + başlık + açıklama. */
@Composable
fun EmptyState(
    @DrawableRes icon: Int,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconBadge(
            icon = icon,
            tint = tint,
            size = 88.dp,
            iconSize = 40.dp,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        extra()
    }
}

/** Simge + metin içeren düğme içeriği. */
@Composable
fun IconLabel(@DrawableRes icon: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp))
        Text(text)
    }
}

/** Ana eylem düğmesi: tam genişlik, 18 dp köşe, gölgesiz. Pill biçimi Material'ın en tanıdık işaretiydi. */
@Composable
fun PomoriButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        modifier = modifier.fillMaxWidth().heightIn(min = 58.dp),
    ) { content() }
}

/** Erişilebilirlik için dokunma hedefi en az 48 dp olmalıdır. */
private val STEPPER_TOUCH_TARGET = 48.dp

/**
 * Etiketli, tek parça "segment" kontrol: [ − | değer | + ]. Üç ayrı daire düğmeden çok daha derli toplu.
 * Düğmeler `${tag}_dec` / `${tag}_inc`, değer `${tag}_value` etiketini taşır.
 */
@Composable
fun StepperRow(
    @DrawableRes icon: Int,
    tint: Color,
    label: String,
    valueText: String,
    canDecrement: Boolean,
    canIncrement: Boolean,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
) {
    val decreaseDescription = stringResource(R.string.settings_decrease, label)
    val increaseDescription = stringResource(R.string.settings_increase, label)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBadge(icon = icon, tint = tint)
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        // Tek parça "segment" kontrol: [ - | değer | + ]. Üç ayrı daire düğmeden çok daha derli toplu.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
        ) {
            IconButton(
                onClick = onDecrement,
                enabled = canDecrement,
                modifier = Modifier
                    .size(STEPPER_TOUCH_TARGET)
                    .semantics { contentDescription = decreaseDescription }
                    .testTag("${tag}_dec"),
            ) { Icon(painterResource(R.drawable.ic_remove), contentDescription = null) }
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                // Tek satır: büyük yazı tipinde "25 dk" ikiye bölünmez, bunun yerine etiket daralır.
                maxLines = 1,
                softWrap = false,
                // En az genişlik: normal boyutta "5 dk", "25 dk" ve "4 seans" aynı yeri kaplar, kontroller
                // satırlar arasında hizalı kalır; büyük yazı tipinde değer gerektiği kadar genişler.
                modifier = Modifier
                    .widthIn(min = 64.dp)
                    .testTag("${tag}_value"),
            )
            IconButton(
                onClick = onIncrement,
                enabled = canIncrement,
                modifier = Modifier
                    .size(STEPPER_TOUCH_TARGET)
                    .semantics { contentDescription = increaseDescription }
                    .testTag("${tag}_inc"),
            ) { Icon(painterResource(R.drawable.ic_add), contentDescription = null) }
        }
    }
}

/** Geniş (yatay, tablet) ekranlarda liste ekranlarının satırları uçtan uca uzamasın: içerik ortalanır ve sınırlanır. */
@Composable
fun ContentWidthLimit(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(modifier = Modifier.widthIn(max = 640.dp).fillMaxHeight()) { content() }
    }
}
