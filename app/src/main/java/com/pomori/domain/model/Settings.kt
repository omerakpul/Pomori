package com.pomori.domain.model

/** Ayarların varsayılanları ve izin verilen aralıkları (docs/data-model.md ile aynı). */
object SettingsDefaults {
    const val FOCUS_MINUTES = 25
    const val BREAK_MINUTES = 5
    const val LONG_BREAK_MINUTES = 15

    /** Her bu kadar tamamlanan odak seansından sonra mola uzun olur (klasik Pomodoro: 4). */
    const val LONG_BREAK_EVERY = 4
    const val RETENTION_DAYS = 7

    val FOCUS_RANGE = 1..120
    val BREAK_RANGE = 1..60
    val LONG_BREAK_RANGE = 1..120
    val RETENTION_RANGE = 1..30
}

/** Seçilebilir bir uygulama (istisna listesi için yüklü uygulamalardan seçerken). */
data class InstalledApp(
    val packageName: String,
    val label: String,
)
