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
    /** Mod sayısı ve seçili modun adı: ayarlardaki "Modlar" satırında gösterilir. */
    val presetCount: Int = 0,
    val selectedPresetName: String? = null,
    val retentionDays: Int = SettingsDefaults.RETENTION_DAYS,
    /** Günlük odak hedefi, dakika; 0 = kapalı. */
    val dailyGoalMinutes: Int = SettingsDefaults.DAILY_GOAL_MINUTES,
    /** Yakalanan bildirimler seans bitince panele geri getirilsin mi? */
    val restoreNotifications: Boolean = true,
    /** Eklenme sırasıyla. */
    val rules: List<RuleItem> = emptyList(),
    /** Henüz istisna olmayan, yüklü uygulamalar; uygulama adına göre sıralı. */
    val selectableApps: List<InstalledApp> = emptyList(),
)
