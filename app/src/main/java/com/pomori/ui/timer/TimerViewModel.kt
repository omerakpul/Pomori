package com.pomori.ui.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pomori.domain.Clock
import com.pomori.domain.model.ActiveSession
import com.pomori.domain.model.SessionType
import com.pomori.domain.repository.ActiveSessionStore
import com.pomori.domain.timer.TimerDefaults
import com.pomori.domain.timer.TimerState
import com.pomori.domain.timer.toTimerState
import com.pomori.domain.usecase.EndReason
import com.pomori.domain.usecase.EndSession
import com.pomori.domain.usecase.StartSession
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val clock: Clock,
    activeSessionStore: ActiveSessionStore,
    private val startSession: StartSession,
    private val endSession: EndSession,
) : ViewModel() {

    /**
     * Tek doğruluk kaynağı DataStore'daki aktif seans. Ekran her tikte
     * `kalan = bitişZamanı - şimdi` hesaplar; bir sayaç değişkenini azaltmaz.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TimerUiState> = activeSessionStore.active
        .flatMapLatest { active -> ticking(active) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), TimerUiState())

    private fun ticking(active: ActiveSession?): Flow<TimerUiState> = flow {
        var settled = false
        while (true) {
            val now = clock.now()
            val state = active.toTimerState(now)
            emit(state.toUiState(now))

            // Süre dolduysa kaydı kapat (idempotent; servis de aynısını yapabilir).
            val expired = active != null && (state is TimerState.BreakPending || state is TimerState.Idle)
            if (expired && !settled) {
                settled = true
                endSession(EndReason.TIME_UP)
            }
            // Sürekli güncelleme yalnızca süre akarken gerekli. Diğer durumlarda
            // aktif seans değişince flatMapLatest akışı yeniden başlatır.
            if (state is TimerState.Focus || state is TimerState.Break) delay(TICK_MS) else break
        }
    }

    fun startFocus() = launch { startSession(SessionType.FOCUS, TimerDefaults.FOCUS_MS) }

    fun startBreak() = launch { startSession(SessionType.BREAK, TimerDefaults.BREAK_MS) }

    fun cancelFocus() = launch { endSession(EndReason.CANCEL) }

    fun skipBreak() = launch { endSession(EndReason.SKIP) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private companion object {
        const val TICK_MS = 250L
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
