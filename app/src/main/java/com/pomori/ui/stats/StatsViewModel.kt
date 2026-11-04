package com.pomori.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pomori.domain.usecase.GetGoalProgress
import com.pomori.domain.usecase.GetWeeklyStats
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
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = combine(getWeeklyStats(), getGoalProgress()) { stats, goal ->
        StatsUiState(
            isLoading = false,
            totalFocusMs = stats.totalFocusMs,
            completedSessions = stats.completedSessions,
            topDistractors = stats.topDistractors,
            dailyFocus = stats.dailyFocus,
            labelBreakdown = stats.labelBreakdown,
            goal = goal,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), StatsUiState())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
