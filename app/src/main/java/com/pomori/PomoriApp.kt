package com.pomori

import android.app.Application
import com.pomori.platform.notification.NotificationChannels
import dagger.hilt.android.HiltAndroidApp

/** Uygulama girişi. Hilt'in bağımlılık grafiğini başlatır. */
@HiltAndroidApp
class PomoriApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationChannels.create(this)
    }
}
