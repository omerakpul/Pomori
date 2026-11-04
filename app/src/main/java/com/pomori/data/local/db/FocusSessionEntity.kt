package com.pomori.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pomori.domain.model.SessionStatus
import com.pomori.domain.model.SessionType

/** Room 2.3+ enum'ları adıyla TEXT olarak saklar; ayrıca TypeConverter gerekmez. */
@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: SessionType,
    val startedAt: Long,
    val plannedDurationMs: Long,
    val endedAt: Long?,
    val status: SessionStatus,
    val presetId: Long? = null,
    @ColumnInfo(defaultValue = "0") val interruptions: Int = 0,
    /** Kullanılmıyor: etiket özelliği kaldırıldı (mod adı etiket görevi görür). Sütun v6 şemasıyla uyumlu kalsın diye duruyor. */
    val label: String? = null,
)
