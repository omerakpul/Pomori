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

    /** Atomik artış: okuyup yazarken seans bitişiyle yarışmasın. Yalnızca çalışan seansta sayar. */
    @Query("UPDATE focus_sessions SET interruptions = interruptions + 1 WHERE id = :id AND status = 'RUNNING'")
    suspend fun incrementInterruptions(id: Long): Int

    @Query("SELECT interruptions FROM focus_sessions WHERE id = :id")
    fun observeInterruptions(id: Long): Flow<Int?>

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
