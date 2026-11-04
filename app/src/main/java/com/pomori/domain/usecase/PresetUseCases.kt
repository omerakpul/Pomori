package com.pomori.domain.usecase

import com.pomori.domain.model.Preset
import com.pomori.domain.repository.PresetRepository
import com.pomori.domain.repository.SettingsStore
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first

/**
 * Hiç mod yoksa (ilk açılış ya da modlardan önceki bir sürümden güncelleme) "Varsayılan" modu oluşturur ve seçer.
 * Eski tek-ayar süreleri (odak/mola/uzun mola) bu moda taşınır: güncelleyen kullanıcı kendi sürelerini kaybetmez.
 * Mod varsa hiçbir şey yapmaz; birden çok kez çağrılması zararsızdır.
 */
class EnsureDefaultPreset @Inject constructor(
    private val presets: PresetRepository,
    private val settings: SettingsStore,
) {
    suspend operator fun invoke() {
        if (presets.count() > 0) return
        val id = presets.add(
            Preset(
                name = Preset.DEFAULT_NAME,
                focusMinutes = settings.focusMinutes.first(),
                breakMinutes = settings.breakMinutes.first(),
                longBreakMinutes = settings.longBreakMinutes.first(),
            ),
        )
        if (id != PresetRepository.INVALID) settings.setSelectedPresetId(id)
    }
}

/**
 * Şu an seçili mod. Kayıtlı seçim yoksa ya da o mod silinmişse ilk mod; hiç mod yoksa (henüz tohumlanmadı) null.
 */
class ObserveSelectedPreset @Inject constructor(
    private val presets: PresetRepository,
    private val settings: SettingsStore,
) {
    operator fun invoke(): Flow<Preset?> =
        combine(presets.observeAll(), settings.selectedPresetId) { list, selectedId ->
            list.firstOrNull { it.id == selectedId } ?: list.firstOrNull()
        }.distinctUntilChanged()
}
