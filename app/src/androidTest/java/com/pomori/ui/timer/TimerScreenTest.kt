package com.pomori.ui.timer

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pomori.R
import com.pomori.ui.permission.TAG_ACCESS_WARNING
import com.pomori.ui.pomoriComposeRule
import com.pomori.ui.str
import com.pomori.ui.theme.PomoriTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimerScreenTest {

    @get:Rule
    val rule = pomoriComposeRule()

    private val minute = 60_000L

    /** Tıklamaları sayar, böylece hangi düğmenin hangi geri çağrıyı tetiklediği doğrulanabilir. */
    private class Calls {
        val log = mutableListOf<String>()
    }

    private fun show(
        state: TimerUiState,
        calls: Calls = Calls(),
        accessGranted: Boolean = true,
    ): Calls {
        rule.setContent {
            PomoriTheme {
                TimerScreen(
                    state = state,
                    onStartFocus = { calls.log += "startFocus" },
                    onCancelFocus = { calls.log += "cancelFocus" },
                    onStartBreak = { calls.log += "startBreak" },
                    onSkipBreak = { calls.log += "skipBreak" },
                    notificationAccessGranted = accessGranted,
                    onOpenNotificationAccess = { calls.log += "openAccess" },
                    onOpenSettings = { calls.log += "openSettings" },
                    onOpenStats = { calls.log += "openStats" },
                )
            }
        }
        return calls
    }

    private val idle = TimerUiState(TimerPhase.IDLE, 25 * minute, 25 * minute)
    private val focus = TimerUiState(TimerPhase.FOCUS, 24 * minute + 52_000, 25 * minute)
    private val breakPending = TimerUiState(TimerPhase.BREAK_PENDING, 0, 5 * minute)
    private val onBreak = TimerUiState(TimerPhase.BREAK, 3 * minute + 5_000, 5 * minute)

    // --- fazlar ve metinler ---

    @Test
    fun idle_showsReadyLabelFullTimeAndOnlyTheStartButton() {
        show(idle)

        rule.onNodeWithTag(TAG_PHASE).assertTextEquals(rule.str(R.string.phase_idle))
        rule.onNodeWithTag(TAG_TIME, useUnmergedTree = true).assertTextEquals("25:00")
        rule.onNodeWithTag(TAG_START_FOCUS).assertIsDisplayed()
        rule.onNodeWithTag(TAG_CANCEL).assertDoesNotExist()
        rule.onNodeWithTag(TAG_START_BREAK).assertDoesNotExist()
        rule.onNodeWithTag(TAG_SKIP).assertDoesNotExist()
    }

    @Test
    fun focus_showsRemainingTimeAndOnlyTheCancelButton() {
        show(focus)

        rule.onNodeWithTag(TAG_PHASE).assertTextEquals(rule.str(R.string.phase_focus))
        rule.onNodeWithTag(TAG_TIME, useUnmergedTree = true).assertTextEquals("24:52")
        rule.onNodeWithTag(TAG_CANCEL).assertIsDisplayed()
        rule.onNodeWithTag(TAG_START_FOCUS).assertDoesNotExist()
        rule.onNodeWithTag(TAG_SKIP).assertDoesNotExist()
    }

    @Test
    fun breakPending_showsBreakTimeLabelAndStartAndSkip() {
        show(breakPending)

        rule.onNodeWithTag(TAG_PHASE).assertTextEquals(rule.str(R.string.phase_break_pending))
        rule.onNodeWithTag(TAG_TIME, useUnmergedTree = true).assertTextEquals(rule.str(R.string.time_break_pending))
        rule.onNodeWithTag(TAG_START_BREAK).assertIsDisplayed()
        rule.onNodeWithTag(TAG_SKIP).assertIsDisplayed()
        rule.onNodeWithTag(TAG_START_FOCUS).assertDoesNotExist()
        rule.onNodeWithTag(TAG_CANCEL).assertDoesNotExist()
    }

    @Test
    fun breakRunning_showsRemainingTimeAndOnlySkip() {
        show(onBreak)

        rule.onNodeWithTag(TAG_PHASE).assertTextEquals(rule.str(R.string.phase_break))
        rule.onNodeWithTag(TAG_TIME, useUnmergedTree = true).assertTextEquals("03:05")
        rule.onNodeWithTag(TAG_SKIP).assertIsDisplayed()
        rule.onNodeWithTag(TAG_START_BREAK).assertDoesNotExist()
        rule.onNodeWithTag(TAG_CANCEL).assertDoesNotExist()
    }

    @Test
    fun theLastMillisecondStillShowsOneSecondNotZero() {
        show(TimerUiState(TimerPhase.FOCUS, remainingMs = 1, totalMs = 25 * minute))

        rule.onNodeWithTag(TAG_TIME, useUnmergedTree = true).assertTextEquals("00:01")
    }

    @Test
    fun theRingIsDescribedToAccessibilityServicesAsOneSentence() {
        show(focus)

        val description = rule.str(R.string.timer_content_description, rule.str(R.string.phase_focus), "24:52")
        rule.onNodeWithContentDescription(description).assertIsDisplayed()
    }

    // --- düğmeler ---

    @Test
    fun startButton_startsFocus() {
        val calls = show(idle)

        rule.onNodeWithTag(TAG_START_FOCUS).performClick()

        assertEquals(listOf("startFocus"), calls.log)
    }

    @Test
    fun cancelButton_cancelsFocus() {
        val calls = show(focus)

        rule.onNodeWithTag(TAG_CANCEL).performClick()

        assertEquals(listOf("cancelFocus"), calls.log)
    }

    @Test
    fun breakPendingButtons_startOrSkipTheBreak() {
        val calls = show(breakPending)

        rule.onNodeWithTag(TAG_START_BREAK).performClick()
        rule.onNodeWithTag(TAG_SKIP).performClick()

        assertEquals(listOf("startBreak", "skipBreak"), calls.log)
    }

    @Test
    fun skipButton_skipsARunningBreak() {
        val calls = show(onBreak)

        rule.onNodeWithTag(TAG_SKIP).performClick()

        assertEquals(listOf("skipBreak"), calls.log)
    }

    // --- ayarlar ve istatistik girişi ---

    @Test
    fun settingsAndStatsEntries_openTheirScreensWhenIdle() {
        val calls = show(idle)

        rule.onNodeWithTag(TAG_STATS).performClick()
        rule.onNodeWithTag(TAG_SETTINGS).performClick()

        assertEquals(listOf("openStats", "openSettings"), calls.log)
    }

    // Seans sırasında ayarlar ve istatistik dikkat dağıtmasın, süre de ortada değişmesin.

    private fun assertEntriesHidden() {
        rule.onNodeWithTag(TAG_SETTINGS).assertDoesNotExist()
        rule.onNodeWithTag(TAG_STATS).assertDoesNotExist()
    }

    @Test
    fun settingsAndStatsEntries_areHiddenWhileFocusing() {
        show(focus)
        assertEntriesHidden()
    }

    @Test
    fun settingsAndStatsEntries_areHiddenWhileTheBreakIsPending() {
        show(breakPending)
        assertEntriesHidden()
    }

    @Test
    fun settingsAndStatsEntries_areHiddenDuringTheBreak() {
        show(onBreak)
        assertEntriesHidden()
    }

    // --- bildirim erişimi uyarısı ---

    @Test
    fun accessWarning_isHiddenWhenAccessIsGranted() {
        show(idle, accessGranted = true)

        rule.onNodeWithTag(TAG_ACCESS_WARNING).assertDoesNotExist()
    }

    @Test
    fun accessWarning_showsWhenIdleAndAccessIsOff() {
        show(idle, accessGranted = false)

        rule.onNodeWithTag(TAG_ACCESS_WARNING).assertIsDisplayed()
        rule.onNodeWithText(rule.str(R.string.access_warning_title)).assertIsDisplayed()
    }

    @Test
    fun accessWarning_showsWhileFocusingAndAccessIsOff() {
        show(focus, accessGranted = false)

        rule.onNodeWithTag(TAG_ACCESS_WARNING).assertIsDisplayed()
    }

    @Test
    fun accessWarning_isHiddenDuringBreaksEvenWhenAccessIsOff() {
        // Yakalama yalnızca odak sırasında anlamlı; mola ekranını gereksiz uyarıyla kalabalıklaştırma.
        show(breakPending, accessGranted = false)

        rule.onNodeWithTag(TAG_ACCESS_WARNING).assertDoesNotExist()
    }

    @Test
    fun accessWarningAction_opensTheNotificationAccessSettings() {
        val calls = show(idle, accessGranted = false)

        rule.onNode(hasText(rule.str(R.string.access_warning_action))).performClick()

        assertEquals(listOf("openAccess"), calls.log)
    }

    // --- büyük yazı tipi ve düzen ---

    /** Sistem yazı tipi ölçeğini taklit eder (örneğin 2f = en büyük erişilebilirlik ayarı). */
    private fun showScaled(state: TimerUiState, fontScale: Float, accessGranted: Boolean) {
        rule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale = fontScale)) {
                PomoriTheme {
                    TimerScreen(
                        state = state,
                        onStartFocus = {},
                        onCancelFocus = {},
                        onStartBreak = {},
                        onSkipBreak = {},
                        notificationAccessGranted = accessGranted,
                    )
                }
            }
        }
    }

    private fun assertCardDoesNotOverlapTheLabel() {
        val card = rule.onNodeWithTag(TAG_ACCESS_WARNING).getUnclippedBoundsInRoot()
        val label = rule.onNodeWithTag(TAG_PHASE).getUnclippedBoundsInRoot()
        assertTrue("Uyarı kartı (alt: ${card.bottom}) faz etiketinin (üst: ${label.top}) üstüne biniyor", card.bottom <= label.top)
    }

    @Test
    fun accessWarning_doesNotOverlapThePhaseLabel() {
        showScaled(idle, fontScale = 1f, accessGranted = false)

        assertCardDoesNotOverlapTheLabel()
    }

    @Test
    fun accessWarning_doesNotOverlapThePhaseLabelWithTheLargestFonts() {
        // Kart bir zamanlar sayacın üstüne bağımsız bir katmandı; yazı büyüyünce etiketin üstüne biniyordu.
        showScaled(idle, fontScale = 2f, accessGranted = false)

        assertCardDoesNotOverlapTheLabel()
    }

    @Test
    fun withTheLargestFonts_everyControlIsStillReachable() {
        showScaled(idle, fontScale = 2f, accessGranted = false)

        // Sığmayan içerik kaydırılır: düğmelere kaydırılarak ulaşılabilir, hiçbiri kesilip kaybolmaz.
        rule.onNodeWithTag(TAG_START_FOCUS).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun breakPendingLabel_fitsInsideTheRingAtNormalFontSize() {
        showScaled(breakPending, fontScale = 1f, accessGranted = true)

        // "Mola zamanı" halkaya (280 dp) sığar: iki satıra bölünüp taşmaz.
        val ring = rule.onNodeWithContentDescription(
            rule.str(R.string.timer_content_description, rule.str(R.string.phase_break_pending), rule.str(R.string.time_break_pending)),
        ).getUnclippedBoundsInRoot()
        val label = rule.onNodeWithTag(TAG_TIME, useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("Etiket halkadan geniş", label.width <= ring.width)
        assertTrue("Etiket halkadan yüksek", label.height <= ring.height)
        assertTrue("Etiket tek satır olmalı", label.height < 56.dp)
    }

    // --- uzun mola ve döngü noktaları ---

    private val longBreakPending =
        TimerUiState(TimerPhase.BREAK_PENDING, 0, 15 * minute, isLongBreak = true, cycleDone = 0)
    private val longBreakRunning =
        TimerUiState(TimerPhase.BREAK, 14 * minute, 15 * minute, isLongBreak = true, cycleDone = 0)

    @Test
    fun longBreakPending_namesTheLongBreakAndOffersToStartIt() {
        val calls = show(longBreakPending)

        rule.onNodeWithTag(TAG_TIME, useUnmergedTree = true).assertTextEquals(rule.str(R.string.time_long_break_pending))
        rule.onNodeWithContentDescription(rule.str(R.string.action_start_long_break)).performClick()

        assertEquals(listOf("startBreak"), calls.log)
    }

    @Test
    fun shortBreakPending_stillOffersTheNormalBreak() {
        show(breakPending)

        rule.onNodeWithContentDescription(rule.str(R.string.action_start_break)).assertIsDisplayed()
        rule.onNodeWithContentDescription(rule.str(R.string.action_start_long_break)).assertDoesNotExist()
    }

    @Test
    fun longBreakRunning_showsTheLongBreakLabelAndCaption() {
        show(longBreakRunning)

        rule.onNodeWithTag(TAG_PHASE).assertTextEquals(rule.str(R.string.phase_long_break))
        rule.onNodeWithText(rule.str(R.string.timer_caption_long_break, 15)).assertIsDisplayed()
    }

    @Test
    fun cycleDots_countTheFinishedFocusSessions() {
        show(focus.copy(cycleDone = 2))

        rule.onNodeWithContentDescription(rule.str(R.string.timer_cycle_description, 2, 4)).assertIsDisplayed()
        rule.onNodeWithTag(TAG_CYCLE).assertIsDisplayed()
    }

    @Test
    fun cycleDots_areFullWhileALongBreakIsEarned() {
        show(longBreakPending)

        rule.onNodeWithContentDescription(rule.str(R.string.timer_cycle_description, 4, 4)).assertIsDisplayed()
    }

    @Test
    fun cycleDots_startOverAfterTheLongBreak() {
        show(idle.copy(cycleDone = 0))

        rule.onNodeWithContentDescription(rule.str(R.string.timer_cycle_description, 0, 4)).assertIsDisplayed()
    }
}
