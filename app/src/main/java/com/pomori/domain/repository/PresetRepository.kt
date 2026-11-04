package com.pomori.domain.repository

import com.pomori.domain.model.Preset
import kotlinx.coroutines.flow.Flow

interface PresetRepository {
    /** Oluşturulma sırasıyla. */
    fun observeAll(): Flow<List<Preset>>

    suspend fun count(): Int

    /**
     * Modu (normalize ederek) ekler ve id'sini döner. Ad boşsa hiçbir şey eklemez ve [INVALID] döner.
     */
    suspend fun add(preset: Preset): Long

    /** Var olan modu günceller. Ad boşsa ya da mod yoksa false. */
    suspend fun update(preset: Preset): Boolean

    /** Modu siler. Son kalan mod silinemez: false döner (uygulamanın her zaman en az bir modu olmalı). */
    suspend fun delete(id: Long): Boolean

    companion object {
        const val INVALID = -1L
    }
}
