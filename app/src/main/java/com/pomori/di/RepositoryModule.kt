package com.pomori.di

import com.pomori.data.local.prefs.ActiveSessionDataStore
import com.pomori.data.local.prefs.SettingsDataStore
import com.pomori.data.repository.ExceptionRepositoryImpl
import com.pomori.data.repository.NotificationRepositoryImpl
import com.pomori.data.repository.SessionRepositoryImpl
import com.pomori.platform.apps.PackageManagerInstalledApps
import com.pomori.domain.repository.ActiveSessionStore
import com.pomori.domain.repository.ExceptionRepository
import com.pomori.domain.repository.InstalledApps
import com.pomori.domain.repository.NotificationRepository
import com.pomori.domain.repository.SessionRepository
import com.pomori.domain.repository.SettingsStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    abstract fun bindActiveSessionStore(impl: ActiveSessionDataStore): ActiveSessionStore

    @Binds
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

    @Binds
    abstract fun bindExceptionRepository(impl: ExceptionRepositoryImpl): ExceptionRepository

    @Binds
    abstract fun bindInstalledApps(impl: PackageManagerInstalledApps): InstalledApps

    @Binds
    abstract fun bindSettingsStore(impl: SettingsDataStore): SettingsStore
}
