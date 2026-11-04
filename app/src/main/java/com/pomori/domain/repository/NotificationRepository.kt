package com.pomori.domain.repository

import com.pomori.domain.model.CapturedNotification
import com.pomori.domain.model.Distractor
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    /** Bildirimi kaydeder, üretilen id'yi döner. Seans yoksa kayıt reddedilir (yabancı anahtar). */
    suspend fun insert(notification: CapturedNotification): Long

    /** `olderThan`'dan (epoch ms) kesin olarak eski bildirimleri siler, silinen sayıyı döner. */
    suspend fun deleteOlderThan(olderThan: Long): Int

    /** Seansın bildirimlerini en yeniden eskiye sıralı verir ve yeni kayıtlarda güncellenir. */
    fun observeBySession(sessionId: Long): Flow<List<CapturedNotification>>

    /**
     * `from`'dan (epoch ms) beri en çok bildirim yakalanan uygulamalar, en fazla `limit` tane.
     * Sayıya göre azalan; eşitlikte uygulama adı (harf duyarsız), sonra paket adı.
     */
    fun observeTopDistractors(from: Long, limit: Int): Flow<List<Distractor>>
}
