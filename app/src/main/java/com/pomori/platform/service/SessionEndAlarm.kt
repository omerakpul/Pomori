package com.pomori.platform.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Seans bitişine sistem alarmı kurar. Servisin kendi `delay`'i telefon uykudayken (ekran kilitli, işlemci askıda)
 * ilerlemez; bildirim telefon uyanana kadar gecikebilirdi. Alarm ise cihazı tam zamanında uyandırır.
 */
object SessionEndAlarm {
    private const val REQUEST_CODE = 1
    private const val TAG = "SessionEndAlarm"

    fun schedule(context: Context, endAtMs: Long) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val pending = pendingIntent(context)
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
        try {
            if (exactAllowed) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAtMs, pending)
            } else {
                // Tam alarm izni yoksa (kullanıcı geri almış olabilir) yine de uyandıran, biraz esnek bir alarm kur.
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAtMs, pending)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Alarm kurulamadı; servisin kendi zamanlayıcısına güveniliyor", e)
        }
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, SessionEndReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
