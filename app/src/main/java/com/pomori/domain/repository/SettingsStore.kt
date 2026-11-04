package com.pomori.domain.repository

import kotlinx.coroutines.flow.Flow

/** Kullanıcı ayarları. Şimdilik yalnızca onboarding bayrağı; süreler ve saklama süresi Hafta 3'te eklenecek. */
interface SettingsStore {
    val onboardingDone: Flow<Boolean>

    suspend fun setOnboardingDone()
}
