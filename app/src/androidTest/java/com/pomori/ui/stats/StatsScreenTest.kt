package com.pomori.ui.stats

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pomori.R
import com.pomori.domain.model.DayFocus
import com.pomori.domain.model.Distractor
import com.pomori.ui.pomoriComposeRule
import com.pomori.ui.str
import com.pomori.ui.theme.PomoriTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class StatsScreenTest {

    @get:Rule
    val rule = pomoriComposeRule()

    private val minute = 60_000L
    private val calls = mutableListOf<String>()

    private fun show(state: StatsUiState) {
        rule.setContent { PomoriTheme { StatsScreen(state = state, onBack = { calls += "back" }) } }
    }

    private val populated = StatsUiState(
        isLoading = false,
        totalFocusMs = (2 * 60 + 30) * minute,
        completedSessions = 5,
        topDistractors = listOf(
            Distractor("com.whatsapp", "WhatsApp", 6),
            Distractor("com.google.android.gm", "Gmail", 3),
        ),
    )

    @Test
    fun theHeaderNamesThePeriod() {
        show(populated)

        rule.onNodeWithText(rule.str(R.string.stats_title)).assertIsDisplayed()
        rule.onNodeWithText(rule.str(R.string.stats_period)).assertIsDisplayed()
    }

    @Test
    fun loading_drawsNeitherTheContentNorTheEmptyState() {
        show(StatsUiState(isLoading = true))

        rule.onNodeWithTag("stats_content").assertDoesNotExist()
        rule.onNodeWithTag("stats_empty").assertDoesNotExist()
    }

    @Test
    fun withoutAnyData_showsTheEmptyState() {
        show(StatsUiState(isLoading = false))

        rule.onNodeWithTag("stats_empty").assertIsDisplayed()
        rule.onNodeWithText(rule.str(R.string.stats_empty_title)).assertIsDisplayed()
        rule.onNodeWithTag("stats_content").assertDoesNotExist()
    }

    @Test
    fun totals_showHoursAndMinutesAndTheSessionCount() {
        show(populated)

        rule.onNodeWithTag("stats_focus_time").assertTextEquals(rule.str(R.string.stats_duration_hm, 2, 30))
        rule.onNodeWithTag("stats_completed").assertTextEquals("5")
    }

    @Test
    fun anHourOrLess_showsOnlyMinutes() {
        show(populated.copy(totalFocusMs = 45 * minute))

        rule.onNodeWithTag("stats_focus_time").assertTextEquals(rule.str(R.string.stats_duration_m, 45))
    }

    @Test
    fun exactlyOneHour_showsHoursAndZeroMinutes() {
        show(populated.copy(totalFocusMs = 60 * minute))

        rule.onNodeWithTag("stats_focus_time").assertTextEquals(rule.str(R.string.stats_duration_hm, 1, 0))
    }

    @Test
    fun distractors_areListedWithTheirNotificationCounts() {
        show(populated)

        rule.onNodeWithTag("stats_distractor_com.whatsapp").assertIsDisplayed()
        rule.onNodeWithTag("stats_distractor_com.google.android.gm").assertIsDisplayed()
        // Satır tek bir erişilebilirlik cümlesi olarak sunulur (içindeki metinler ayrı düğüm değildir).
        rule.onNodeWithContentDescription("WhatsApp, " + rule.str(R.string.stats_notification_count, 6)).assertIsDisplayed()
        rule.onNodeWithContentDescription("Gmail, " + rule.str(R.string.stats_notification_count, 3)).assertIsDisplayed()
    }

    @Test
    fun eachDistractorRow_isReadAsOneSentenceByAccessibilityServices() {
        show(populated)

        val description = "WhatsApp, " + rule.str(R.string.stats_notification_count, 6)
        rule.onNodeWithContentDescription(description).assertIsDisplayed()
    }

    @Test
    fun sessionsWithoutNotifications_sayNoNotificationsWereCaptured() {
        show(populated.copy(topDistractors = emptyList()))

        rule.onNodeWithTag("stats_distractors_empty").assertIsDisplayed()
        rule.onNodeWithTag("stats_completed").assertTextEquals("5")
        rule.onNodeWithTag("stats_empty").assertDoesNotExist()
    }

    @Test
    fun notificationsWithoutSessions_stillShowTheContent() {
        show(populated.copy(totalFocusMs = 0, completedSessions = 0))

        rule.onNodeWithTag("stats_content").assertIsDisplayed()
        rule.onNodeWithTag("stats_focus_time").assertTextEquals(rule.str(R.string.stats_duration_m, 0))
        rule.onNodeWithTag("stats_empty").assertDoesNotExist()
    }

    @Test
    fun backButton_goesBack() {
        show(populated)

        // Geri artık simgeli bir düğme: metni yok, ekran okuyucu için açıklaması var.
        rule.onNodeWithContentDescription(rule.str(R.string.action_back)).performClick()

        assertEquals(listOf("back"), calls)
    }

    // --- günlük grafik ---

    private val today = LocalDate.of(2026, 11, 4)
    private val dailyMinutes = listOf(50L, 100L, 0L, 75L, 125L, 25L, 155L)
    private val week = dailyMinutes.mapIndexed { i, m -> DayFocus(today.minusDays((6 - i).toLong()), m * minute) }

    private fun barDescription(index: Int, durationText: String): String {
        val dayName = week[index].date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        return rule.str(R.string.stats_bar_description, dayName, durationText)
    }

    @Test
    fun chart_showsAnEntryForEachOfTheSevenDays() {
        show(populated.copy(dailyFocus = week))

        rule.onNodeWithTag("stats_chart").assertIsDisplayed()
        repeat(7) { rule.onNodeWithTag("stats_bar_$it").assertExists() }
        rule.onNodeWithText(rule.str(R.string.stats_chart_title)).assertIsDisplayed()
    }

    @Test
    fun chart_barsAreReadAsDayAndDuration() {
        show(populated.copy(dailyFocus = week))

        // 155 dk = 2 sa 35 dk; 0 olan gün de "0 dk" diye okunur.
        rule.onNodeWithContentDescription(barDescription(6, rule.str(R.string.stats_duration_hm, 2, 35))).assertExists()
        rule.onNodeWithContentDescription(barDescription(2, rule.str(R.string.stats_duration_m, 0))).assertExists()
        rule.onNodeWithContentDescription(barDescription(0, rule.str(R.string.stats_duration_m, 50))).assertExists()
    }

    @Test
    fun chart_isHiddenWhenNoDayHasFocusTime() {
        show(populated.copy(dailyFocus = week.map { it.copy(focusMs = 0) }))

        rule.onNodeWithTag("stats_chart").assertDoesNotExist()
        rule.onNodeWithText(rule.str(R.string.stats_chart_title)).assertDoesNotExist()
    }

    @Test
    fun chart_isHiddenWithoutDailyData() {
        show(populated)

        rule.onNodeWithTag("stats_chart").assertDoesNotExist()
    }
}
