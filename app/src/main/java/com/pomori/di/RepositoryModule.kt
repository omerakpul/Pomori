package com.pomori.di

import com.pomori.data.local.prefs.ActiveSessionDataStore
import com.pomori.data.repository.SessionRepositoryImpl
import com.pomori.domain.repository.ActiveSessionStore
import com.pomori.domain.repository.SessionRepository
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
}
