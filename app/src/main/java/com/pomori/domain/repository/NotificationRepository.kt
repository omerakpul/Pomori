package com.pomori.domain.repository

import com.pomori.domain.model.CapturedNotification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    /** Bildirimi kaydeder, üretilen id'yi döner. Seans yoksa kayıt reddedilir (yabancı anahtar). */
    suspend fun insert(notification: CapturedNotification): Long

    /** Seansın bildirimlerini en yeniden eskiye sıralı verir ve yeni kayıtlarda güncellenir. */
    fun observeBySession(sessionId: Long): Flow<List<CapturedNotification>>
}
