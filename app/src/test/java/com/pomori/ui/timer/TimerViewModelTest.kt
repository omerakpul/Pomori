package com.pomori.ui.timer

import app.cash.turbine.test
import com.pomori.domain.timer.TimerDefaults
import com.pomori.domain.usecase.EndSession
import com.pomori.domain.usecase.StartSession
import com.pomori.testutil.FakeActiveSessionStore
import com.pomori.testutil.FakeClock
import com.pomori.testutil.FakeSessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private fun viewModel() = TimerViewModel(clock, store, startSession, endSession)

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
}
