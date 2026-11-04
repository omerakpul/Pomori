package com.pomori.di

import com.pomori.data.time.RealClock
import com.pomori.domain.Clock
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ClockModule {

    @Binds
    abstract fun bindClock(impl: RealClock): Clock
}
