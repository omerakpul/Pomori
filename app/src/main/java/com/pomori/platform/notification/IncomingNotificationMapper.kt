package com.pomori.platform.notification

import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.service.notification.StatusBarNotification
import com.pomori.domain.digest.IncomingNotification

/** Android'e özgü `StatusBarNotification`'ı, saf Kotlin filtresinin anladığı modele çevirir. */
fun StatusBarNotification.toIncoming(context: Context): IncomingNotification =
    notification.toIncoming(context, packageName = packageName, postTime = postTime, isOngoing = isOngoing)

/**
 * Asıl eşleme, `StatusBarNotification` yerine düz bir [Notification] üzerinde çalışır: böylece gerçek
 * `Notification` nesneleriyle cihazda test edilebilir (StatusBarNotification'ı elle üretmek mümkün değil).
 */
internal fun Notification.toIncoming(
    context: Context,
    packageName: String,
    postTime: Long,
    isOngoing: Boolean,
): IncomingNotification = IncomingNotification(
    packageName = packageName,
    appLabel = resolveAppLabel(context.packageManager, packageName),
    title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
    text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
    postedAt = postTime,
    isOngoing = isOngoing || flags and Notification.FLAG_FOREGROUND_SERVICE != 0,
    isGroupSummary = flags and Notification.FLAG_GROUP_SUMMARY != 0,
    isOwnApp = packageName == context.packageName,
    category = category,
    hasFullScreenIntent = fullScreenIntent != null,
    isInsistent = flags and Notification.FLAG_INSISTENT != 0,
)

/**
 * Uygulama adını çözer. Android 11+ paket görünürlüğü yüzünden başka bir uygulamanın bilgisine
 * erişilemezse (`<queries>` kapsamı dışında) çökmek yerine paket adına düşer.
 */
private fun resolveAppLabel(packageManager: PackageManager, packageName: String): String = try {
    packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
} catch (_: PackageManager.NameNotFoundException) {
    packageName
}
