package com.pomori.ui.settings

import com.pomori.domain.model.ExceptionRule
import com.pomori.domain.model.InstalledApp
import com.pomori.domain.model.SettingsDefaults

/** Listedeki bir istisna kuralı ve ekranda gösterilecek adı (uygulama kuralında uygulama adı). */
data class RuleItem(
    val rule: ExceptionRule,
    val displayName: String,
)

data class SettingsUiState(
    val focusMinutes: Int = SettingsDefaults.FOCUS_MINUTES,
    val breakMinutes: Int = SettingsDefaults.BREAK_MINUTES,
    val retentionDays: Int = SettingsDefaults.RETENTION_DAYS,
    /** Eklenme sırasıyla. */
    val rules: List<RuleItem> = emptyList(),
    /** Henüz istisna olmayan, yüklü uygulamalar; uygulama adına göre sıralı. */
    val selectableApps: List<InstalledApp> = emptyList(),
)
