package com.pomori.data.repository

import com.pomori.data.local.db.CapturedNotificationEntity
import com.pomori.data.local.db.NotificationDao
import com.pomori.domain.model.CapturedNotification
import com.pomori.domain.model.Distractor
import com.pomori.domain.repository.NotificationRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NotificationRepositoryImpl @Inject constructor(
    private val dao: NotificationDao,
) : NotificationRepository {

    override suspend fun insert(notification: CapturedNotification): Long =
        dao.insert(notification.toEntity())

    override suspend fun deleteOlderThan(olderThan: Long): Int = dao.deleteOlderThan(olderThan)

    override fun observeBySession(sessionId: Long): Flow<List<CapturedNotification>> =
        dao.observeBySession(sessionId).map { rows -> rows.map { it.toDomain() } }

    override fun observeTopDistractors(from: Long, limit: Int): Flow<List<Distractor>> =
        dao.observeTopDistractors(from, limit).map { rows ->
            rows.map { Distractor(packageName = it.packageName, appLabel = it.appLabel, count = it.cnt) }
        }
}

internal fun CapturedNotification.toEntity() = CapturedNotificationEntity(
    id = id,
    sessionId = sessionId,
    packageName = packageName,
    appLabel = appLabel,
    title = title,
    text = text,
    postedAt = postedAt,
)

internal fun CapturedNotificationEntity.toDomain() = CapturedNotification(
    id = id,
    sessionId = sessionId,
    packageName = packageName,
    appLabel = appLabel,
    title = title,
    text = text,
    postedAt = postedAt,
)
