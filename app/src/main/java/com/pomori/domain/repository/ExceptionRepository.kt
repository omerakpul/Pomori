package com.pomori.domain.repository

import com.pomori.domain.model.ExceptionRule
import kotlinx.coroutines.flow.Flow

interface ExceptionRepository {
    /** Kurallar, eklenme sırasıyla; her değişiklikte yeniden yayınlanır. */
    fun observeAll(): Flow<List<ExceptionRule>>

    /**
     * Kuralı ekler. Değer baştaki/sondaki boşluklardan arındırılır. Boşsa ya da aynı kural zaten
     * varsa eklemez ve `false` döner.
     */
    suspend fun add(rule: ExceptionRule): Boolean

    suspend fun remove(rule: ExceptionRule)
}
