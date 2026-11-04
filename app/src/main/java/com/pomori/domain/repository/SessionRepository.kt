package com.pomori.domain.repository

import com.pomori.domain.model.FocusSession

interface SessionRepository {
    /** Yeni seansı kaydeder, üretilen id'yi döner. */
    suspend fun insert(session: FocusSession): Long
    suspend fun update(session: FocusSession)
    suspend fun getById(id: Long): FocusSession?
}
