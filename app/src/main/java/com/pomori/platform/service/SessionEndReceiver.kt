package com.pomori.platform.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pomori.domain.Clock
import com.pomori.domain.repository.ActiveSessionStore
import com.pomori.domain.timer.TimerState
import com.pomori.domain.timer.toTimerState
import com.pomori.domain.usecase.EndReason
import com.pomori.domain.usecase.EndSession
import com.pomori.platform.locale.AppLocale
import com.pomori.platform.notification.FocusNotifications
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * [SessionEndAlarm] çaldığında seansı kapatır ve "bitti" bildirimini gönderir. Servisle aynı işi yapar; ikisinden
 * hangisi önce çalışırsa çalışsın bildirim yalnızca bir kez ses çıkarır ([FocusNotifications.alertOnce]).
 */
@AndroidEntryPoint
class SessionEndReceiver : BroadcastReceiver() {

    @Inject lateinit var activeSessionStore: ActiveSessionStore
    @Inject lateinit var endSession: EndSession
    @Inject lateinit var clock: Clock

    override fun onReceive(base: Context, intent: Intent) {
        val context = AppLocale.wrap(base)
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val active = activeSessionStore.current() ?: return@launch
                when (active.toTimerState(clock.now())) {
                    // Odak bitti: kayıt, mola başlatılana/atlanana kadar BreakPending olarak kalır.
                    TimerState.BreakPending -> {
                        endSession(EndReason.TIME_UP)
                        FocusNotifications.alertOnce(context, active.sessionId, FocusNotifications.focusDone(context))
                    }
                    // Mola bitti.
                    TimerState.Idle -> {
                        endSession(EndReason.TIME_UP)
                        FocusNotifications.alertOnce(context, active.sessionId, FocusNotifications.breakDone(context))
                    }
                    else -> return@launch // Süre henüz dolmadı (alarm eski bir seansa aitti).
                }
                // Servisin kalıcı geri sayım bildirimi bayat kalmasın.
                context.stopService(Intent(context, FocusForegroundService::class.java))
            } finally {
                pending.finish()
            }
        }
    }
}
