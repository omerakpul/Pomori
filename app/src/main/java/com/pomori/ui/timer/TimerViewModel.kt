package com.pomori.ui.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pomori.domain.Clock
import com.pomori.domain.model.ActiveSession
import com.pomori.domain.model.MAX_LABEL_LENGTH
import com.pomori.domain.model.SessionType
import com.pomori.domain.repository.ActiveSessionStore
import com.pomori.domain.repository.PresetRepository
import com.pomori.domain.repository.SessionRepository
import com.pomori.domain.repository.SettingsStore
import com.pomori.domain.timer.TimerDefaults
import com.pomori.domain.timer.TimerState
import com.pomori.domain.timer.toTimerState
import com.pomori.domain.usecase.EndReason
import com.pomori.domain.usecase.EndSession
import com.pomori.domain.usecase.GetBreakPlan
import com.pomori.domain.usecase.GetDigest
import com.pomori.domain.usecase.ObserveSelectedPreset
import com.pomori.domain.usecase.RecordInterruption
import com.pomori.domain.usecase.StartSession
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val clock: Clock,
    private val activeSessionStore: ActiveSessionStore,
    private val startSession: StartSession,
    private val endSession: EndSession,
    private val settings: SettingsStore,
    private val getBreakPlan: GetBreakPlan,
    private val getDigest: GetDigest,
    private val observeSelectedPreset: ObserveSelectedPreset,
    private val recordInterruption: RecordInterruption,
    sessions: SessionRepository,
    presets: PresetRepository,
) : ViewModel() {

    /**
     * Tek doğruluk kaynağı DataStore'daki aktif seans. Ekran her tikte
     * `kalan = bitişZamanı - şimdi` hesaplar; bir sayaç değişkenini azaltmaz.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TimerUiState> = combine(
        activeSessionStore.active.flatMapLatest { active -> ticking(active) },
        observeSelectedPreset(),
        presets.observeAll(),
        getBreakPlan(),
        activeSessionStore.active.flatMapLatest { a ->
            if (a == null) flowOf(0) else sessions.observeInterruptions(a.sessionId)
        },
    ) { ticking, selected, all, plan, interruptions ->
        val state = ticking.copy(
            cycleDone = plan.focusDoneInCycle,
            cycleLength = plan.cycleLength,
            presets = all,
            selectedPresetId = selected?.id,
            interruptions = interruptions,
        )
        when (state.phase) {
            // Boştayken, başlatılınca çalışacak süreyi (seçili modun odak süresini) göster.
            TimerPhase.IDLE -> ((selected?.focusMinutes ?: return@combine state) * MS_PER_MINUTE)
                .let { ms -> state.copy(remainingMs = ms, totalMs = ms) }
            // Bekleyen molanın uzun mu kısa mı olacağı, tamamlanan odak sayısından gelir.
            TimerPhase.BREAK_PENDING -> state.copy(isLongBreak = plan.isLong, totalMs = plan.durationMs)
            else -> state
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), TimerUiState())

    /** Sonraki seansa yazılacak etiket taslağı. Seans başlayınca korunur: aynı işe arka arkaya odaklanmak sık görülür. */
    private val _label = MutableStateFlow("")
    val label: StateFlow<String> = _label

    /** Önceki seanslardan öneri olarak sunulan etiketler. */
    val recentLabels: StateFlow<List<String>> = sessions.observeRecentLabels(RECENT_LABELS)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    fun setLabel(value: String) {
        _label.value = value.take(MAX_LABEL_LENGTH)
    }

    private val shownDigestSessionId = MutableStateFlow<Long?>(null)

    /**
     * İptal edilen ve bildirim yakalamış seans. Bildirimler yakalanırken sistemden kaldırıldığı için, özet
     * gösterilmezse kaybolurlardı: iptalde de özet açılır (hiç bildirim yakalanmadıysa açılmaz).
     */
    private val cancelledDigestSessionId = MutableStateFlow<Long?>(null)

    /**
     * Özeti gösterilmesi gereken seans: odak seansı bitip mola beklemeye geçince ya da bildirim yakalamış bir
     * seans iptal edilince, bir kez yayınlanır. [onDigestShown] çağrılınca (ya da ekran yeniden oluşunca bile) aynı
     * seans için tekrar yayınlanmaz; kullanıcı özeti kapatıp mola beklerken ekran onu tekrar açmaya zorlanmaz.
     */
    val pendingDigestSessionId: StateFlow<Long?> =
        combine(
            uiState,
            activeSessionStore.active,
            shownDigestSessionId,
            cancelledDigestSessionId,
        ) { ui, active, shown, cancelled ->
            val sessionId = active?.sessionId
            val finished = sessionId.takeIf { ui.phase == TimerPhase.BREAK_PENDING }
            (finished ?: cancelled)?.takeIf { it != shown }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    /** Özet ekranı açıldı; aynı seans için tekrar önerme. */
    fun onDigestShown(sessionId: Long) {
        shownDigestSessionId.value = sessionId
    }

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

    // Süre, seans başlarken seçili modundan okunur; sonradan değişen mod çalışan seansı etkilemez
    // (bitiş zamanı başlangıçta DataStore'a yazılır). Seans, çalıştığı modun id'sini taşır.
    fun startFocus() = launch {
        val preset = observeSelectedPreset().filterNotNull().first()
        startSession(SessionType.FOCUS, preset.focusMinutes * MS_PER_MINUTE, presetId = preset.id, label = _label.value)
    }

    /**
     * Modu seçer. Yalnızca boştayken: çalışan bir seansın (ya da bekleyen molanın) ortasında mod değişirse
     * mola planı yanlış modun süresine göre hesaplanırdı. Ekran da çipleri yalnızca boştayken gösterir.
     */
    fun selectPreset(id: Long) = launch {
        if (activeSessionStore.current() == null) settings.setSelectedPresetId(id)
    }

    // Uzun mu kısa mı olacağı o anki plandan okunur ve kayda yazılır; sonradan ayar değişse de mola aynı kalır.
    fun startBreak() = launch {
        val plan = getBreakPlan().first()
        startSession(SessionType.BREAK, plan.durationMs, plan.isLong, plan.presetId)
    }

    fun cancelFocus() = launch {
        val sessionId = activeSessionStore.current()?.sessionId
        endSession(EndReason.CANCEL)
        // Seans boyunca yakalanan bildirimler sistemden kaldırılmıştı; iptal edildi diye kaybolmasınlar.
        if (sessionId != null && getDigest(sessionId).first().total > 0) {
            cancelledDigestSessionId.value = sessionId
        }
    }

    /** Süre durmaz; yalnızca çalışan odak seansının sayacını artırır. */
    fun interrupt() = launch { recordInterruption() }

    fun skipBreak() = launch { endSession(EndReason.SKIP) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private companion object {
        const val TICK_MS = 250L
        const val MS_PER_MINUTE = 60_000L
        const val STOP_TIMEOUT_MS = 5_000L
        const val RECENT_LABELS = 6
    }
}
