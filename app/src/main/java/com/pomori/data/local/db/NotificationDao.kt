package com.pomori.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Insert
    suspend fun insert(notification: CapturedNotificationEntity): Long

    /** Aynı `postedAt` değerinde sıra `id` ile belirlenir, böylece sonuç her zaman aynı kalır. */
    @Query(
        """
        SELECT * FROM captured_notifications
        WHERE sessionId = :sessionId
        ORDER BY postedAt DESC, id DESC
        """,
    )
    fun observeBySession(sessionId: Long): Flow<List<CapturedNotificationEntity>>
}
