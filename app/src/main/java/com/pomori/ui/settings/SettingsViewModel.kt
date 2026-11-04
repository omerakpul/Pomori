package com.pomori.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pomori.domain.model.ExceptionRule
import com.pomori.domain.model.InstalledApp
import com.pomori.domain.repository.ExceptionRepository
import com.pomori.domain.repository.InstalledApps
import com.pomori.domain.repository.SettingsStore
import com.pomori.domain.usecase.CleanupOldNotifications
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsStore,
    private val exceptions: ExceptionRepository,
    private val cleanupOldNotifications: CleanupOldNotifications,
    installedApps: InstalledApps,
) : ViewModel() {

    /** Yüklü uygulamalar bir kez okunur (PackageManager sorgusu pahalı); ekran açılışında gelir. */
    private val installed = MutableStateFlow<List<InstalledApp>>(emptyList())

    init {
        viewModelScope.launch { installed.value = installedApps.launchableApps() }
    }

    /** Odak, mola ve uzun mola süreleri. `combine` türlü en fazla 5 akış alır; süreleri bir arada tutarız. */
    private data class Durations(val focus: Int, val break_: Int, val longBreak: Int)

    private val durations = combine(
        settings.focusMinutes,
        settings.breakMinutes,
        settings.longBreakMinutes,
        ::Durations,
    )

    val uiState: StateFlow<SettingsUiState> = combine(
        durations,
        settings.retentionDays,
        exceptions.observeAll(),
        installed,
    ) { time, retention, rules, apps ->
        val labels = apps.associate { it.packageName to it.label }
        val excepted = rules.filterIsInstance<ExceptionRule.Package>().map { it.value }.toSet()
        SettingsUiState(
            focusMinutes = time.focus,
            breakMinutes = time.break_,
            longBreakMinutes = time.longBreak,
            retentionDays = retention,
            rules = rules.map { RuleItem(it, displayName(it, labels)) },
            selectableApps = apps.filter { it.packageName !in excepted },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SettingsUiState())

    // Aralık dışı değerleri depo en yakın sınıra çeker; ekran sınırlarda düğmeyi zaten kapatır.
    fun setFocusMinutes(minutes: Int) = launch { settings.setFocusMinutes(minutes) }

    fun setBreakMinutes(minutes: Int) = launch { settings.setBreakMinutes(minutes) }

    fun setLongBreakMinutes(minutes: Int) = launch { settings.setLongBreakMinutes(minutes) }

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
