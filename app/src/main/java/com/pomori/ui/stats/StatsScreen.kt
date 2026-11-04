package com.pomori.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pomori.R
import com.pomori.domain.model.DayFocus
import com.pomori.domain.model.Distractor
import com.pomori.domain.model.GoalProgress
import com.pomori.domain.model.LabelFocus
import com.pomori.ui.components.EmptyState
import com.pomori.ui.components.InitialAvatar
import com.pomori.ui.components.ScreenHeader
import com.pomori.ui.components.SectionHeader
import com.pomori.ui.theme.PomoriTheme
import com.pomori.ui.theme.StatNumberStyle

@Composable
fun StatsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StatsScreen(state = state, onBack = onBack, modifier = modifier)
}

@Composable
fun StatsScreen(
    state: StatsUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().safeDrawingPadding()) {
        ScreenHeader(
            title = stringResource(R.string.stats_title),
            subtitle = stringResource(R.string.stats_period),
            onBack = onBack,
            backTag = "stats_back",
        )
        when {
            // İlk veri gelene kadar hiçbir şey çizme: "veri yok" yanıp sönmesin.
            state.isLoading -> Unit
            state.isEmpty -> EmptyState(
                icon = R.drawable.ic_bar_chart,
                title = stringResource(R.string.stats_empty_title),
                body = stringResource(R.string.stats_empty_body),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("stats_empty"),
            )
            else -> StatsContent(state)
        }
    }
}

@Composable
private fun StatsContent(state: StatsUiState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("stats_content"),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
    ) {
        item {
            // İki büyük rakam yan yana, kutusuz: sayı kendi başına tasarımın kahramanı.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                BigStat(
                    value = focusTimeText(state),
                    label = stringResource(R.string.stats_focus_time),
                    valueTag = "stats_focus_time",
                    accent = true,
                    modifier = Modifier.weight(1f),
                )
                BigStat(
                    value = state.completedSessions.toString(),
                    label = stringResource(R.string.stats_completed),
                    valueTag = "stats_completed",
                    accent = false,
                    modifier = Modifier.weight(1f),
                )
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
        if (state.goal.enabled) {
            item { GoalCard(state.goal) }
        }
        if (state.hasChart) {
            item { SectionHeader(stringResource(R.string.stats_chart_title)) }
            item { WeekChart(days = state.dailyFocus, maxMs = state.maxDailyFocusMs) }
            item {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(top = 20.dp),
                )
            }
        }
        if (state.labelBreakdown.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.stats_labels_title), Modifier.padding(start = 0.dp)) }
            itemsIndexed(state.labelBreakdown, key = { _, it -> it.label ?: "" }) { index, entry ->
                LabelRow(entry, maxMs = state.maxLabelFocusMs)
                if (index != state.labelBreakdown.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
            item {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        item { SectionHeader(stringResource(R.string.stats_distractors_title), Modifier.padding(start = 0.dp)) }
        if (state.topDistractors.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.stats_distractors_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .testTag("stats_distractors_empty"),
                )
            }
        } else {
            itemsIndexed(state.topDistractors, key = { _, it -> it.packageName }) { index, distractor ->
                DistractorRow(distractor, rank = index + 1, maxCount = state.maxDistractorCount)
                if (index != state.topDistractors.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

private val CHART_HEIGHT = 132.dp

/**
 * Son 7 günün odak süresi: her gün için bir dikey çubuk, altında kısa gün adı. Bugün vurgulu, önceki günler
 * soluk. Sıfır olan gün de görünür (ince bir çentik): boş bir gün de bilgidir. Her çubuk ekran okuyucuya
 * "Pazartesi: 1 sa 15 dk" diye okunur; çubuk ve etiketler görsel süstür.
 */
@Composable
private fun WeekChart(days: List<DayFocus>, maxMs: Long) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stats_chart"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        days.forEachIndexed { index, day ->
            val isToday = index == days.lastIndex
            val fraction = if (maxMs == 0L) 0f else day.focusMs.toFloat() / maxMs
            val dayName = day.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
            val description = stringResource(R.string.stats_bar_description, dayName, durationText(day.focusMs))
            Column(
                modifier = Modifier
                    .weight(1f)
                    // testTag, clearAndSetSemantics'ten ÖNCE gelmeli: sonra gelirse onun tarafından silinir.
                    .testTag("stats_bar_$index")
                    .clearAndSetSemantics { contentDescription = description },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(CHART_HEIGHT), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            // Süresi olan en kısa gün bile görünsün (6 dp); sıfır gün 3 dp'lik bir çentik.
                            .height(if (day.focusMs == 0L) 3.dp else (CHART_HEIGHT * fraction).coerceAtLeast(6.dp))
                            .background(
                                color = when {
                                    day.focusMs == 0L -> scheme.outlineVariant
                                    isToday -> scheme.primary
                                    else -> scheme.primary.copy(alpha = 0.38f)
                                },
                                shape = RoundedCornerShape(6.dp),
                            ),
                    )
                }
                Text(
                    text = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isToday) scheme.primary else scheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Süreyi "X sa Y dk" ya da "Y dk" yazar. */
@Composable
private fun durationText(ms: Long): String {
    val hours = (ms / 3_600_000L).toInt()
    val minutes = ((ms / 60_000L) % 60).toInt()
    return if (hours > 0) {
        stringResource(R.string.stats_duration_hm, hours, minutes)
    } else {
        stringResource(R.string.stats_duration_m, minutes)
    }
}

@Composable
private fun focusTimeText(state: StatsUiState): String =
    if (state.focusHours > 0) {
        stringResource(R.string.stats_duration_hm, state.focusHours, state.focusMinutes)
    } else {
        stringResource(R.string.stats_duration_m, state.focusMinutes)
    }

/**
 * "6 sa 15 dk" gibi bir değerde rakamlar büyük, birimler ("sa", "dk") küçük yazılır: yarım genişlikli bir
 * sütuna sığar ve gözün rakamlara gitmesini sağlar. Metin aynı kalır (ekran okuyucu ve testler için).
 */
private fun withSmallUnits(value: String): AnnotatedString = buildAnnotatedString {
    // Ardışık rakam grupları ve rakam olmayan gruplar (birimler).
    Regex("[0-9]+|[^0-9]+").findAll(value).forEach { part ->
        if (part.value.first().isDigit()) {
            append(part.value)
        } else {
            withStyle(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium)) { append(part.value) }
        }
    }
}

@Composable
private fun BigStat(value: String, label: String, valueTag: String, accent: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = withSmallUnits(value),
            style = StatNumberStyle,
            color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag(valueTag),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GoalCard(goal: GoalProgress) {
    val today = durationText(goal.todayMs)
    val target = durationText(goal.goalMs)
    val streakText = if (goal.streak > 0) pluralStringResource(R.plurals.stats_streak_days, goal.streak, goal.streak) else null
    val progressText = stringResource(R.string.stats_goal_progress, today, target)
    val description = listOfNotNull(
        stringResource(R.string.stats_goal_title),
        progressText,
        if (goal.reached) stringResource(R.string.stats_goal_reached) else null,
        streakText,
    ).joinToString(". ")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp)
            .testTag("stats_goal")
            .clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.stats_goal_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (streakText != null) {
                Text(
                    text = streakText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("stats_streak"),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = goal.fraction)
                    .height(8.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50)),
            )
        }
        Text(
            text = if (goal.reached) stringResource(R.string.stats_goal_reached) else progressText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun LabelRow(entry: LabelFocus, maxMs: Long) {
    val name = entry.label ?: stringResource(R.string.stats_label_none)
    val duration = durationText(entry.focusMs)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp)
            .testTag("stats_label_${entry.label ?: "none"}")
            .clearAndSetSemantics { contentDescription = "$name, $duration" },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(text = duration, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = if (maxMs == 0L) 0f else entry.focusMs.toFloat() / maxMs)
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50)),
            )
        }
    }
}

@Composable
private fun DistractorRow(distractor: Distractor, rank: Int, maxCount: Int) {
    val countText = stringResource(R.string.stats_notification_count, distractor.count)
    val description = "${distractor.appLabel}, $countText"
    Row(
        // Sıra numarası, damga ve çubuk görsel süstür; ekran okuyucu satırı tek cümle olarak okur.
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp)
            .testTag("stats_distractor_${distractor.packageName}")
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = rank.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        InitialAvatar(name = distractor.appLabel)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = distractor.appLabel,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = countText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = if (maxCount == 0) 0f else distractor.count.toFloat() / maxCount)
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50)),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StatsScreenPreview() {
    PomoriTheme {
        StatsScreen(
            state = StatsUiState(
                isLoading = false,
                totalFocusMs = (2 * 60 + 35) * 60_000L,
                completedSessions = 6,
                topDistractors = listOf(
                    Distractor("com.whatsapp", "WhatsApp", 12),
                    Distractor("com.google.android.gm", "Gmail", 7),
                    Distractor("org.telegram.messenger", "Telegram", 3),
                ),
                dailyFocus = sampleWeek(),
            ),
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StatsScreenEmptyPreview() {
    PomoriTheme { StatsScreen(state = StatsUiState(isLoading = false), onBack = {}) }
}

private fun sampleWeek(): List<DayFocus> {
    val minutes = listOf(50L, 100L, 0L, 75L, 125L, 25L, 155L)
    val today = LocalDate.of(2026, 11, 4)
    return minutes.mapIndexed { i, m -> DayFocus(today.minusDays((6 - i).toLong()), m * 60_000L) }
}
