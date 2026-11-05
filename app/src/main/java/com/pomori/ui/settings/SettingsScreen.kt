package com.pomori.ui.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pomori.R
import com.pomori.domain.model.ExceptionRule
import com.pomori.domain.model.InstalledApp
import com.pomori.domain.model.SettingsDefaults
import com.pomori.ui.components.IconBadge
import com.pomori.ui.components.IconLabel
import com.pomori.ui.components.InitialAvatar
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import com.pomori.domain.model.AppLanguage
import com.pomori.domain.model.ThemeMode
import com.pomori.ui.components.ScreenHeader
import com.pomori.ui.components.SectionHeader
import com.pomori.ui.components.SoftCard
import com.pomori.ui.components.StepperRow
import com.pomori.ui.theme.PomoriTheme

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onOpenPresets: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    SettingsScreen(
        state = state,
        onOpenPresets = onOpenPresets,
        onRetentionDaysChange = viewModel::setRetentionDays,
        onDailyGoalChange = viewModel::setDailyGoalMinutes,
        onThemeModeChange = viewModel::setThemeMode,
        onLanguageChange = { language ->
            viewModel.setLanguage(language)
            // Dil, Activity'nin bağlamına işlenir: ekranların yeni dille çizilmesi için yeniden oluşturulur.
            var ctx = context
            while (ctx is android.content.ContextWrapper && ctx !is android.app.Activity) ctx = ctx.baseContext
            (ctx as? android.app.Activity)?.recreate()
        },
        onRestoreNotificationsChange = viewModel::setRestoreNotifications,
        onAddSender = viewModel::addSender,
        onAddApp = viewModel::addApp,
        onRemoveRule = viewModel::removeRule,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onRetentionDaysChange: (Int) -> Unit,
    onAddSender: (String) -> Unit,
    onAddApp: (InstalledApp) -> Unit,
    onRemoveRule: (ExceptionRule) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenPresets: () -> Unit = {},
    onRestoreNotificationsChange: (Boolean) -> Unit = {},
    onDailyGoalChange: (Int) -> Unit = {},
    onThemeModeChange: (ThemeMode) -> Unit = {},
    onLanguageChange: (AppLanguage) -> Unit = {},
) {
    var showAppPicker by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .testTag("settings_list"),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            ScreenHeader(
                title = stringResource(R.string.settings_title),
                onBack = onBack,
                backTag = "settings_back",
            )
        }

        item { SectionHeader(stringResource(R.string.settings_section_presets), Modifier.padding(horizontal = 16.dp)) }
        item {
            // Süreler artık modlarda ("Ders" 40/5/15, "Kodlama" 25/5/15...). Burası modlar ekranının kapısı.
            SoftCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button, onClick = onOpenPresets)
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .testTag("settings_presets"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    IconBadge(icon = R.drawable.ic_schedule, tint = scheme.primary)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.settings_presets_title),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringResource(
                                R.string.settings_presets_summary,
                                state.presetCount,
                                state.selectedPresetName ?: "-",
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.testTag("settings_presets_summary"),
                        )
                    }
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = scheme.onSurfaceVariant,
                    )
                }
            }
        }

        item { SectionHeader(stringResource(R.string.settings_section_notifications), Modifier.padding(horizontal = 16.dp)) }
        item {
            SoftCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                // Tüm satır tek bir anahtar: etiket de dokunma hedefi, ekran okuyucu "açık/kapalı" olarak okur.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = state.restoreNotifications,
                            role = Role.Switch,
                            onValueChange = onRestoreNotificationsChange,
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .testTag("settings_restore"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    IconBadge(icon = R.drawable.ic_notifications, tint = scheme.tertiary)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.settings_restore_title),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringResource(R.string.settings_restore_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = state.restoreNotifications, onCheckedChange = null)
                }
            }
        }

        item { SectionHeader(stringResource(R.string.settings_section_appearance), Modifier.padding(horizontal = 16.dp)) }
        item {
            SoftCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = state.themeMode == mode,
                            onClick = { onThemeModeChange(mode) },
                            label = {
                                Text(
                                    stringResource(
                                        when (mode) {
                                            ThemeMode.SYSTEM -> R.string.settings_theme_system
                                            ThemeMode.LIGHT -> R.string.settings_theme_light
                                            ThemeMode.DARK -> R.string.settings_theme_dark
                                        },
                                    ),
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = scheme.primary.copy(alpha = 0.16f),
                                selectedLabelColor = scheme.primary,
                            ),
                            modifier = Modifier.testTag("settings_theme_${mode.name.lowercase()}"),
                        )
                    }
                }
            }
        }

        item { SectionHeader(stringResource(R.string.settings_section_language), Modifier.padding(horizontal = 16.dp)) }
        item {
            SoftCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppLanguage.entries.forEach { language ->
                        FilterChip(
                            selected = state.language == language,
                            onClick = { onLanguageChange(language) },
                            label = {
                                Text(
                                    stringResource(
                                        when (language) {
                                            AppLanguage.SYSTEM -> R.string.settings_language_system
                                            AppLanguage.TURKISH -> R.string.settings_language_turkish
                                            AppLanguage.ENGLISH -> R.string.settings_language_english
                                        },
                                    ),
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = scheme.primary.copy(alpha = 0.16f),
                                selectedLabelColor = scheme.primary,
                            ),
                            modifier = Modifier.testTag("settings_language_${language.name.lowercase()}"),
                        )
                    }
                }
            }
        }

        item { SectionHeader(stringResource(R.string.settings_section_goal), Modifier.padding(horizontal = 16.dp)) }
        item {
            SoftCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(Modifier.padding(vertical = 4.dp)) {
                    StepperRow(
                        icon = R.drawable.ic_schedule,
                        tint = scheme.primary,
                        label = stringResource(R.string.settings_goal),
                        valueText = if (state.dailyGoalMinutes == 0) {
                            stringResource(R.string.settings_goal_off)
                        } else {
                            stringResource(R.string.settings_goal_minutes, state.dailyGoalMinutes)
                        },
                        canDecrement = state.dailyGoalMinutes > SettingsDefaults.DAILY_GOAL_RANGE.first,
                        canIncrement = state.dailyGoalMinutes < SettingsDefaults.DAILY_GOAL_RANGE.last,
                        onDecrement = { onDailyGoalChange(state.dailyGoalMinutes - SettingsDefaults.DAILY_GOAL_STEP) },
                        onIncrement = { onDailyGoalChange(state.dailyGoalMinutes + SettingsDefaults.DAILY_GOAL_STEP) },
                        tag = "settings_goal",
                    )
                    Text(
                        text = stringResource(R.string.settings_goal_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    )
                }
            }
        }

        item { SectionHeader(stringResource(R.string.settings_section_retention), Modifier.padding(horizontal = 16.dp)) }
        item {
            SoftCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(Modifier.padding(vertical = 4.dp)) {
                    StepperRow(
                        icon = R.drawable.ic_delete,
                        tint = scheme.tertiary,
                        label = stringResource(R.string.settings_retention),
                        valueText = stringResource(R.string.settings_days_format, state.retentionDays),
                        canDecrement = state.retentionDays > SettingsDefaults.RETENTION_RANGE.first,
                        canIncrement = state.retentionDays < SettingsDefaults.RETENTION_RANGE.last,
                        onDecrement = { onRetentionDaysChange(state.retentionDays - 1) },
                        onIncrement = { onRetentionDaysChange(state.retentionDays + 1) },
                        tag = "settings_retention",
                    )
                    Text(
                        text = stringResource(R.string.settings_retention_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    )
                }
            }
        }

        item { SectionHeader(stringResource(R.string.settings_section_exceptions), Modifier.padding(horizontal = 16.dp)) }
        item {
            Text(
                text = stringResource(R.string.settings_exceptions_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 4.dp),
            )
        }
        if (state.rules.isEmpty()) {
            item {
                SoftCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_exceptions_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(16.dp)
                            .testTag("settings_rules_empty"),
                    )
                }
            }
        } else {
            items(state.rules, key = { "${it.rule::class.simpleName}:${it.rule.value}" }) { item ->
                RuleRow(item, onRemove = { onRemoveRule(item.rule) })
            }
        }
        item { AddSenderRow(onAddSender) }
        item {
            OutlinedButton(
                onClick = { showAppPicker = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .heightIn(min = 52.dp)
                    .testTag("settings_add_app"),
            ) { IconLabel(R.drawable.ic_apps, stringResource(R.string.settings_add_app)) }
        }
    }

    if (showAppPicker) {
        AppPickerDialog(
            apps = state.selectableApps,
            onPick = { app ->
                onAddApp(app)
                showAppPicker = false
            },
            onDismiss = { showAppPicker = false },
        )
    }
}

@Composable
private fun RuleRow(item: RuleItem, onRemove: () -> Unit) {
    val removeDescription = stringResource(R.string.settings_remove_rule, item.displayName)
    val scheme = MaterialTheme.colorScheme
    val isApp = item.rule is ExceptionRule.Package
    SoftCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (isApp) {
                InitialAvatar(name = item.displayName)
            } else {
                IconBadge(icon = R.drawable.ic_person, tint = scheme.secondary)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(if (isApp) R.string.settings_rule_type_app else R.string.settings_rule_type_sender),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .semantics { contentDescription = removeDescription }
                    .testTag("settings_remove_${item.rule.value}"),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AddSenderRow(onAddSender: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(stringResource(R.string.settings_sender_label)) },
            leadingIcon = { Icon(painterResource(R.drawable.ic_person), contentDescription = null) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .weight(1f)
                .testTag("settings_sender_input"),
        )
        Button(
            onClick = {
                onAddSender(text)
                text = ""
            },
            enabled = text.isNotBlank(),
            modifier = Modifier
                .heightIn(min = 56.dp)
                .padding(top = 8.dp)
                .testTag("settings_sender_add"),
        ) { Text(stringResource(R.string.settings_add)) }
    }
}

@Composable
private fun AppPickerDialog(
    apps: List<InstalledApp>,
    onPick: (InstalledApp) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(apps, query) {
        if (query.isBlank()) apps else apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_pick_app_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.settings_pick_app_search)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_app_search"),
                )
                if (filtered.isEmpty()) {
                    Text(
                        text = stringResource(R.string.settings_pick_app_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(Modifier.heightIn(max = 320.dp).testTag("settings_app_list")) {
                        items(filtered, key = { it.packageName }) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(app) }
                                    .padding(vertical = 8.dp)
                                    .testTag("settings_app_${app.packageName}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                InitialAvatar(name = app.label, size = 36.dp)
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_close)) }
        },
    )
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun SettingsScreenPreview() {
    PomoriTheme {
        SettingsScreen(
            state = SettingsUiState(
                presetCount = 3,
                selectedPresetName = "Ders",
                retentionDays = 7,
                rules = listOf(
                    RuleItem(ExceptionRule.Package("com.whatsapp"), "WhatsApp"),
                    RuleItem(ExceptionRule.Sender("Anne"), "Anne"),
                ),
            ),
            onRetentionDaysChange = {},
            onAddSender = {},
            onAddApp = {},
            onRemoveRule = {},
            onBack = {},
        )
    }
}
