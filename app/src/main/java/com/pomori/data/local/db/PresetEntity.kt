package com.pomori.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val focusMinutes: Int,
    val breakMinutes: Int,
    val longBreakMinutes: Int,
    val longBreakEvery: Int,
)
