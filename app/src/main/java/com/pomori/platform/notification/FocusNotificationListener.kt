package com.pomori.platform.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.pomori.domain.Clock
import com.pomori.domain.repository.ActiveSessionStore
import com.pomori.domain.timer.FocusSessionEndDetector
import com.pomori.domain.timer.TimerState
import com.pomori.domain.timer.toTimerState
import com.pomori.domain.usecase.CaptureNotification
import com.pomori.domain.usecase.CaptureResult
import com.pomori.domain.usecase.ChooseNotificationDisposition
import com.pomori.domain.usecase.NotificationDisposition
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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
    @Inject lateinit var chooseDisposition: ChooseNotificationDisposition
    @Inject lateinit var ledger: SnoozeLedger
    @Inject lateinit var clock: Clock
    @Inject lateinit var activeSessionStore: ActiveSessionStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessionEnds = FocusSessionEndDetector()
    private var watchJob: Job? = null

    /**
     * Servis bağlanınca çalışan odak seansını izlemeye başlar. Seans iptal edilince (ya da başka seansa geçilince)
     * o seansta ertelenen bildirimler, planlanan bitişi beklemeden panele hemen geri getirilir.
     */
    override fun onListenerConnected() {
        watchJob?.cancel()
        watchJob = scope.launch {
            activeSessionStore.active.collect { active ->
                val now = clock.now()
                val runningFocus = active?.takeIf { it.toTimerState(now) is TimerState.Focus }?.sessionId
                sessionEnds.onFocusSessionChanged(runningFocus)?.let { ended -> returnToShade(ended) }
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val incoming = sbn.toIncoming(applicationContext)
        val key = sbn.key
        // Pomori'nin ertelediği bir bildirim süresi dolup geri geldi: dokunma, yeniden yakalama.
        if (ledger.consume(key)) return
        scope.launch {
            try {
                // Önce kaydet, sonra sistemden al: kayıt başarısız olursa bildirim yerinde kalır.
                val result = captureNotification(incoming)
                if (result is CaptureResult.Captured) takeFromShade(key, result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Bildirim yakalanamadı, olduğu gibi bırakıldı", e)
            }
        }
    }

    /**
     * Yakalanan bildirimi panelden alır. Varsayılan: seans bitişine kadar ertele (süre dolunca sistem orijinali
     * kendi eylemleriyle geri koyar). Ertelenemezse bildirim yerinde bırakılır: görünür kalması, kaybolmasından iyi.
     */
    private suspend fun takeFromShade(key: String, result: CaptureResult.Captured) {
        val now = clock.now()
        when (val choice = chooseDisposition(result.sessionEndsAt, now)) {
            NotificationDisposition.Remove -> cancelNotification(key)
            is NotificationDisposition.Snooze -> {
                // Önce deftere yaz: dönen bildirim, snooze çağrısı dönmeden bile gelebilir.
                ledger.remember(key, result.sessionId, choice.returnsAt, now)
                try {
                    snoozeNotification(key, choice.durationMs)
                } catch (e: Exception) {
                    ledger.consume(key)
                    Log.w(TAG, "Bildirim ertelenemedi, yerinde bırakıldı", e)
                }
            }
        }
    }

    /**
     * Bitmiş seansta ertelenen bildirimleri kısa bir erteleme ile hemen geri getirir. Sistem, ertelenmiş bir
     * bildirimin süresinin kısaltılmasına izin verir; Android uygulamalara ayrı bir "ertelemeyi kaldır" vermez.
     * Defterde kalan her anahtar dönüşte yine tanınır, yani geri gelen bildirim yeni bir seansa yeniden yakalanmaz.
     */
    private fun returnToShade(sessionId: Long) {
        val keys = ledger.keysOf(sessionId)
        if (keys.isEmpty()) return
        val returnsAt = clock.now() + RETURN_DELAY_MS
        keys.forEach { key ->
            try {
                ledger.reschedule(key, returnsAt)
                snoozeNotification(key, RETURN_DELAY_MS)
            } catch (e: Exception) {
                // Bildirim kullanıcı tarafından kapatılmış ya da uygulama tarafından iptal edilmiş olabilir.
                Log.w(TAG, "Bildirim geri getirilemedi", e)
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "FocusNotificationListener"

        /** İptalde bildirimin geri gelmesi için kısa erteleme: panele neredeyse anında düşer. */
        const val RETURN_DELAY_MS = 2_000L
    }
}
