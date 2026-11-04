package com.pomori.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pomori.domain.model.ExceptionRule
import com.pomori.domain.model.InstalledApp
import com.pomori.domain.repository.ExceptionRepository
import com.pomori.domain.repository.InstalledApps
import com.pomori.domain.repository.PresetRepository
import com.pomori.domain.repository.SettingsStore
import com.pomori.domain.usecase.CleanupOldNotifications
import com.pomori.domain.usecase.ObserveSelectedPreset
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private data class GeneralSettings(val retentionDays: Int, val restore: Boolean, val dailyGoalMinutes: Int)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsStore,
    private val exceptions: ExceptionRepository,
    private val cleanupOldNotifications: CleanupOldNotifications,
    installedApps: InstalledApps,
    presets: PresetRepository,
    observeSelectedPreset: ObserveSelectedPreset,
) : ViewModel() {

    /** Yüklü uygulamalar bir kez okunur (PackageManager sorgusu pahalı); ekran açılışında gelir. */
    private val installed = MutableStateFlow<List<InstalledApp>>(emptyList())

    init {
        viewModelScope.launch { installed.value = installedApps.launchableApps() }
    }

    /** `combine` türlü en fazla 5 akış alır; sayısal ve açık/kapalı ayarları bir çifte toplarız. */
    private val general = combine(
        settings.retentionDays,
        settings.restoreNotifications,
        settings.dailyGoalMinutes,
    ) { days, restore, goal -> GeneralSettings(days, restore, goal) }

    val uiState: StateFlow<SettingsUiState> = combine(
        presets.observeAll(),
        observeSelectedPreset(),
        general,
        exceptions.observeAll(),
        installed,
    ) { allPresets, selected, general, rules, apps ->
        val labels = apps.associate { it.packageName to it.label }
        val excepted = rules.filterIsInstance<ExceptionRule.Package>().map { it.value }.toSet()
        SettingsUiState(
            presetCount = allPresets.size,
            selectedPresetName = selected?.name,
            retentionDays = general.retentionDays,
            restoreNotifications = general.restore,
            dailyGoalMinutes = general.dailyGoalMinutes,
            rules = rules.map { RuleItem(it, displayName(it, labels)) },
            selectableApps = apps.filter { it.packageName !in excepted },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SettingsUiState())

    fun setDailyGoalMinutes(minutes: Int) = launch { settings.setDailyGoalMinutes(minutes) }

    fun setRestoreNotifications(enabled: Boolean) = launch { settings.setRestoreNotifications(enabled) }

    // Süre kısaltıldığında kullanıcı bildirimlerin silinmesini bekler: günlük işi beklemeden hemen temizle.
    fun setRetentionDays(days: Int) = launch {
        settings.setRetentionDays(days)
        cleanupOldNotifications()
    }

    fun addSender(name: String) = launch { exceptions.add(ExceptionRule.Sender(name)) }

    fun addApp(app: InstalledApp) = launch { exceptions.add(ExceptionRule.Package(app.packageName)) }

    fun removeRule(rule: ExceptionRule) = launch { exceptions.remove(rule) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private fun displayName(rule: ExceptionRule, labels: Map<String, String>): String = when (rule) {
        // Uygulama kaldırılmış ya da görünmüyorsa paket adı gösterilir.
        is ExceptionRule.Package -> labels[rule.value] ?: rule.value
        is ExceptionRule.Sender -> rule.value
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
