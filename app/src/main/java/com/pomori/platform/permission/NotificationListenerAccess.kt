package com.pomori.platform.permission

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/**
 * Bildirim erişimi (NotificationListenerService izni), POST_NOTIFICATIONS'tan farklı olarak
 * çalışma zamanı penceresiyle istenemez: kullanıcı sistem ayarlarındaki "Bildirim erişimi"
 * listesinden uygulamayı kendisi açmak zorundadır. Bu sınıf durumu okur ve o ekranı açar.
 */
object NotificationListenerAccess {

    fun isGranted(context: Context): Boolean =
        context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

    /**
     * Sistemin "Bildirim erişimi" listesini açar. Bu ekranı olmayan nadir cihazlarda
     * `false` döner, çağıran taraf çökmek yerine kullanıcıya yol gösterebilir.
     *
     * Not: Uygulama bu listede yalnızca manifest'te bir NotificationListenerService
     * tanımlıysa görünür (FocusNotificationListener sonraki adımda eklenecek).
     */
    fun openSettings(context: Context): Boolean = try {
        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
