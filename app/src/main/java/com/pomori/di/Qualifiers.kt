package com.pomori.di

import javax.inject.Qualifier

/** Aktif seans için ayrılmış DataStore (ayarlar DataStore'undan ayrıdır). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ActiveSessionPrefs
