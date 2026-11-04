package com.pomori.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExceptionDao {
    /** Aynı `(type, value)` zaten varsa eklemez ve `-1` döner. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(rule: ExceptionRuleEntity): Long

    @Query("DELETE FROM exception_rules WHERE type = :type AND value = :value")
    suspend fun delete(type: ExceptionType, value: String)

    /** Eklenme sırasıyla. */
    @Query("SELECT * FROM exception_rules ORDER BY id")
    fun observeAll(): Flow<List<ExceptionRuleEntity>>
}
