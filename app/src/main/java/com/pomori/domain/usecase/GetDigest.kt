package com.pomori.domain.usecase

import com.pomori.domain.digest.NotificationGrouper
import com.pomori.domain.model.SessionDigest
import com.pomori.domain.repository.NotificationRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Bir seansın bildirim özetini verir; seansa yeni bildirim yazıldıkça yeniden yayınlar. */
class GetDigest @Inject constructor(
    private val notifications: NotificationRepository,
) {
    operator fun invoke(sessionId: Long): Flow<SessionDigest> =
        notifications.observeBySession(sessionId).map { NotificationGrouper.group(sessionId, it) }
}
