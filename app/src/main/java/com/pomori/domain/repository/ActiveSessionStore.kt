package com.pomori.domain.repository

import com.pomori.domain.model.ActiveSession
import kotlinx.coroutines.flow.Flow

/** Aktif seansın (bitiş zamanı dahil) süreç ölümüne dayanıklı kaydı. */
interface ActiveSessionStore {
    val active: Flow<ActiveSession?>
    suspend fun current(): ActiveSession?
    suspend fun set(session: ActiveSession)
    suspend fun clear()
}
