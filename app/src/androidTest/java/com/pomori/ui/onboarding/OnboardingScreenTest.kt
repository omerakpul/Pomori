package com.pomori.ui.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pomori.R
import com.pomori.ui.pomoriComposeRule
import com.pomori.ui.str
import com.pomori.ui.theme.PomoriTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {

    @get:Rule
    val rule = pomoriComposeRule()

    private val calls = mutableListOf<String>()

    private fun show() {
        rule.setContent {
            PomoriTheme {
                OnboardingScreen(
                    onGrantAccess = { calls += "grant" },
                    onSkip = { calls += "skip" },
                )
            }
        }
    }

    @Test
    fun explainsWhyWhatAndPrivacyBeforeAskingForAnything() {
        show()

        listOf(
            R.string.onboarding_title,
            R.string.onboarding_why_title,
            R.string.onboarding_why_body,
            R.string.onboarding_data_title,
            R.string.onboarding_data_body,
            R.string.onboarding_privacy_title,
            R.string.onboarding_privacy_body,
        ).forEach { id ->
            rule.onNodeWithText(rule.str(id)).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun grantButton_asksForAccess() {
        show()

        rule.onNodeWithTag("onboarding_grant_access").performScrollTo().performClick()

        assertEquals(listOf("grant"), calls)
    }

    @Test
    fun skipButton_continuesWithoutAccess() {
        show()

        rule.onNodeWithTag("onboarding_skip").performScrollTo().performClick()

        assertEquals(listOf("skip"), calls)
    }

    @Test
    fun bothChoicesAreOffered() {
        show()

        rule.onNodeWithText(rule.str(R.string.onboarding_grant_access)).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText(rule.str(R.string.onboarding_skip)).performScrollTo().assertIsDisplayed()
    }
}
