package com.pomori.data.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Seans silinirse bildirimleri de silinir (CASCADE). `sessionId` üzerindeki indeks,
 * özet ekranının seansa göre sorgusunu ve CASCADE silmeyi hızlı tutar.
 */
@Entity(
    tableName = "captured_notifications",
    foreignKeys = [
        ForeignKey(
            entity = FocusSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class CapturedNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val packageName: String,
    val appLabel: String,
    val title: String?,
    val text: String?,
    val postedAt: Long,
)
