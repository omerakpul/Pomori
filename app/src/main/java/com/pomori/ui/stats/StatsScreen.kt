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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pomori.R
import com.pomori.domain.model.Distractor
import com.pomori.ui.theme.PomoriTheme

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
    Column(modifier = modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)) {
            TextButton(onClick = onBack, modifier = Modifier.testTag("stats_back")) {
                Text(stringResource(R.string.stats_back))
            }
            Text(
                text = stringResource(R.string.stats_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.stats_period),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when {
            // İlk veri gelene kadar hiçbir şey çizme: "veri yok" yanıp sönmesin.
            state.isLoading -> Unit
            state.isEmpty -> EmptyStats()
            else -> StatsContent(state)
        }
    }
}

@Composable
private fun EmptyStats() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .testTag("stats_empty"),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.stats_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.stats_empty_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StatsContent(state: StatsUiState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("stats_content"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    label = stringResource(R.string.stats_focus_time),
                    value = focusTimeText(state),
                    valueTag = "stats_focus_time",
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = stringResource(R.string.stats_completed),
                    value = state.completedSessions.toString(),
                    valueTag = "stats_completed",
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Text(
                text = stringResource(R.string.stats_distractors_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .semantics { heading() },
            )
        }
        if (state.topDistractors.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.stats_distractors_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag("stats_distractors_empty"),
                )
            }
        } else {
            items(state.topDistractors, key = { it.packageName }) { distractor ->
                DistractorRow(distractor, maxCount = state.maxDistractorCount)
            }
        }
    }
}

@Composable
private fun focusTimeText(state: StatsUiState): String =
    if (state.focusHours > 0) {
        stringResource(R.string.stats_duration_hm, state.focusHours, state.focusMinutes)
    } else {
        stringResource(R.string.stats_duration_m, state.focusMinutes)
    }

@Composable
private fun StatTile(label: String, value: String, valueTag: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.testTag(valueTag),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DistractorRow(distractor: Distractor, maxCount: Int) {
    val countText = stringResource(R.string.stats_notification_count, distractor.count)
    val description = "${distractor.appLabel}, $countText"
    Column(
        // Çubuk görsel bir süstür; ekran okuyucu satırı tek cümle olarak okur.
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stats_distractor_${distractor.packageName}")
            .clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = distractor.appLabel,
                style = MaterialTheme.typography.bodyLarge,
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
                .height(8.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = if (maxCount == 0) 0f else distractor.count.toFloat() / maxCount)
                    .height(8.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50)),
            )
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
