package com.pomori.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: FocusSessionEntity): Long

    @Update
    suspend fun update(session: FocusSessionEntity)

    @Query("SELECT * FROM focus_sessions WHERE id = :id")
    suspend fun getById(id: Long): FocusSessionEntity?

    @Query(
        """
        SELECT * FROM focus_sessions
        WHERE type = 'FOCUS' AND status = 'COMPLETED' AND startedAt >= :from
        ORDER BY startedAt DESC
        """,
    )
    fun observeCompletedSince(from: Long): Flow<List<FocusSessionEntity>>
}
