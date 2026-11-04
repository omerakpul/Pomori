package com.pomori.domain.repository

import com.pomori.domain.model.FocusSession
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    /** Yeni seansı kaydeder, üretilen id'yi döner. */
    suspend fun insert(session: FocusSession): Long
    suspend fun update(session: FocusSession)
    suspend fun getById(id: Long): FocusSession?

    /** `from`'dan (epoch ms) beri başlamış, tamamlanan odak seansları; yeniden eskiye sıralı. */
    fun observeCompletedFocusSince(from: Long): Flow<List<FocusSession>>
}
