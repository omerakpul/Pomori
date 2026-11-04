package com.pomori.domain.digest

/**
 * Sistemden gelen bildirimin, kararın verilmesi için gereken alanları. Android'e özgü
 * `StatusBarNotification` platform katmanında bu modele çevrilir; böylece filtre JVM'de test edilir.
 */
data class IncomingNotification(
    val packageName: String,
    /** Yakalama anında çözülür; çözülemezse paket adı kullanılır. */
    val appLabel: String,
    val title: String?,
    val text: String?,
    val postedAt: Long,
    /** Kalıcı bildirim (müzik oynatıcı, devam eden arama, foreground service vb.). */
    val isOngoing: Boolean,
    /** Gruplanmış bildirimlerin özet satırı; çocukları zaten ayrı ayrı gelir. */
    val isGroupSummary: Boolean,
    /** Pomori'nin kendi bildirimleri (sayaç, seans bitişi). */
    val isOwnApp: Boolean,
)

enum class IgnoreReason { ONGOING, GROUP_SUMMARY, OWN_APP }

object NotificationFilter {

    /**
     * Bildirim yakalanmamalıysa nedenini, yakalanabilirse `null` döner.
     * Kendi uygulamamız en başta elenir: sayaç bildirimini yakalayıp silmek seansı bozardı.
     */
    fun ignoreReason(notification: IncomingNotification): IgnoreReason? = when {
        notification.isOwnApp -> IgnoreReason.OWN_APP
        notification.isOngoing -> IgnoreReason.ONGOING
        notification.isGroupSummary -> IgnoreReason.GROUP_SUMMARY
        else -> null
    }
}
