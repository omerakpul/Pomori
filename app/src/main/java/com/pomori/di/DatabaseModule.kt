package com.pomori.di

import android.content.Context
import androidx.room.Room
import com.pomori.data.local.db.AppDatabase
import com.pomori.data.local.db.ExceptionDao
import com.pomori.data.local.db.MIGRATION_1_2
import com.pomori.data.local.db.MIGRATION_2_3
import com.pomori.data.local.db.MIGRATION_3_4
import com.pomori.data.local.db.NotificationDao
import com.pomori.data.local.db.PresetDao
import com.pomori.data.local.db.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "pomori.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()

    @Provides
    fun provideSessionDao(db: AppDatabase): SessionDao = db.sessionDao()

    @Provides
    fun provideNotificationDao(db: AppDatabase): NotificationDao = db.notificationDao()

    @Provides
    fun provideExceptionDao(db: AppDatabase): ExceptionDao = db.exceptionDao()

    @Provides
    fun providePresetDao(db: AppDatabase): PresetDao = db.presetDao()
}
