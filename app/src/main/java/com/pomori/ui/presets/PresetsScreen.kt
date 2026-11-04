package com.pomori.ui.presets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pomori.R
import com.pomori.domain.model.Preset
import com.pomori.domain.model.SettingsDefaults
import com.pomori.ui.components.IconBadge
import com.pomori.ui.components.IconLabel
import com.pomori.ui.components.PomoriButton
import com.pomori.ui.components.ScreenHeader
import com.pomori.ui.components.SoftCard
import com.pomori.ui.components.StepperRow
import com.pomori.ui.theme.PomoriTheme

/** Odak ve uzun mola süreleri 5'er dakika, mola süresi ve uzun mola sıklığı 1'er birim değişir. */
private const val BIG_STEP = 5

/** Düzenleyicide `null` = kapalı, `NEW` = yeni mod, diğerleri düzenlenen modun id'si. */
private const val NEW = 0L

@Composable
fun PresetsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PresetsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PresetsScreen(
        state = state,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun PresetsScreen(
    state: PresetsUiState,
    onSave: (Preset) -> Unit,
    onDelete: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Yalnızca id saklanır (Preset kaydedilebilir değil); düzenlenen mod her seferinde güncel listeden okunur.
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }

    Column(modifier = modifier.fillMaxSize().safeDrawingPadding()) {
        ScreenHeader(
            title = stringResource(R.string.presets_title),
            subtitle = stringResource(R.string.presets_subtitle),
            onBack = onBack,
            backTag = "presets_back",
        )
        // İlk veri gelene kadar hiçbir şey çizme: boş liste yanıp sönmesin.
        if (!state.isLoading) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .testTag("presets_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.presets, key = { it.id }) { preset ->
                    PresetCard(
                        preset = preset,
                        isSelected = preset.id == state.selectedId,
                        onClick = { editingId = preset.id },
                    )
                }
            }
            PomoriButton(
                onClick = { editingId = NEW },
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("presets_add"),
            ) {
                IconLabel(R.drawable.ic_add, stringResource(R.string.presets_add))
            }
        }
    }

    val id = editingId
    if (id != null) {
        val existing = state.presets.firstOrNull { it.id == id }
        // Düzenlenen mod başka yerden silindiyse (ya da liste henüz yoksa) düzenleyici kapanır.
        if (id == NEW || existing != null) {
            PresetEditor(
                initial = existing ?: Preset.blank(),
                isNew = id == NEW,
                canDelete = state.canDelete,
                onSave = { onSave(it); editingId = null },
                onDelete = { onDelete(id); editingId = null },
                onDismiss = { editingId = null },
            )
        }
    }
}

@Composable
private fun PresetCard(preset: Preset, isSelected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    SoftCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .testTag("preset_card_${preset.id}"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            IconBadge(
                icon = R.drawable.ic_schedule,
                tint = if (isSelected) scheme.primary else scheme.onSurfaceVariant,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = presetSummary(preset),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            if (isSelected) {
                Text(
                    text = stringResource(R.string.presets_active),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.primary,
                    modifier = Modifier
                        .background(scheme.primary.copy(alpha = 0.14f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("preset_active_${preset.id}"),
                )
            }
        }
    }
}

@Composable
private fun presetSummary(preset: Preset): String = stringResource(
    R.string.presets_summary,
    preset.focusMinutes,
    preset.breakMinutes,
    preset.longBreakMinutes,
)

/**
 * Mod düzenleyicisi: alttan açılan sayfa. Değerler yerel taslakta tutulur; yalnızca Kaydet'e basılınca depoya yazılır,
 * Vazgeç ya da kapatma hiçbir şeyi değiştirmez.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetEditor(
    initial: Preset,
    isNew: Boolean,
    canDelete: Boolean,
    onSave: (Preset) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable(initial.id) { mutableStateOf(initial.name) }
    var focus by rememberSaveable(initial.id) { mutableIntStateOf(initial.focusMinutes) }
    var shortBreak by rememberSaveable(initial.id) { mutableIntStateOf(initial.breakMinutes) }
    var longBreak by rememberSaveable(initial.id) { mutableIntStateOf(initial.longBreakMinutes) }
    var every by rememberSaveable(initial.id) { mutableIntStateOf(initial.longBreakEvery) }
    val scheme = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = scheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
                .testTag("preset_editor"),
        ) {
            Text(
                text = stringResource(if (isNew) R.string.preset_editor_new else R.string.preset_editor_edit),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .semantics { heading() },
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(Preset.NAME_MAX_LENGTH) },
                label = { Text(stringResource(R.string.preset_name_label)) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .testTag("preset_name_input"),
            )
            StepperRow(
                icon = R.drawable.ic_schedule,
                tint = scheme.primary,
                label = stringResource(R.string.settings_focus_duration),
                valueText = stringResource(R.string.settings_minutes_format, focus),
                canDecrement = focus > SettingsDefaults.FOCUS_RANGE.first,
                canIncrement = focus < SettingsDefaults.FOCUS_RANGE.last,
                onDecrement = { focus = (focus - BIG_STEP).coerceIn(SettingsDefaults.FOCUS_RANGE) },
                onIncrement = { focus = (focus + BIG_STEP).coerceIn(SettingsDefaults.FOCUS_RANGE) },
                tag = "preset_focus",
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            StepperRow(
                icon = R.drawable.ic_coffee,
                tint = scheme.secondary,
                label = stringResource(R.string.settings_break_duration),
                valueText = stringResource(R.string.settings_minutes_format, shortBreak),
                canDecrement = shortBreak > SettingsDefaults.BREAK_RANGE.first,
                canIncrement = shortBreak < SettingsDefaults.BREAK_RANGE.last,
                onDecrement = { shortBreak = (shortBreak - 1).coerceIn(SettingsDefaults.BREAK_RANGE) },
                onIncrement = { shortBreak = (shortBreak + 1).coerceIn(SettingsDefaults.BREAK_RANGE) },
                tag = "preset_break",
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            StepperRow(
                icon = R.drawable.ic_coffee,
                tint = scheme.secondary,
                label = stringResource(R.string.settings_long_break_duration),
                valueText = stringResource(R.string.settings_minutes_format, longBreak),
                canDecrement = longBreak > SettingsDefaults.LONG_BREAK_RANGE.first,
                canIncrement = longBreak < SettingsDefaults.LONG_BREAK_RANGE.last,
                onDecrement = { longBreak = (longBreak - BIG_STEP).coerceIn(SettingsDefaults.LONG_BREAK_RANGE) },
                onIncrement = { longBreak = (longBreak + BIG_STEP).coerceIn(SettingsDefaults.LONG_BREAK_RANGE) },
                tag = "preset_long_break",
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            StepperRow(
                icon = R.drawable.ic_list,
                tint = scheme.tertiary,
                label = stringResource(R.string.preset_every_label),
                valueText = stringResource(R.string.preset_every_value, every),
                canDecrement = every > SettingsDefaults.LONG_BREAK_EVERY_RANGE.first,
                canIncrement = every < SettingsDefaults.LONG_BREAK_EVERY_RANGE.last,
                onDecrement = { every = (every - 1).coerceIn(SettingsDefaults.LONG_BREAK_EVERY_RANGE) },
                onIncrement = { every = (every + 1).coerceIn(SettingsDefaults.LONG_BREAK_EVERY_RANGE) },
                tag = "preset_every",
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            HorizontalDivider(
                color = scheme.outlineVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            PomoriButton(
                onClick = {
                    onSave(
                        initial.copy(
                            name = name,
                            focusMinutes = focus,
                            breakMinutes = shortBreak,
                            longBreakMinutes = longBreak,
                            longBreakEvery = every,
                        ),
                    )
                },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .testTag("preset_save"),
            ) {
                Text(stringResource(R.string.preset_save))
            }
            // Son kalan mod silinemez; yeni mod henüz kayıtlı değil, silinecek bir şey yok.
            if (canDelete && !isNew) {
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .testTag("preset_delete"),
                ) {
                    Text(stringResource(R.string.preset_delete), color = scheme.error)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PresetsScreenPreview() {
    PomoriTheme {
        PresetsScreen(
            state = PresetsUiState(
                isLoading = false,
                presets = listOf(
                    Preset(1, "Varsayılan", 25, 5, 15),
                    Preset(2, "Ders", 40, 5, 15),
                    Preset(3, "Hızlı", 20, 2, 10),
                ),
                selectedId = 2,
            ),
            onSave = {},
            onDelete = {},
            onBack = {},
        )
    }
}
