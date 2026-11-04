package com.pomori.platform.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.pomori.domain.usecase.CaptureNotification
import com.pomori.domain.usecase.CaptureResult
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Odak seansı sırasında gelen bildirimleri yakalar ve sistemden kaldırır.
 * İş mantığı içermez: bildirimi [CaptureNotification]'a verir, yalnızca kaydedilmişse kaldırır.
 *
 * Callback ana iş parçacığında gelir. Room yazımı onu bloklamasın diye kendi
 * `SupervisorJob + Dispatchers.IO` kapsamında yapılır; bir bildirimin hatası diğerlerini etkilemez.
 */
@AndroidEntryPoint
class FocusNotificationListener : NotificationListenerService() {

    @Inject lateinit var captureNotification: CaptureNotification

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val incoming = sbn.toIncoming(applicationContext)
        val key = sbn.key
        scope.launch {
            try {
                // Önce kaydet, sonra kaldır: kayıt başarısız olursa bildirim yerinde kalır.
                if (captureNotification(incoming) is CaptureResult.Captured) {
                    cancelNotification(key)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Bildirim yakalanamadı, olduğu gibi bırakıldı", e)
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "FocusNotificationListener"
    }
}
