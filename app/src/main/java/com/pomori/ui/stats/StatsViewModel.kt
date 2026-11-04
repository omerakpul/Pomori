package com.pomori.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pomori.domain.repository.PresetRepository
import com.pomori.domain.usecase.GetGoalProgress
import com.pomori.domain.usecase.GetWeeklyStats
import com.pomori.domain.model.ModeFocus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class StatsViewModel @Inject constructor(
    getWeeklyStats: GetWeeklyStats,
    getGoalProgress: GetGoalProgress,
    presets: PresetRepository,
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = combine(getWeeklyStats(), getGoalProgress(), presets.observeAll()) { stats, goal, allPresets ->
        val names = allPresets.associate { it.id to it.name }
        StatsUiState(
            isLoading = false,
            totalFocusMs = stats.totalFocusMs,
            completedSessions = stats.completedSessions,
            topDistractors = stats.topDistractors,
            dailyFocus = stats.dailyFocus,
            modeBreakdown = resolveModes(stats.modeBreakdown, names),
            goal = goal,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), StatsUiState())

    /** Mod kimliklerini adlara çevirir; adı çözülemeyenleri ("Diğer") tek satırda birleştirir, yine sona koyar. */
    private fun resolveModes(modes: List<ModeFocus>, names: Map<Long, String>): List<ModeStat> {
        val named = modes.filter { names[it.presetId] != null }
            .map { ModeStat(names.getValue(it.presetId!!), it.focusMs, it.sessions) }
        val unknown = modes.filter { names[it.presetId] == null }
        val other = if (unknown.isEmpty()) emptyList() else {
            listOf(ModeStat(null, unknown.sumOf { it.focusMs }, unknown.sumOf { it.sessions }))
        }
        return named + other
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
