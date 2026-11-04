package com.pomori.data.repository

import com.pomori.data.local.db.FocusSessionEntity
import com.pomori.data.local.db.SessionDao
import com.pomori.domain.model.FocusSession
import com.pomori.domain.repository.SessionRepository
import javax.inject.Inject

class SessionRepositoryImpl @Inject constructor(
    private val dao: SessionDao,
) : SessionRepository {

    override suspend fun insert(session: FocusSession): Long = dao.insert(session.toEntity())

    override suspend fun update(session: FocusSession) = dao.update(session.toEntity())

    override suspend fun getById(id: Long): FocusSession? = dao.getById(id)?.toDomain()
}

internal fun FocusSession.toEntity() = FocusSessionEntity(
    id = id,
    type = type,
    startedAt = startedAt,
    plannedDurationMs = plannedDurationMs,
    endedAt = endedAt,
    status = status,
)

internal fun FocusSessionEntity.toDomain() = FocusSession(
    id = id,
    type = type,
    startedAt = startedAt,
    plannedDurationMs = plannedDurationMs,
    endedAt = endedAt,
    status = status,
)
