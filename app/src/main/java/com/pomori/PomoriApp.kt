package com.pomori

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.pomori.platform.notification.NotificationChannels
import com.pomori.platform.work.CleanupScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/** Uygulama girişi. Hilt'in bağımlılık grafiğini ve WorkManager'ı (Hilt worker fabrikasıyla) başlatır. */
@HiltAndroidApp
class PomoriApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.create(this)
        CleanupScheduler.schedule(this)
    }
}
