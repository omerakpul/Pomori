package com.pomori.ui.presets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pomori.domain.model.Preset
import com.pomori.domain.repository.PresetRepository
import com.pomori.domain.repository.SettingsStore
import com.pomori.domain.usecase.ObserveSelectedPreset
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PresetsUiState(
    /** İlk veri gelene kadar true; ekran bu sürede boş liste çizmemelidir. */
    val isLoading: Boolean = true,
    val presets: List<Preset> = emptyList(),
    val selectedId: Long? = null,
) {
    /** Son kalan mod silinemez: uygulamanın her zaman en az bir modu olmalı. */
    val canDelete: Boolean get() = presets.size > 1
}

@HiltViewModel
class PresetsViewModel @Inject constructor(
    private val presets: PresetRepository,
    private val settings: SettingsStore,
    observeSelectedPreset: ObserveSelectedPreset,
) : ViewModel() {

    val uiState: StateFlow<PresetsUiState> = combine(
        presets.observeAll(),
        observeSelectedPreset(),
    ) { all, selected ->
        PresetsUiState(isLoading = false, presets = all, selectedId = selected?.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PresetsUiState())

    /**
     * Yeni modu ekler (ve seçer: kullanıcı onu hemen kullanmak için oluşturur) ya da varolanı günceller.
     * Ad boşsa depo reddeder; ekran zaten Kaydet'i o durumda kapatır.
     */
    fun save(preset: Preset) {
        viewModelScope.launch {
            if (preset.id == 0L) {
                val id = presets.add(preset)
                if (id != PresetRepository.INVALID) settings.setSelectedPresetId(id)
            } else {
                presets.update(preset)
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { presets.delete(id) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
