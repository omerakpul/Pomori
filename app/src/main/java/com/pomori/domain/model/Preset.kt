package com.pomori.domain.model

/**
 * Adlandırılmış bir süre seti ("Ders" 40/5/15, "Kodlama" 25/5/15...). Her mod kendi odak, mola ve uzun mola
 * süresini ve uzun molanın kaç odak seansında bir geleceğini taşır.
 */
data class Preset(
    val id: Long = 0,
    val name: String,
    val focusMinutes: Int,
    val breakMinutes: Int,
    val longBreakMinutes: Int,
    /** Her bu kadar tamamlanan odak seansından sonra mola uzun olur. */
    val longBreakEvery: Int = SettingsDefaults.LONG_BREAK_EVERY,
) {
    /**
     * Adı kırpılmış ve süreleri geçerli aralıklara çekilmiş hâli. Depo kaydederken bunu uygular: ekran sınırda
     * düğmeyi kapatsa bile bozuk bir değer diske ulaşamaz.
     */
    fun normalized(): Preset = copy(
        name = name.trim().take(NAME_MAX_LENGTH),
        focusMinutes = focusMinutes.coerceIn(SettingsDefaults.FOCUS_RANGE),
        breakMinutes = breakMinutes.coerceIn(SettingsDefaults.BREAK_RANGE),
        longBreakMinutes = longBreakMinutes.coerceIn(SettingsDefaults.LONG_BREAK_RANGE),
        longBreakEvery = longBreakEvery.coerceIn(SettingsDefaults.LONG_BREAK_EVERY_RANGE),
    )

    companion object {
        const val NAME_MAX_LENGTH = 24

        /** İlk açılışta, eski tek-ayar süreleriyle oluşturulan mod. */
        const val DEFAULT_NAME = "Varsayılan"

        /** Yeni mod eklerken editörün başlangıç değerleri. */
        fun blank() = Preset(
            name = "",
            focusMinutes = SettingsDefaults.FOCUS_MINUTES,
            breakMinutes = SettingsDefaults.BREAK_MINUTES,
            longBreakMinutes = SettingsDefaults.LONG_BREAK_MINUTES,
        )
    }
}
