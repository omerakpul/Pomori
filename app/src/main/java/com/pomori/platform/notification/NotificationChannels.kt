package com.pomori.platform.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.pomori.R

/** minSdk 26 olduğu için kanallar her zaman mevcuttur (API 26+ zorunlu). */
object NotificationChannels {
    const val TIMER = "timer"
    const val ALERTS = "alerts"

    fun create(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                TIMER,
                context.getString(R.string.channel_timer_name),
                NotificationManager.IMPORTANCE_LOW, // sessiz, kalıcı
            ).apply { description = context.getString(R.string.channel_timer_description) },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                ALERTS,
                context.getString(R.string.channel_alerts_name),
                NotificationManager.IMPORTANCE_HIGH, // ses + başlık bildirimi
            ).apply { description = context.getString(R.string.channel_alerts_description) },
        )
    }
}
