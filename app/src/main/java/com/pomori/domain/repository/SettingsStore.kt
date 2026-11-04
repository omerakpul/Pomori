package com.pomori.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Kullanıcı ayarları. Değerler her zaman `SettingsDefaults` aralıklarına çekilir (bozuk ya da
 * eski bir değer okunsa bile akış geçerli bir sayı yayınlar).
 */
interface SettingsStore {
    val onboardingDone: Flow<Boolean>

    /** Odak seansı süresi, dakika. */
    val focusMinutes: Flow<Int>

    /** Mola süresi, dakika. */
    val breakMinutes: Flow<Int>

    /** Yakalanan bildirimlerin kaç gün saklanacağı. */
    val retentionDays: Flow<Int>

    suspend fun setOnboardingDone()

    suspend fun setFocusMinutes(minutes: Int)

    suspend fun setBreakMinutes(minutes: Int)

    suspend fun setRetentionDays(days: Int)
}
