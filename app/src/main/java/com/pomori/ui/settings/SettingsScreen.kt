package com.pomori.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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
import com.pomori.domain.model.ExceptionRule
import com.pomori.domain.model.InstalledApp
import com.pomori.domain.model.SettingsDefaults
import com.pomori.ui.theme.PomoriTheme

/** Odak süresi 5'er dakika, mola ve saklama süresi 1'er birim değişir. */
private const val FOCUS_STEP = 5

/** Erişilebilirlik için dokunma hedefi en az 48 dp olmalıdır. */
private val TOUCH_TARGET = 48.dp

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onFocusMinutesChange = viewModel::setFocusMinutes,
        onBreakMinutesChange = viewModel::setBreakMinutes,
        onRetentionDaysChange = viewModel::setRetentionDays,
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
    onFocusMinutesChange: (Int) -> Unit,
    onBreakMinutesChange: (Int) -> Unit,
    onRetentionDaysChange: (Int) -> Unit,
    onAddSender: (String) -> Unit,
    onAddApp: (InstalledApp) -> Unit,
    onRemoveRule: (ExceptionRule) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAppPicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack, modifier = Modifier.testTag("settings_back")) {
                    Text(stringResource(R.string.settings_back))
                }
            }
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
        }

        item { SectionTitle(R.string.settings_section_durations) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = 4.dp)) {
                    StepperRow(
                        label = stringResource(R.string.settings_focus_duration),
                        valueText = stringResource(R.string.settings_minutes_format, state.focusMinutes),
                        canDecrement = state.focusMinutes > SettingsDefaults.FOCUS_RANGE.first,
                        canIncrement = state.focusMinutes < SettingsDefaults.FOCUS_RANGE.last,
                        onDecrement = { onFocusMinutesChange(state.focusMinutes - FOCUS_STEP) },
                        onIncrement = { onFocusMinutesChange(state.focusMinutes + FOCUS_STEP) },
                        tag = "settings_focus",
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    StepperRow(
                        label = stringResource(R.string.settings_break_duration),
                        valueText = stringResource(R.string.settings_minutes_format, state.breakMinutes),
                        canDecrement = state.breakMinutes > SettingsDefaults.BREAK_RANGE.first,
                        canIncrement = state.breakMinutes < SettingsDefaults.BREAK_RANGE.last,
                        onDecrement = { onBreakMinutesChange(state.breakMinutes - 1) },
                        onIncrement = { onBreakMinutesChange(state.breakMinutes + 1) },
                        tag = "settings_break",
                    )
                }
            }
        }

        item { SectionTitle(R.string.settings_section_retention) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = 4.dp)) {
                    StepperRow(
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    )
                }
            }
        }

        item { SectionTitle(R.string.settings_section_exceptions) }
        item {
            Text(
                text = stringResource(R.string.settings_exceptions_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.rules.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.settings_exceptions_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag("settings_rules_empty"),
                )
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
                    .testTag("settings_add_app"),
            ) { Text(stringResource(R.string.settings_add_app)) }
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
private fun SectionTitle(titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(top = 8.dp)
            .semantics { heading() },
    )
}

@Composable
private fun StepperRow(
    label: String,
    valueText: String,
    canDecrement: Boolean,
    canIncrement: Boolean,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    tag: String,
) {
    val decreaseDescription = stringResource(R.string.settings_decrease, label)
    val increaseDescription = stringResource(R.string.settings_increase, label)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        OutlinedButton(
            onClick = onDecrement,
            enabled = canDecrement,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .size(TOUCH_TARGET)
                .semantics { contentDescription = decreaseDescription }
                .testTag("${tag}_dec"),
        ) { Text("−", style = MaterialTheme.typography.titleLarge) }
        Text(
            text = valueText,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            // Tek satır: büyük yazı tipinde "25 dk" ikiye bölünmez, bunun yerine etiket daralır.
            maxLines = 1,
            softWrap = false,
            // En az genişlik: normal boyutta "5 dk" ve "25 dk" aynı yeri kaplar, düğmeler satırlar arasında
            // hizalı kalır; büyük yazı tipinde değer gerektiği kadar genişler.
            modifier = Modifier
                .widthIn(min = 64.dp)
                .testTag("${tag}_value"),
        )
        OutlinedButton(
            onClick = onIncrement,
            enabled = canIncrement,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .size(TOUCH_TARGET)
                .semantics { contentDescription = increaseDescription }
                .testTag("${tag}_inc"),
        ) { Text("+", style = MaterialTheme.typography.titleLarge) }
    }
}

@Composable
private fun RuleRow(item: RuleItem, onRemove: () -> Unit) {
    val removeDescription = stringResource(R.string.settings_remove_rule, item.displayName)
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        when (item.rule) {
                            is ExceptionRule.Package -> R.string.settings_rule_type_app
                            is ExceptionRule.Sender -> R.string.settings_rule_type_sender
                        },
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(
                onClick = onRemove,
                modifier = Modifier
                    .semantics { contentDescription = removeDescription }
                    .testTag("settings_remove_${item.rule.value}"),
            ) { Text(stringResource(R.string.settings_remove)) }
        }
    }
}

@Composable
private fun AddSenderRow(onAddSender: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(stringResource(R.string.settings_sender_label)) },
            singleLine = true,
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
            modifier = Modifier.testTag("settings_sender_add"),
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
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(app) }
                                    .padding(vertical = 12.dp)
                                    .testTag("settings_app_${app.packageName}"),
                            )
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

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    PomoriTheme {
        SettingsScreen(
            state = SettingsUiState(
                focusMinutes = 25,
                breakMinutes = 5,
                retentionDays = 7,
                rules = listOf(
                    RuleItem(ExceptionRule.Package("com.whatsapp"), "WhatsApp"),
                    RuleItem(ExceptionRule.Sender("Anne"), "Anne"),
                ),
            ),
            onFocusMinutesChange = {},
            onBreakMinutesChange = {},
            onRetentionDaysChange = {},
            onAddSender = {},
            onAddApp = {},
            onRemoveRule = {},
            onBack = {},
        )
    }
}
