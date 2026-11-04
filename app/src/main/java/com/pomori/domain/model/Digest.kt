package com.pomori.domain.model

/** Bir uygulamanın seans sırasında yakalanan bildirimleri. */
data class AppDigest(
    val packageName: String,
    val appLabel: String,
    /** En yeniden eskiye sıralı. */
    val items: List<CapturedNotification>,
) {
    /** Listeden türetilir; böylece sayı ile liste birbirinden ayrışamaz. */
    val count: Int get() = items.size
}

/** Mola başlayınca gösterilen özet. */
data class SessionDigest(
    val sessionId: Long,
    /** Bildirim sayısına göre azalan sıralı. */
    val apps: List<AppDigest>,
) {
    val total: Int get() = apps.sumOf { it.count }
}
