package com.pomori.domain.repository

import com.pomori.domain.model.FocusSession
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    /** Yeni seansı kaydeder, üretilen id'yi döner. */
    suspend fun insert(session: FocusSession): Long
    suspend fun update(session: FocusSession)
    /** Çalışan seansın kesinti sayacını bir artırır; seans çalışmıyorsa false döner. */
    suspend fun recordInterruption(id: Long): Boolean
    /** Seansın kesinti sayısını canlı izler (seans yoksa 0). */
    fun observeInterruptions(id: Long): Flow<Int>
    suspend fun getById(id: Long): FocusSession?

    /** `from`'dan (epoch ms) beri başlamış, tamamlanan odak seansları; yeniden eskiye sıralı. */
    fun observeCompletedFocusSince(from: Long): Flow<List<FocusSession>>
}
