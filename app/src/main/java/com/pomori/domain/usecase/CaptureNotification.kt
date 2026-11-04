package com.pomori.domain.usecase

import com.pomori.domain.Clock
import com.pomori.domain.digest.IgnoreReason
import com.pomori.domain.digest.IncomingNotification
import com.pomori.domain.digest.NotificationFilter
import com.pomori.domain.model.CapturedNotification
import com.pomori.domain.repository.ActiveSessionStore
import com.pomori.domain.repository.NotificationRepository
import com.pomori.domain.timer.TimerState
import com.pomori.domain.timer.toTimerState
import javax.inject.Inject

sealed interface CaptureResult {
    /** Bildirim kaydedildi; çağıran taraf onu sistemden kaldırabilir. */
    data class Captured(val id: Long) : CaptureResult

    /** Çalışan bir odak seansı yok (boşta, mola ya da süre dolmuş): bildirime dokunulmaz. */
    data object NoActiveFocus : CaptureResult

    /** Filtre elendi: bildirime dokunulmaz. */
    data class Ignored(val reason: IgnoreReason) : CaptureResult
}

/**
 * Odak seansı sürerken gelen bildirimi özet için kaydeder. Bildirimi sistemden kaldırma
 * kararı çağırana kalır: yalnızca [CaptureResult.Captured] dönerse kaldırılmalıdır.
 * Kayıt başarısız olursa istisna fırlar ve bildirim yerinde kalır, yani veri kaybolmaz.
 */
class CaptureNotification @Inject constructor(
    private val activeStore: ActiveSessionStore,
    private val notifications: NotificationRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(incoming: IncomingNotification): CaptureResult {
        val now = clock.now()
        val active = activeStore.current()
        if (active.toTimerState(now) !is TimerState.Focus) return CaptureResult.NoActiveFocus

        NotificationFilter.ignoreReason(incoming)?.let { return CaptureResult.Ignored(it) }

        val id = notifications.insert(
            CapturedNotification(
                sessionId = checkNotNull(active).sessionId,
                packageName = incoming.packageName,
                appLabel = incoming.appLabel,
                title = incoming.title,
                text = incoming.text,
                postedAt = incoming.postedAt,
            ),
        )
        return CaptureResult.Captured(id)
    }
}
