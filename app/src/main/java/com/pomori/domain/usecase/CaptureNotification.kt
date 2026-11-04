package com.pomori.domain.usecase

import com.pomori.domain.Clock
import com.pomori.domain.digest.IgnoreReason
import com.pomori.domain.digest.ExceptionMatcher
import com.pomori.domain.digest.IncomingNotification
import com.pomori.domain.digest.NotificationFilter
import com.pomori.domain.model.CapturedNotification
import com.pomori.domain.repository.ActiveSessionStore
import com.pomori.domain.repository.ExceptionRepository
import com.pomori.domain.repository.NotificationRepository
import com.pomori.domain.timer.TimerState
import com.pomori.domain.timer.toTimerState
import javax.inject.Inject
import kotlinx.coroutines.flow.first

sealed interface CaptureResult {
    /**
     * Bildirim kaydedildi; çağıran taraf onu sistemden kaldırabilir ya da ertelebilir.
     *
     * @property sessionId yakalandığı odak seansı: seans iptal edilirse bildirimleri geri getirmek için.
     * @property sessionEndsAt o seansın planlanan bitişi (epoch ms): erteleme buna kadar sürer.
     */
    data class Captured(val id: Long, val sessionId: Long, val sessionEndsAt: Long) : CaptureResult

    /** Çalışan bir odak seansı yok (boşta, mola ya da süre dolmuş): bildirime dokunulmaz. */
    data object NoActiveFocus : CaptureResult

    /** Filtre elendi: bildirime dokunulmaz. */
    data class Ignored(val reason: IgnoreReason) : CaptureResult
}

/**
 * Odak seansı sürerken gelen bildirimi özet için kaydeder, ama istisna listesindekilere dokunmaz.
 * Bildirimi sistemden kaldırma kararı çağırana kalır: yalnızca [CaptureResult.Captured] dönerse
 * kaldırılmalıdır. Kayıt ya da kural okuması başarısız olursa istisna fırlar ve bildirim yerinde
 * kalır: emin olunamadığında bildirim yakalanmaz, veri ya da bildirim kaybolmaz.
 */
class CaptureNotification @Inject constructor(
    private val activeStore: ActiveSessionStore,
    private val notifications: NotificationRepository,
    private val exceptions: ExceptionRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(incoming: IncomingNotification): CaptureResult {
        val now = clock.now()
        val active = activeStore.current()
        if (active.toTimerState(now) !is TimerState.Focus) return CaptureResult.NoActiveFocus

        NotificationFilter.ignoreReason(incoming)?.let { return CaptureResult.Ignored(it) }

        // Kurallar her bildirimde yeniden okunur: ayarlardan eklenen istisna bir sonraki bildirimden
        // itibaren geçerli olur. Okunamazsa istisna fırlar ve bildirime dokunulmaz.
        val rules = exceptions.observeAll().first()
        if (ExceptionMatcher.isExcepted(incoming.packageName, incoming.title, rules)) {
            return CaptureResult.Ignored(IgnoreReason.EXCEPTED)
        }

        val session = checkNotNull(active)
        val id = notifications.insert(
            CapturedNotification(
                sessionId = session.sessionId,
                packageName = incoming.packageName,
                appLabel = incoming.appLabel,
                title = incoming.title,
                text = incoming.text,
                postedAt = incoming.postedAt,
            ),
        )
        return CaptureResult.Captured(id, sessionId = session.sessionId, sessionEndsAt = session.endAt)
    }
}
