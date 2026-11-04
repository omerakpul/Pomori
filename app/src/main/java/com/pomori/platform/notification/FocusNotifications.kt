package com.pomori.platform.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.pomori.MainActivity
import com.pomori.R
import com.pomori.domain.model.SessionType

/** Servisin kullandığı bildirimleri üretir. İş mantığı içermez. */
object FocusNotifications {
    const val ONGOING_ID = 1
    const val ALERT_ID = 2

    /** Placeholder: startForeground 5 sn içinde çağrılmalı, durum henüz okunmadan önce bile. */
    fun starting(context: Context): Notification = base(context, NotificationChannels.TIMER)
        .setContentTitle(context.getString(R.string.app_name))
        .setOngoing(true)
        .build()

    /**
     * Sistem geri sayımı kendisi gösterir (chronometer, bitiş zamanına göre).
     * Servis her saniye bildirimi güncellemek zorunda kalmaz; pil dostudur.
     */
    fun running(context: Context, type: SessionType, endAt: Long, isLongBreak: Boolean = false): Notification {
        val title = when (type) {
            SessionType.FOCUS -> R.string.notification_focus_title
            SessionType.BREAK ->
                if (isLongBreak) R.string.notification_long_break_title else R.string.notification_break_title
        }
        return base(context, NotificationChannels.TIMER)
            .setContentTitle(context.getString(title))
            .setContentText(context.getString(R.string.notification_running_text))
            .setWhen(endAt)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    fun focusDone(context: Context): Notification = alert(
        context,
        R.string.notification_focus_done_title,
        R.string.notification_focus_done_text,
    )

    fun breakDone(context: Context): Notification = alert(
        context,
        R.string.notification_break_done_title,
        R.string.notification_break_done_text,
    )

    fun notifyAlert(context: Context, notification: Notification) {
        context.getSystemService(NotificationManager::class.java).notify(ALERT_ID, notification)
    }

    private fun alert(context: Context, title: Int, text: Int): Notification =
        base(context, NotificationChannels.ALERTS)
            .setContentTitle(context.getString(title))
            .setContentText(context.getString(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()

    private fun base(context: Context, channelId: String): NotificationCompat.Builder =
        NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentIntent(openAppIntent(context))

    private fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
