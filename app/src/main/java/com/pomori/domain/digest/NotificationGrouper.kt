package com.pomori.domain.digest

import com.pomori.domain.model.AppDigest
import com.pomori.domain.model.CapturedNotification
import com.pomori.domain.model.SessionDigest

/**
 * Bir seansın bildirimlerini uygulamaya göre gruplar. Saf fonksiyon: Android'e, saate ya da
 * veritabanına bağlı değildir. [items], [sessionId] seansına ait olmalıdır (DAO sorgusu bunu sağlar).
 */
object NotificationGrouper {

    // Önce bildirim sayısı (çoktan aza), eşitlikte uygulama adı (büyük/küçük harf fark etmez),
    // o da eşitse paket adı: aynı girdi her zaman aynı sırayı üretsin.
    private val appOrder = compareByDescending<AppDigest> { it.count }
        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.appLabel }
        .thenBy { it.packageName }

    // En yeni önce; aynı anda gelenlerde sonradan kaydedilen (büyük id) önce, DAO sıralamasıyla aynı.
    private val newestFirst = compareByDescending<CapturedNotification> { it.postedAt }
        .thenByDescending { it.id }

    fun group(sessionId: Long, items: List<CapturedNotification>): SessionDigest {
        val apps = items
            .groupBy { it.packageName }
            .map { (packageName, appItems) ->
                val sorted = appItems.sortedWith(newestFirst)
                AppDigest(
                    packageName = packageName,
                    // Ad yakalama anında çözülür; zamanla değişmişse (örn. paket adına düşmüşken
                    // sonradan çözülebildiyse) en yeni bildirimdeki ad geçerlidir.
                    appLabel = sorted.first().appLabel,
                    items = sorted,
                )
            }
            .sortedWith(appOrder)
        return SessionDigest(sessionId = sessionId, apps = apps)
    }
}
