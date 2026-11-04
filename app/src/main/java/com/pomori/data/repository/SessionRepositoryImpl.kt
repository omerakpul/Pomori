package com.pomori.data.repository

import com.pomori.data.local.db.FocusSessionEntity
import com.pomori.data.local.db.SessionDao
import com.pomori.domain.model.FocusSession
import com.pomori.domain.repository.SessionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SessionRepositoryImpl @Inject constructor(
    private val dao: SessionDao,
) : SessionRepository {

    override suspend fun insert(session: FocusSession): Long = dao.insert(session.toEntity())

    override suspend fun update(session: FocusSession) = dao.update(session.toEntity())

    override suspend fun recordInterruption(id: Long): Boolean = dao.incrementInterruptions(id) > 0

    override fun observeInterruptions(id: Long): Flow<Int> = dao.observeInterruptions(id).map { it ?: 0 }

    override suspend fun getById(id: Long): FocusSession? = dao.getById(id)?.toDomain()

    override fun observeCompletedFocusSince(from: Long): Flow<List<FocusSession>> =
        dao.observeCompletedSince(from).map { rows -> rows.map { it.toDomain() } }
}

internal fun FocusSession.toEntity() = FocusSessionEntity(
    id = id,
    type = type,
    startedAt = startedAt,
    plannedDurationMs = plannedDurationMs,
    endedAt = endedAt,
    status = status,
    presetId = presetId,
    interruptions = interruptions,
)

internal fun FocusSessionEntity.toDomain() = FocusSession(
    id = id,
    type = type,
    startedAt = startedAt,
    plannedDurationMs = plannedDurationMs,
    endedAt = endedAt,
    status = status,
    presetId = presetId,
    interruptions = interruptions,
)
