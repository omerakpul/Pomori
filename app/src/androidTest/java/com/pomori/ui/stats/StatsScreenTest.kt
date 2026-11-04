package com.pomori.ui.stats

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pomori.R
import com.pomori.domain.model.Distractor
import com.pomori.ui.pomoriComposeRule
import com.pomori.ui.str
import com.pomori.ui.theme.PomoriTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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

        rule.onNode(hasText(rule.str(R.string.stats_back))).performClick()

        assertEquals(listOf("back"), calls)
    }
}
