package com.pomori.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Insert
    suspend fun insert(notification: CapturedNotificationEntity): Long

    /** `olderThan`'dan (epoch ms) kesin olarak eski bildirimleri siler, silinen sayıyı döner. */
    @Query("DELETE FROM captured_notifications WHERE postedAt < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long): Int

    /** Aynı `postedAt` değerinde sıra `id` ile belirlenir, böylece sonuç her zaman aynı kalır. */
    @Query(
        """
        SELECT * FROM captured_notifications
        WHERE sessionId = :sessionId
        ORDER BY postedAt DESC, id DESC
        """,
    )
    fun observeBySession(sessionId: Long): Flow<List<CapturedNotificationEntity>>

    /**
     * Uygulama başına bildirim sayısı. `MAX(postedAt)` ile birlikte seçilen çıplak `appLabel`,
     * SQLite kuralı gereği en yeni satırdan gelir; etiket zamanla değişmişse güncel olan görünür.
     */
    @Query(
        """
        SELECT packageName, appLabel, COUNT(*) AS cnt, MAX(postedAt) AS lastAt
        FROM captured_notifications
        WHERE postedAt >= :from
        GROUP BY packageName
        ORDER BY cnt DESC, appLabel COLLATE NOCASE, packageName
        LIMIT :limit
        """,
    )
    fun observeTopDistractors(from: Long, limit: Int): Flow<List<DistractorRow>>
}

/** [NotificationDao.observeTopDistractors] sorgusunun satırı. `lastAt` yalnızca etiket seçimi içindir. */
data class DistractorRow(
    val packageName: String,
    val appLabel: String,
    val cnt: Int,
    val lastAt: Long,
)
