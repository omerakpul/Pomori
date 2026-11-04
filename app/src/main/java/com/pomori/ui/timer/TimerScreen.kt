package com.pomori.ui.timer

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pomori.R
import com.pomori.domain.model.Preset
import com.pomori.platform.permission.NotificationListenerAccess
import com.pomori.platform.permission.NotificationPermission
import com.pomori.platform.service.FocusForegroundService
import com.pomori.ui.components.ambientGlow
import com.pomori.ui.permission.NotificationAccessCard
import com.pomori.ui.permission.rememberNotificationAccessGranted
import com.pomori.ui.theme.PomoriTheme
import com.pomori.ui.theme.TimerDigitsStyle
import kotlin.math.cos
import kotlin.math.sin

/** ViewModel'e, izin isteme akışına ve servis başlatmaya bağlanan giriş noktası. */
@Composable
fun TimerRoute(
    onOpenDigest: (sessionId: Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenPresets: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TimerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pendingDigest by viewModel.pendingDigestSessionId.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val accessGranted = rememberNotificationAccessGranted()

    // Odak bitince o seansın özetine git; ViewModel aynı seans için yalnızca bir kez önerir.
    LaunchedEffect(pendingDigest) {
        pendingDigest?.let { sessionId ->
            viewModel.onDigestShown(sessionId)
            onOpenDigest(sessionId)
        }
    }

    // İzin verilse de verilmese de seans başlar: reddedilirse uygulama bozulmaz,
    // yalnızca sistem bildirimi görünmez (sayaç uygulama içinde çalışmaya devam eder).
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.startFocus() }

    // Seans sürerken (ekran açıldığında da) servisin çalıştığından emin ol.
    LaunchedEffect(state.phase) {
        if (state.phase == TimerPhase.FOCUS || state.phase == TimerPhase.BREAK) {
            FocusForegroundService.start(context)
        }
    }

    TimerScreen(
        state = state,
        onStartFocus = {
            if (NotificationPermission.isGranted(context)) {
                viewModel.startFocus()
            } else {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onCancelFocus = viewModel::cancelFocus,
        onInterrupt = viewModel::interrupt,
        onStartBreak = viewModel::startBreak,
        onSkipBreak = viewModel::skipBreak,
        notificationAccessGranted = accessGranted,
        onOpenNotificationAccess = { NotificationListenerAccess.openSettings(context) },
        onOpenSettings = onOpenSettings,
        onOpenStats = onOpenStats,
        onSelectPreset = viewModel::selectPreset,
        onOpenPresets = onOpenPresets,
        modifier = modifier,
    )
}

@Composable
fun TimerScreen(
    state: TimerUiState,
    onStartFocus: () -> Unit,
    onCancelFocus: () -> Unit,
    onStartBreak: () -> Unit,
    onSkipBreak: () -> Unit,
    modifier: Modifier = Modifier,
    onInterrupt: () -> Unit = {},
    notificationAccessGranted: Boolean = true,
    onOpenNotificationAccess: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenStats: () -> Unit = {},
    onSelectPreset: (Long) -> Unit = {},
    onOpenPresets: () -> Unit = {},
) {
    val phaseLabel = when (state.phase) {
        TimerPhase.IDLE -> stringResource(R.string.phase_idle)
        TimerPhase.FOCUS -> stringResource(R.string.phase_focus)
        TimerPhase.BREAK_PENDING -> stringResource(R.string.phase_break_pending)
        TimerPhase.BREAK ->
            stringResource(if (state.isLongBreak) R.string.phase_long_break else R.string.phase_break)
    }
    val timeText = if (state.phase == TimerPhase.BREAK_PENDING) {
        stringResource(if (state.isLongBreak) R.string.time_long_break_pending else R.string.time_break_pending)
    } else {
        formatRemaining(state.remainingMs)
    }
    val ringDescription = stringResource(R.string.timer_content_description, phaseLabel, timeText)

    // Odak kırmızı, mola yeşil: kullanıcı hangi fazda olduğunu okumadan, renkten anlar. Renk geçişi yumuşak.
    val isBreakPhase = state.phase == TimerPhase.BREAK_PENDING || state.phase == TimerPhase.BREAK
    val scheme = MaterialTheme.colorScheme
    val accent by animateColorAsState(if (isBreakPhase) scheme.secondary else scheme.primary, tween(600), label = "accent")
    val onAccent = if (isBreakPhase) scheme.onSecondary else scheme.onPrimary

    Column(
        modifier = modifier
            .fillMaxSize()
            // Işıma durum çubuğunun altına da uzanır; içerik ise güvenli alana oturur.
            .ambientGlow(accent)
            .safeDrawingPadding(),
    ) {
        TimerTopBar(
            accent = accent,
            showEntries = state.phase == TimerPhase.IDLE,
            onOpenStats = onOpenStats,
            onOpenSettings = onOpenSettings,
        )

        // Uyarı yalnızca yakalamanın anlamlı olduğu fazlarda (boşta, odak) görünür. Sayacın üstüne binen bir
        // katman değil, sütunun bir öğesidir: büyük yazı tipinde kart uzayınca etiketin üstüne taşmaz.
        if (!notificationAccessGranted && (state.phase == TimerPhase.IDLE || state.phase == TimerPhase.FOCUS)) {
            NotificationAccessCard(
                onOpenSettings = onOpenNotificationAccess,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
            )
        }

        // Modlar yalnızca boştayken seçilir: çalışan bir seansın ya da bekleyen molanın ortasında mod değişirse
        // mola planı yanlış modun süresine göre hesaplanırdı.
        if (state.phase == TimerPhase.IDLE && state.presets.isNotEmpty()) {
            PresetChips(
                presets = state.presets,
                selectedId = state.selectedPresetId,
                onSelect = onSelectPreset,
                onManage = onOpenPresets,
            )
        }

        // Sığıyorsa ortalanır, sığmıyorsa (büyük yazı tipi, küçük ekran) kaydırılır: hiçbir düğme kesilmez.
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = maxHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = phaseLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = accent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag(TAG_PHASE),
                )
                Spacer(Modifier.height(14.dp))
                CycleDots(filled = state.cycleFilled, total = state.cycleLength, accent = accent)
                Spacer(Modifier.height(20.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(280.dp)
                        .semantics(mergeDescendants = true) { contentDescription = ringDescription },
                ) {
                    DialRing(progress = state.progress, color = accent, modifier = Modifier.fillMaxSize())
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (state.phase == TimerPhase.BREAK_PENDING) {
                            Icon(
                                painter = painterResource(R.drawable.ic_coffee),
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .size(32.dp),
                            )
                        }
                        Text(
                            text = timeText,
                            // Rakamlar büyük, ama "Mola zamanı" gibi bir kelime kadrana sığmak için daha küçük yazılır.
                            style = if (state.phase == TimerPhase.BREAK_PENDING) {
                                MaterialTheme.typography.headlineMedium
                            } else {
                                TimerDigitsStyle
                            },
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier.testTag(TAG_TIME),
                        )
                        sessionCaption(state)?.let { caption ->
                            Text(
                                text = caption,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(40.dp))

                TimerActions(
                    phase = state.phase,
                    isLongBreak = state.isLongBreak,
                    accent = accent,
                    onAccent = onAccent,
                    onStartFocus = onStartFocus,
                    onCancelFocus = onCancelFocus,
                    onStartBreak = onStartBreak,
                    onSkipBreak = onSkipBreak,
                    interruptions = state.interruptions,
                    onInterrupt = onInterrupt,
                )
                Spacer(Modifier.height(20.dp))

                Text(
                    text = hintText(state),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 300.dp),
                )
            }
        }
    }
}

/**
 * Marka satırı: vurgu renginde bir nokta ve ad. Ayarlar ve istatistikler yalnızca boştayken görünür: süreyi
 * çalışan bir seansın ortasında değiştirmek karışıklık yaratır, istatistikler de seans sırasında dikkat dağıtmasın.
 */
@Composable
private fun TimerTopBar(accent: Color, showEntries: Boolean, onOpenStats: () -> Unit, onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(start = 24.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).background(accent, CircleShape))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .padding(start = 10.dp)
                .weight(1f)
                .semantics { heading() },
        )
        if (showEntries) {
            IconButton(onClick = onOpenStats, modifier = Modifier.testTag(TAG_STATS)) {
                Icon(
                    painter = painterResource(R.drawable.ic_bar_chart),
                    contentDescription = stringResource(R.string.timer_stats),
                )
            }
            IconButton(onClick = onOpenSettings, modifier = Modifier.testTag(TAG_SETTINGS)) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.timer_settings),
                )
            }
        }
    }
}

private const val DIAL_TICKS = 60

/**
 * Çentikli kadran: 60 çentik, kalan süre kadarı yanar. Saatin saat kadranını andırır; dolu bir çember yerine
 * zamanın "parçalarını" gösterir. Sınırdaki çentik yavaşça söner, böylece ilerleme saniyede bir sıçramaz.
 */
@Composable
private fun DialRing(progress: Float, color: Color, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 900, easing = LinearEasing),
        label = "dialProgress",
    )
    val unlit = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val stroke = 3.5.dp.toPx()
        val outer = radius - 4.dp.toPx()
        val lit = animated * DIAL_TICKS
        for (i in 0 until DIAL_TICKS) {
            val length = if (i % 5 == 0) 26.dp.toPx() else 14.dp.toPx()
            val angle = Math.toRadians(-90.0 + i * 360.0 / DIAL_TICKS)
            val dx = cos(angle).toFloat()
            val dy = sin(angle).toFloat()
            val tickColor = lerp(unlit, color, (lit - i).coerceIn(0f, 1f))
            drawLine(
                color = tickColor,
                start = Offset(center.x + dx * (outer - length), center.y + dy * (outer - length)),
                end = Offset(center.x + dx * outer, center.y + dy * outer),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun TimerActions(
    phase: TimerPhase,
    isLongBreak: Boolean,
    accent: Color,
    onAccent: Color,
    onStartFocus: () -> Unit,
    onCancelFocus: () -> Unit,
    onStartBreak: () -> Unit,
    onSkipBreak: () -> Unit,
    interruptions: Int = 0,
    onInterrupt: () -> Unit = {},
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (phase) {
            TimerPhase.IDLE -> RoundAction(
                icon = R.drawable.ic_play,
                label = stringResource(R.string.action_start_focus),
                onClick = onStartFocus,
                tag = TAG_START_FOCUS,
                filled = true,
                accent = accent,
                onAccent = onAccent,
            )

            TimerPhase.FOCUS -> {
                RoundAction(
                    icon = R.drawable.ic_stop,
                    label = stringResource(R.string.action_cancel),
                    onClick = onCancelFocus,
                    tag = TAG_CANCEL,
                    filled = false,
                    accent = accent,
                    onAccent = onAccent,
                )
                // Süre durmaz: kullanıcı dikkatinin dağıldığını kendisi işaretler, seans kaydına sayı olarak yazılır.
                TextButton(onClick = onInterrupt, modifier = Modifier.testTag(TAG_INTERRUPT)) {
                    Text(
                        text = if (interruptions > 0) {
                            stringResource(R.string.action_interrupt_count, interruptions)
                        } else {
                            stringResource(R.string.action_interrupt)
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            TimerPhase.BREAK_PENDING -> {
                RoundAction(
                    icon = R.drawable.ic_coffee,
                    label = stringResource(
                        if (isLongBreak) R.string.action_start_long_break else R.string.action_start_break,
                    ),
                    onClick = onStartBreak,
                    tag = TAG_START_BREAK,
                    filled = true,
                    accent = accent,
                    onAccent = onAccent,
                )
                TextButton(onClick = onSkipBreak, modifier = Modifier.testTag(TAG_SKIP)) {
                    Text(stringResource(R.string.action_skip_break), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            TimerPhase.BREAK -> RoundAction(
                icon = R.drawable.ic_skip_next,
                label = stringResource(R.string.action_skip_break),
                onClick = onSkipBreak,
                tag = TAG_SKIP,
                filled = false,
                accent = accent,
                onAccent = onAccent,
            )
        }
    }
}

/**
 * Büyük yuvarlak eylem düğmesi ve altında etiketi. Dolu (ana eylem) ya da yalnızca çizgili (ikincil) olabilir.
 * Düğmenin kendisi tek bir dokunma hedefi; etiket ekran okuyucuya tekrar okunmaz (düğmenin açıklaması aynı metin).
 */
@Composable
private fun RoundAction(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    tag: String,
    filled: Boolean,
    accent: Color,
    onAccent: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(if (filled) 84.dp else 68.dp)
                .clip(CircleShape)
                .then(
                    if (filled) Modifier.background(accent)
                    else Modifier.border(BorderStroke(2.dp, accent), CircleShape),
                )
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = label }
                .testTag(tag),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = if (filled) onAccent else accent,
                modifier = Modifier.size(if (filled) 38.dp else 30.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

/** Kadranın altındaki küçük açıklama: "25 dakikalık odak" gibi. "Mola zamanı" ekranında gerek yok. */
@Composable
private fun sessionCaption(state: TimerUiState): String? {
    val minutes = (state.totalMs / 60_000L).toInt()
    return when (state.phase) {
        TimerPhase.IDLE, TimerPhase.FOCUS -> stringResource(R.string.timer_caption_focus, minutes)
        TimerPhase.BREAK -> stringResource(
            if (state.isLongBreak) R.string.timer_caption_long_break else R.string.timer_caption_break,
            minutes,
        )
        TimerPhase.BREAK_PENDING -> null
    }
}

/** Fazın altındaki kısa ipucu. Uzun mola hak edilmişken tebrik eder. */
@Composable
private fun hintText(state: TimerUiState): String = when (state.phase) {
    TimerPhase.IDLE -> stringResource(R.string.timer_hint_idle)
    TimerPhase.FOCUS -> stringResource(R.string.timer_hint_focus)
    TimerPhase.BREAK_PENDING ->
        if (state.isLongBreak) {
            stringResource(R.string.timer_hint_long_break_pending, state.cycleLength)
        } else {
            stringResource(R.string.timer_hint_break_pending)
        }
    TimerPhase.BREAK ->
        stringResource(if (state.isLongBreak) R.string.timer_hint_long_break else R.string.timer_hint_break)
}

/**
 * Pomodoro döngüsü: uzun molaya kadar kaç odak seansı gerektiğini ve kaçının tamamlandığını gösteren noktalar.
 * Dolu noktalar vurgu renginde. Süs; ekran okuyucuya tek cümle ("Döngü: 2 / 4 odak seansı") okunur.
 */
@Composable
private fun CycleDots(filled: Int, total: Int, accent: Color) {
    val description = stringResource(R.string.timer_cycle_description, filled, total)
    val empty = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .testTag(TAG_CYCLE)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        repeat(total) { index ->
            Box(Modifier.size(8.dp).background(if (index < filled) accent else empty, CircleShape))
        }
    }
}

/**
 * Mod çipleri: her mod için bir çip, sonda modları düzenleme girişi. Seçili çip vurgu renginde. Yatay kaydırılır:
 * çok sayıda mod ya da büyük yazı tipinde hiçbiri kesilmez.
 */
@Composable
private fun PresetChips(
    presets: List<Preset>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onManage: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 4.dp)
            .testTag(TAG_PRESETS),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        presets.forEach { preset ->
            val selected = preset.id == selectedId
            // Dokunma hedefi 48 dp; görünen çip biraz daha ince (40 dp).
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Tab, onClick = { onSelect(preset.id) })
                    .semantics { this.selected = selected }
                    .testTag("timer_preset_${preset.id}"),
            ) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) scheme.primary else scheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier
                        .background(
                            color = if (selected) scheme.primary.copy(alpha = 0.16f) else Color.Transparent,
                            shape = RoundedCornerShape(14.dp),
                        )
                        .border(
                            width = 1.dp,
                            color = if (selected) scheme.primary else scheme.outlineVariant,
                            shape = RoundedCornerShape(14.dp),
                        )
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                )
            }
        }
        IconButton(onClick = onManage, modifier = Modifier.testTag(TAG_PRESETS_EDIT)) {
            Icon(
                painter = painterResource(R.drawable.ic_edit),
                contentDescription = stringResource(R.string.timer_presets_edit),
                tint = scheme.onSurfaceVariant,
            )
        }
    }
}

const val TAG_PRESETS = "timer_presets"
const val TAG_PRESETS_EDIT = "timer_presets_edit"
const val TAG_CYCLE = "timer_cycle"
const val TAG_PHASE = "timer_phase"
const val TAG_TIME = "timer_time"
const val TAG_START_FOCUS = "timer_start_focus"
const val TAG_CANCEL = "timer_cancel"
const val TAG_INTERRUPT = "timer_interrupt"
const val TAG_START_BREAK = "timer_start_break"
const val TAG_SKIP = "timer_skip"
const val TAG_SETTINGS = "timer_settings"
const val TAG_STATS = "timer_stats"

@Preview(showBackground = true)
@Composable
private fun TimerScreenIdlePreview() {
    PomoriTheme {
        TimerScreen(state = TimerUiState(), onStartFocus = {}, onCancelFocus = {}, onStartBreak = {}, onSkipBreak = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun TimerScreenFocusPreview() {
    PomoriTheme {
        TimerScreen(
            state = TimerUiState(TimerPhase.FOCUS, remainingMs = 18 * 60_000L + 5_000L, totalMs = 25 * 60_000L),
            onStartFocus = {},
            onCancelFocus = {},
            onStartBreak = {},
            onSkipBreak = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TimerScreenBreakPendingPreview() {
    PomoriTheme {
        TimerScreen(
            state = TimerUiState(TimerPhase.BREAK_PENDING, remainingMs = 0, totalMs = 5 * 60_000L),
            onStartFocus = {},
            onCancelFocus = {},
            onStartBreak = {},
            onSkipBreak = {},
        )
    }
}
