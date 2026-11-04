package com.pomori

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.pomori.domain.usecase.EnsureDefaultPreset
import com.pomori.platform.notification.NotificationChannels
import com.pomori.platform.work.CleanupScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Uygulama girişi. Hilt'in bağımlılık grafiğini ve WorkManager'ı (Hilt worker fabrikasıyla) başlatır. */
@HiltAndroidApp
class PomoriApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var ensureDefaultPreset: EnsureDefaultPreset

    /** Uygulama ömrü boyunca yaşayan kapsam: açılışta bir kez çalışan kısa işler için. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.create(this)
        CleanupScheduler.schedule(this)
        // İlk açılışta (ya da modlardan önceki bir sürümden güncellemede) "Varsayılan" modu eski süreleriyle oluştur.
        appScope.launch { ensureDefaultPreset() }
    }
}
