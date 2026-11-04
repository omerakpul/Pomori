package com.pomori.ui.stats

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.pomori.data.repository.PresetRepositoryImpl
import com.pomori.domain.model.CapturedNotification
import com.pomori.domain.model.Preset
import com.pomori.testutil.FakePresetDao
import com.pomori.domain.model.FocusSession
import com.pomori.domain.model.SessionStatus
import com.pomori.domain.model.SessionType
import com.pomori.domain.usecase.GetGoalProgress
import com.pomori.domain.usecase.GetWeeklyStats
import com.pomori.testutil.FakeSettingsStore
import com.pomori.testutil.FakeClock
import com.pomori.testutil.FakeNotificationRepository
import com.pomori.testutil.FakeSessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    private val day = 24 * 60 * 60 * 1000L
    private val minute = 60 * 1000L
    private val now = 100 * day
    private val sessions = FakeSessionRepository()
    private val notifications = FakeNotificationRepository()
    private val presets = PresetRepositoryImpl(FakePresetDao())

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = StatsViewModel(
        GetWeeklyStats(sessions, notifications, FakeClock(nowMs = now)),
        GetGoalProgress(sessions, FakeSettingsStore(), FakeClock(nowMs = now)),
        presets,
    )

    private suspend fun completedFocus(minutes: Long) {
        sessions.insert(
            FocusSession(
                type = SessionType.FOCUS,
                startedAt = now - day,
                plannedDurationMs = minutes * minute,
                endedAt = now - day + minutes * minute,
                status = SessionStatus.COMPLETED,
            ),
        )
    }

    private suspend fun captured(pkg: String, label: String) {
        notifications.insert(
            CapturedNotification(
                sessionId = 1,
                packageName = pkg,
                appLabel = label,
                title = null,
                text = null,
                postedAt = now - day,
            ),
        )
    }

    /** Yükleme durumunu atlayıp ilk gerçek durumu verir. */
    private suspend fun ReceiveTurbine<StatsUiState>.awaitLoaded(): StatsUiState {
        var state = awaitItem()
        while (state.isLoading) state = awaitItem()
        return state
    }

    @Test
    fun `the initial state is loading and not yet empty`() {
        val initial = StatsUiState()

        assertTrue(initial.isLoading)
        assertFalse(initial.isEmpty)
    }

    @Test
    fun `the breakdown names modes and merges unknown ones into other`() = runTest {
        val study = presets.add(Preset(name = "Ders", focusMinutes = 40, breakMinutes = 5, longBreakMinutes = 15))
        val code = presets.add(Preset(name = "Kod", focusMinutes = 25, breakMinutes = 5, longBreakMinutes = 15))
        fun add(presetId: Long?, minutes: Long) = FocusSession(
            type = SessionType.FOCUS,
            startedAt = now - day,
            plannedDurationMs = minutes * minute,
            endedAt = now - day + minutes * minute,
            status = SessionStatus.COMPLETED,
            presetId = presetId,
        )
        sessions.insert(add(study, 40))
        sessions.insert(add(code, 60))
        sessions.insert(add(null, 10))
        sessions.insert(add(999, 5)) // silinmiş mod

        viewModel().uiState.test {
            val state = awaitLoaded()
            assertEquals(listOf<String?>("Kod", "Ders", null), state.modeBreakdown.map { it.name })
            assertEquals(15 * minute, state.modeBreakdown.last().focusMs)
        }
    }

    @Test
    fun `without data the loaded state is empty`() = runTest {
        viewModel().uiState.test {
            val state = awaitLoaded()
            assertTrue(state.isEmpty)
            assertEquals(0, state.completedSessions)
        }
    }

    @Test
    fun `the ui state carries totals and distractors`() = runTest {
        completedFocus(25)
        completedFocus(50)
        repeat(3) { captured("com.whatsapp", "WhatsApp") }
        captured("com.gmail", "Gmail")

        viewModel().uiState.test {
            val state = awaitLoaded()
            assertFalse(state.isEmpty)
            assertEquals(75 * minute, state.totalFocusMs)
            assertEquals(2, state.completedSessions)
            assertEquals(listOf("WhatsApp", "Gmail"), state.topDistractors.map { it.appLabel })
            assertEquals(3, state.maxDistractorCount)
        }
    }

    @Test
    fun `sessions without notifications are not empty`() = runTest {
        completedFocus(25)

        viewModel().uiState.test {
            val state = awaitLoaded()
            assertFalse(state.isEmpty)
            assertTrue(state.topDistractors.isEmpty())
            assertEquals(0, state.maxDistractorCount)
        }
    }

    @Test
    fun `notifications without completed sessions are not empty either`() = runTest {
        captured("com.whatsapp", "WhatsApp")

        viewModel().uiState.test {
            assertFalse(awaitLoaded().isEmpty)
        }
    }

    @Test
    fun `the state updates live when a session completes`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            assertTrue(awaitLoaded().isEmpty)

            completedFocus(25)

            assertEquals(1, awaitItem().completedSessions)
        }
    }
}
