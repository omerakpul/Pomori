package com.pomori.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pomori.domain.repository.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settings: SettingsStore,
) : ViewModel() {

    /**
     * `null`: bayrak henüz diskten okunmadı. Kök ekran bu sürede hiçbir şey çizer,
     * böylece onboarding tamamlanmış kullanıcıda bir anlığına yanıp sönmez.
     */
    val onboardingDone: StateFlow<Boolean?> = settings.onboardingDone
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun complete() {
        viewModelScope.launch { settings.setOnboardingDone() }
    }
}
