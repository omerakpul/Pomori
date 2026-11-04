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
    /** Uygulamanın bildirime verdiği kategori (`Notification.category`), yoksa `null`. */
    val category: String? = null,
    /** Tam ekran niyeti olan bildirim: gelen arama, çalan alarm gibi hemen ilgilenilmesi gerekenler. */
    val hasFullScreenIntent: Boolean = false,
    /** Kullanıcı kapatana kadar sesi tekrarlayan bildirim (`FLAG_INSISTENT`): çalan arama ya da alarm. */
    val isInsistent: Boolean = false,
)

enum class IgnoreReason {
    ONGOING,
    GROUP_SUMMARY,
    OWN_APP,

    /**
     * Zamana duyarlı: gelen arama, alarm, hatırlatma, takvim etkinliği, yol tarifi gibi, kaçırılırsa
     * zarar veren bildirimler. Yakalanıp gizlenmez, olduğu gibi gelir.
     */
    TIME_SENSITIVE,

    /** Kullanıcının istisna listesine takıldı: olduğu gibi gelmeye devam eder. */
    EXCEPTED,
}

object NotificationFilter {

    /**
     * Bildirim yakalanmamalıysa nedenini, yakalanabilirse `null` döner.
     * Kendi uygulamamız en başta elenir: sayaç bildirimini yakalayıp silmek seansı bozardı.
     */
    fun ignoreReason(notification: IncomingNotification): IgnoreReason? = when {
        notification.isOwnApp -> IgnoreReason.OWN_APP
        notification.isOngoing -> IgnoreReason.ONGOING
        notification.isGroupSummary -> IgnoreReason.GROUP_SUMMARY
        isTimeSensitive(notification) -> IgnoreReason.TIME_SENSITIVE
        else -> null
    }

    /**
     * Odak sırasında bile görülmesi gereken bildirimler. Üç işaretten biri yeter, çünkü uygulamalar
     * kategoriyi her zaman doldurmaz: tam ekran niyeti ve ısrarlı ses (çalan telefon/alarm) kategori
     * olmasa da yakalar.
     */
    private fun isTimeSensitive(n: IncomingNotification): Boolean =
        n.hasFullScreenIntent || n.isInsistent || n.category in TIME_SENSITIVE_CATEGORIES

    /**
     * `Notification.CATEGORY_*` değerleri (dize olarak: bazıları yeni Android sürümlerinde eklendi).
     * Mesaj, e-posta, sosyal ve tanıtım kategorileri bilerek listede yok: özetin asıl konusu onlar.
     * Cevapsız arama da yok, zaten kaçırılmış bir bilgi; özette görünmesi doğru.
     */
    private val TIME_SENSITIVE_CATEGORIES = setOf(
        "call", // gelen/süren arama
        "alarm", // alarm, zamanlayıcı
        "stopwatch",
        "reminder",
        "event", // takvim etkinliği
        "navigation",
        "location_sharing",
        "workout",
        "transport", // medya denetimleri
        "progress", // indirme/yükleme ilerlemesi
        "service",
        "status",
        "system",
        "error",
    )
}
