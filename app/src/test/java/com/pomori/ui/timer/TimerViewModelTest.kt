package com.pomori.ui.timer

import app.cash.turbine.test
import com.pomori.domain.timer.TimerDefaults
import com.pomori.domain.usecase.EndSession
import com.pomori.domain.usecase.GetBreakPlan
import com.pomori.domain.usecase.StartSession
import com.pomori.testutil.FakeActiveSessionStore
import com.pomori.testutil.FakeClock
import com.pomori.testutil.FakeSessionRepository
import com.pomori.testutil.FakeSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModelTest {

    private val clock = FakeClock(nowMs = 1_000_000L)
    private val repo = FakeSessionRepository()
    private val store = FakeActiveSessionStore()
    private val endSession = EndSession(repo, store, clock)
    private val startSession = StartSession(repo, store, endSession, clock)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(settings: FakeSettingsStore = FakeSettingsStore()) =
        TimerViewModel(clock, store, startSession, endSession, settings, GetBreakPlan(repo, settings, clock))

    @Test
    fun `initial state is idle with the default focus duration`() = runTest {
        viewModel().uiState.test {
            val state = awaitItem()
            assertEquals(TimerPhase.IDLE, state.phase)
            assertEquals(TimerDefaults.FOCUS_MS, state.remainingMs)
            assertEquals(1f, state.progress, 0.0001f)
        }
    }

    @Test
    fun `starting focus shows a running focus with full remaining time`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            assertEquals(TimerPhase.IDLE, awaitItem().phase)

            vm.startFocus()

            val state = awaitItem()
            assertEquals(TimerPhase.FOCUS, state.phase)
            assertEquals(TimerDefaults.FOCUS_MS, state.remainingMs)
            assertEquals(TimerDefaults.FOCUS_MS, state.totalMs)
        }
    }

    @Test
    fun `remaining time is computed from the end time after the clock moves`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            awaitItem() // idle
            vm.startFocus()
            awaitItem() // focus, tam süre

            clock.advanceBy(10 * 60_000L)
            testScheduler.advanceTimeBy(500)
            testScheduler.runCurrent()

            val state = awaitItem()
            assertEquals(TimerPhase.FOCUS, state.phase)
            assertEquals(TimerDefaults.FOCUS_MS - 10 * 60_000L, state.remainingMs)
        }
    }

    @Test
    fun `a viewmodel created after process death restores the running focus`() = runTest {
        // Önceki "süreç": seansı başlat, 7 dk geç.
        startSession(com.pomori.domain.model.SessionType.FOCUS, TimerDefaults.FOCUS_MS)
        clock.advanceBy(7 * 60_000L)

        // Yeni ViewModel (yeniden açılan uygulama) aynı DataStore'dan okur.
        viewModel().uiState.test {
            var state = awaitItem()
            if (state.phase == TimerPhase.IDLE) state = awaitItem()
            assertEquals(TimerPhase.FOCUS, state.phase)
            assertEquals(TimerDefaults.FOCUS_MS - 7 * 60_000L, state.remainingMs)
        }
    }

    @Test
    fun `when time is up the screen moves to break pending and the session is completed`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            awaitItem() // idle
            vm.startFocus()
            awaitItem() // focus

            clock.advanceBy(TimerDefaults.FOCUS_MS)
            testScheduler.advanceTimeBy(500)
            testScheduler.runCurrent()

            var state = awaitItem()
            while (state.phase == TimerPhase.FOCUS) state = awaitItem()
            assertEquals(TimerPhase.BREAK_PENDING, state.phase)
            assertEquals(
                com.pomori.domain.model.SessionStatus.COMPLETED,
                repo.rows.values.single().status,
            )
            // Seans tamamlanınca döngü sayacı da güncellenir: aynı faz bir kez daha yayınlanabilir.
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `cancelling returns to idle`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            awaitItem() // idle
            vm.startFocus()
            awaitItem() // focus

            vm.cancelFocus()

            assertEquals(TimerPhase.IDLE, awaitItem().phase)
        }
    }

    // --- Seans bitince bir kerelik özet yönlendirmesi ---

    private fun TestScope.finishFocus() {
        clock.advanceBy(TimerDefaults.FOCUS_MS)
        testScheduler.advanceTimeBy(500)
        testScheduler.runCurrent()
    }

    @Test
    fun `no digest is suggested while idle or focusing`() = runTest {
        val vm = viewModel()
        vm.pendingDigestSessionId.test {
            assertEquals(null, awaitItem())

            vm.startFocus()
            testScheduler.runCurrent()

            expectNoEvents()
        }
    }

    @Test
    fun `when focus ends the digest of that session is suggested`() = runTest {
        val vm = viewModel()
        vm.pendingDigestSessionId.test {
            assertEquals(null, awaitItem())
            vm.startFocus()
            testScheduler.runCurrent()
            val sessionId = repo.rows.keys.single()

            finishFocus()

            assertEquals(sessionId, awaitItem())
        }
    }

    @Test
    fun `after the digest was shown it is not suggested again`() = runTest {
        val vm = viewModel()
        vm.pendingDigestSessionId.test {
            assertEquals(null, awaitItem())
            vm.startFocus()
            testScheduler.runCurrent()
            finishFocus()
            val sessionId = awaitItem()!!

            vm.onDigestShown(sessionId)

            assertEquals(null, awaitItem())
        }
        // Ekran yeniden oluşunca (aynı ViewModel) mola hâlâ bekliyor ama özet tekrar önerilmez.
        vm.pendingDigestSessionId.test {
            assertEquals(null, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `the next focus session suggests its own digest again`() = runTest {
        val vm = viewModel()
        vm.pendingDigestSessionId.test {
            assertEquals(null, awaitItem())
            vm.startFocus()
            testScheduler.runCurrent()
            finishFocus()
            vm.onDigestShown(awaitItem()!!)
            assertEquals(null, awaitItem())

            vm.skipBreak()
            vm.startFocus()
            testScheduler.runCurrent()
            finishFocus()

            assertEquals(repo.rows.keys.max(), awaitItem())
        }
    }

    @Test
    fun `skipping the break pending state withdraws the suggestion`() = runTest {
        val vm = viewModel()
        vm.pendingDigestSessionId.test {
            assertEquals(null, awaitItem())
            vm.startFocus()
            testScheduler.runCurrent()
            finishFocus()
            awaitItem() // öneri geldi, henüz gösterilmedi

            vm.skipBreak()

            assertEquals(null, awaitItem())
        }
    }

    // --- ayarlanan süreler ---

    @Test
    fun `idle shows the configured focus duration`() = runTest {
        viewModel(FakeSettingsStore(focusMinutes = 10)).uiState.test {
            var state = awaitItem()
            if (state.remainingMs != 10 * 60_000L) state = awaitItem()
            assertEquals(TimerPhase.IDLE, state.phase)
            assertEquals(10 * 60_000L, state.remainingMs)
            assertEquals(10 * 60_000L, state.totalMs)
        }
    }

    @Test
    fun `starting focus uses the configured duration`() = runTest {
        val vm = viewModel(FakeSettingsStore(focusMinutes = 10))
        vm.uiState.test {
            awaitItem() // idle
            vm.startFocus()

            var state = awaitItem()
            while (state.phase != TimerPhase.FOCUS) state = awaitItem()
            assertEquals(10 * 60_000L, state.totalMs)
            assertEquals(10 * 60_000L, state.remainingMs)
        }
        assertEquals(10 * 60_000L, repo.rows.values.single().plannedDurationMs)
    }

    @Test
    fun `starting a break uses the configured break duration`() = runTest {
        val vm = viewModel(FakeSettingsStore(focusMinutes = 1, breakMinutes = 3))
        vm.uiState.test {
            awaitItem()
            vm.startFocus()
            testScheduler.runCurrent()
            clock.advanceBy(60_000L)
            testScheduler.advanceTimeBy(500)
            testScheduler.runCurrent()
            var state = awaitItem()
            while (state.phase != TimerPhase.BREAK_PENDING) state = awaitItem()

            vm.startBreak()

            while (state.phase != TimerPhase.BREAK) state = awaitItem()
            assertEquals(3 * 60_000L, state.totalMs)
        }
    }

    @Test
    fun `changing the setting during a session does not change the running one`() = runTest {
        val settings = FakeSettingsStore(focusMinutes = 10)
        val vm = viewModel(settings)
        vm.uiState.test {
            awaitItem()
            vm.startFocus()
            var state = awaitItem()
            while (state.phase != TimerPhase.FOCUS) state = awaitItem()

            settings.setFocusMinutes(50)
            testScheduler.runCurrent()
            expectNoEvents()

            assertEquals(10 * 60_000L, state.totalMs)
        }
    }

    // --- uzun mola ---

    private suspend fun completedFocusToday(count: Int) = repeat(count) {
        repo.insert(
            com.pomori.domain.model.FocusSession(
                type = com.pomori.domain.model.SessionType.FOCUS,
                startedAt = clock.now(),
                plannedDurationMs = TimerDefaults.FOCUS_MS,
                endedAt = clock.now(),
                status = com.pomori.domain.model.SessionStatus.COMPLETED,
            ),
        )
    }

    /** Bir odak seansı çalıştırıp süresini doldurur; bekleyen mola durumundaki son durumu döner. */
    private suspend fun app.cash.turbine.ReceiveTurbine<TimerUiState>.finishFocusAndWait(
        vm: TimerViewModel,
        testScheduler: kotlinx.coroutines.test.TestCoroutineScheduler,
        until: (TimerUiState) -> Boolean,
    ): TimerUiState {
        awaitItem() // idle
        vm.startFocus()
        awaitItem() // focus
        clock.advanceBy(TimerDefaults.FOCUS_MS)
        testScheduler.advanceTimeBy(500)
        testScheduler.runCurrent()
        var state = awaitItem()
        while (!(state.phase == TimerPhase.BREAK_PENDING && until(state))) state = awaitItem()
        return state
    }

    @Test
    fun `the fourth completed focus session of the day earns a long break`() = runTest {
        completedFocusToday(3) // bu seansla 4 olur
        val vm = viewModel(FakeSettingsStore(breakMinutes = 5, longBreakMinutes = 20))
        vm.uiState.test {
            val pending = finishFocusAndWait(vm, testScheduler) { it.isLongBreak }
            assertEquals(20 * 60_000L, pending.totalMs)

            vm.startBreak()
            var state = pending
            while (state.phase != TimerPhase.BREAK) state = awaitItem()

            assertEquals(true, state.isLongBreak)
            assertEquals(20 * 60_000L, state.totalMs)
            assertEquals(true, store.current()?.isLongBreak)
        }
    }

    @Test
    fun `the first focus session earns a normal short break`() = runTest {
        val vm = viewModel(FakeSettingsStore(breakMinutes = 5, longBreakMinutes = 20))
        vm.uiState.test {
            val pending = finishFocusAndWait(vm, testScheduler) { it.cycleDone == 1 }
            assertEquals(false, pending.isLongBreak)

            vm.startBreak()
            var state = pending
            while (state.phase != TimerPhase.BREAK) state = awaitItem()

            assertEquals(false, state.isLongBreak)
            assertEquals(5 * 60_000L, state.totalMs)
            assertEquals(false, store.current()?.isLongBreak)
        }
    }

    @Test
    fun `cycle dots count the focus sessions of the day within the cycle`() = runTest {
        completedFocusToday(5) // 5 % 4 = 1
        val vm = viewModel()
        vm.uiState.test {
            var state = awaitItem()
            while (state.cycleDone != 1) state = awaitItem()
            assertEquals(1, state.cycleFilled)
        }
    }
}
