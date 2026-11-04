package com.pomori.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [FocusSessionEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
}
