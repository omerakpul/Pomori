package com.pomori.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Room enum'u adıyla TEXT olarak saklar. */
enum class ExceptionType { PACKAGE, SENDER }

/** `(type, value)` çifti benzersizdir: aynı kural iki kez eklenemez. */
@Entity(
    tableName = "exception_rules",
    indices = [Index(value = ["type", "value"], unique = true)],
)
data class ExceptionRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: ExceptionType,
    /** Paket adı ya da gönderen adı. */
    val value: String,
)
