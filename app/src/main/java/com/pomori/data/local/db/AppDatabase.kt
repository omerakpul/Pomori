package com.pomori.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FocusSessionEntity::class, CapturedNotificationEntity::class, ExceptionRuleEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    abstract fun notificationDao(): NotificationDao

    abstract fun exceptionDao(): ExceptionDao
}
