package com.pomori.platform.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Eski bildirimleri silen günlük işi zamanlar. */
object CleanupScheduler {
    const val UNIQUE_NAME = "cleanup_old_notifications"

    /**
     * Her uygulama açılışında çağrılabilir: iş zaten kuyruktaysa KEEP politikası onu yerinde bırakır,
     * yani zamanlayıcı sıfırlanmaz.
     */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<CleanupWorker>(1, TimeUnit.DAYS)
            // Pil azken ertelenebilir: acil bir iş değil.
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
