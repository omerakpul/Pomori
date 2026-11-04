package com.pomori.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Insert
    suspend fun insert(preset: PresetEntity): Long

    @Update
    suspend fun update(preset: PresetEntity)

    @Query("DELETE FROM presets WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM presets")
    suspend fun count(): Int

    /** Oluşturulma sırasıyla: ilk oluşturulan ("Varsayılan") başta kalır. */
    @Query("SELECT * FROM presets ORDER BY id")
    fun observeAll(): Flow<List<PresetEntity>>
}
