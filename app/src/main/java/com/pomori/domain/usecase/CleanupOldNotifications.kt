package com.pomori.domain.usecase

import com.pomori.domain.Clock
import com.pomori.domain.repository.NotificationRepository
import com.pomori.domain.repository.SettingsStore
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Saklama süresini (ayarlardaki gün sayısı) aşan yakalanmış bildirimleri siler.
 * Hem günlük periyodik iş hem de saklama süresi değiştirildiğinde hemen çağrılır.
 * Yeni dönemin sınırı dahildir: tam N gün önce gelen bildirim henüz silinmez.
 */
class CleanupOldNotifications @Inject constructor(
    private val settings: SettingsStore,
    private val notifications: NotificationRepository,
    private val clock: Clock,
) {
    /** Silinen bildirim sayısını döner. */
    suspend operator fun invoke(): Int {
        val retentionDays = settings.retentionDays.first()
        val cutoff = clock.now() - retentionDays * DAY_MS
        return notifications.deleteOlderThan(cutoff)
    }

    private companion object {
        const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}
