package com.pomori.platform.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.pomori.domain.Clock
import com.pomori.domain.repository.ActiveSessionStore
import com.pomori.domain.timer.TimerState
import com.pomori.domain.timer.toTimerState
import com.pomori.domain.usecase.EndReason
import com.pomori.domain.usecase.EndSession
import com.pomori.platform.notification.FocusNotifications
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Seans boyunca süreci canlı tutar ve kalıcı geri sayım bildirimini gösterir.
 * İş mantığı içermez: durumu DataStore'dan okur, süre dolunca EndSession'ı çağırır.
 *
 * Android 14+ (API 34): foreground service türü zorunlu. Uygulama pomodoro sayacı için
 * hiçbir hazır türe uymadığından `specialUse` kullanılır (manifestte gerekçesiyle).
 */
@AndroidEntryPoint
class FocusForegroundService : Service() {

    @Inject lateinit var activeSessionStore: ActiveSessionStore
    @Inject lateinit var endSession: EndSession
    @Inject lateinit var clock: Clock

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Sistem, başlatmadan sonra kısa sürede startForeground bekler: önce placeholder göster.
        startInForeground(FocusNotifications.starting(this))

        job?.cancel()
        job = scope.launch { observeSession() }

        // Süreç sistem tarafından öldürülüp servis yeniden başlatılırsa intent null gelir;
        // durum yine DataStore'dan okunduğu için sayaç kaldığı yerden devam eder.
        return START_STICKY
    }

    private suspend fun observeSession() {
        activeSessionStore.active.collectLatest { active ->
            val now = clock.now()
            when (val state = active.toTimerState(now)) {
                is TimerState.Focus -> {
                    show(FocusNotifications.running(this, active!!.type, state.endAt))
                    delay((state.endAt - now).coerceAtLeast(0))
                    endSession(EndReason.TIME_UP)
                    FocusNotifications.notifyAlert(this, FocusNotifications.focusDone(this))
                    finish()
                }

                is TimerState.Break -> {
                    show(FocusNotifications.running(this, active!!.type, state.endAt))
                    delay((state.endAt - now).coerceAtLeast(0))
                    endSession(EndReason.TIME_UP)
                    FocusNotifications.notifyAlert(this, FocusNotifications.breakDone(this))
                    finish()
                }

                // Servis yeniden başlatıldığında süre zaten dolmuş olabilir: kaydı kapat, sustur.
                TimerState.BreakPending -> {
                    endSession(EndReason.TIME_UP)
                    finish()
                }

                TimerState.Idle -> {
                    if (active != null) endSession(EndReason.TIME_UP)
                    finish()
                }
            }
        }
    }

    private fun show(notification: android.app.Notification) = startInForeground(notification)

    private fun startInForeground(notification: android.app.Notification) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, FocusNotifications.ONGOING_ID, notification, type)
    }

    private fun finish() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, FocusForegroundService::class.java))
        }
    }
}
