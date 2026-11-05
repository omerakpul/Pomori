package com.pomori.ui.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pomori.R
import com.pomori.domain.model.ExceptionRule
import com.pomori.domain.model.InstalledApp
import com.pomori.ui.pomoriComposeRule
import com.pomori.ui.str
import com.pomori.ui.theme.PomoriTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val rule = pomoriComposeRule()

    private val calls = mutableListOf<String>()

    private val apps = listOf(
        InstalledApp("com.google.android.gm", "Gmail"),
        InstalledApp("com.whatsapp", "WhatsApp"),
    )

    private fun show(state: SettingsUiState = SettingsUiState(selectableApps = apps)) {
        rule.setContent {
            PomoriTheme {
                SettingsScreen(
                    state = state,
                    onRetentionDaysChange = { calls += "retention:$it" },
                    onAddSender = { calls += "sender:$it" },
                    onAddApp = { calls += "app:${it.packageName}" },
                    onRemoveRule = { calls += "remove:${it.value}" },
                    onBack = { calls += "back" },
                    onOpenPresets = { calls += "openPresets" },
                    onRestoreNotificationsChange = { calls += "restore:$it" },
                )
            }
        }
    }

    /** Liste tembel çizildiği için ekran dışındaki öğeye önce kaydırılır. */
    private fun scrollTo(tag: String) {
        rule.onNodeWithTag("settings_list").performScrollToNode(hasTestTag(tag))
    }

    // --- saklama süresi ---

    @Test
    fun showsTheCurrentRetention() {
        show(SettingsUiState(retentionDays = 14))
        scrollTo("settings_retention_inc")

        rule.onNodeWithTag("settings_retention_value").assertTextEquals(rule.str(R.string.settings_days_format, 14))
    }

    @Test
    fun retention_changesInStepsOfOneDay() {
        show(SettingsUiState(retentionDays = 7))
        scrollTo("settings_retention_inc")

        rule.onNodeWithTag("settings_retention_inc").performClick()
        rule.onNodeWithTag("settings_retention_dec").performClick()

        assertEquals(listOf("retention:8", "retention:6"), calls)
    }

    @Test
    fun theLowerLimit_disablesTheDecreaseButton() {
        show(SettingsUiState(retentionDays = 1))
        scrollTo("settings_retention_inc")

        rule.onNodeWithTag("settings_retention_dec").assertIsNotEnabled()
        rule.onNodeWithTag("settings_retention_inc").assertIsEnabled()
    }

    @Test
    fun theUpperLimit_disablesTheIncreaseButton() {
        show(SettingsUiState(retentionDays = 30))
        scrollTo("settings_retention_inc")

        rule.onNodeWithTag("settings_retention_inc").assertIsNotEnabled()
        rule.onNodeWithTag("settings_retention_dec").assertIsEnabled()
    }

    @Test
    fun aDisabledButton_doesNotReportAChange() {
        show(SettingsUiState(retentionDays = 1))
        scrollTo("settings_retention_inc")

        rule.onNodeWithTag("settings_retention_dec").performClick()

        assertEquals(emptyList<String>(), calls)
    }

    // --- modlar ---

    @Test
    fun presetsRow_showsTheCountAndTheActivePreset() {
        show(SettingsUiState(presetCount = 3, selectedPresetName = "Ders"))

        rule.onNodeWithTag("settings_presets_summary", useUnmergedTree = true)
            .assertTextEquals(rule.str(R.string.settings_presets_summary, 3, "Ders"))
    }

    @Test
    fun presetsRow_opensThePresetsScreen() {
        show(SettingsUiState(presetCount = 1, selectedPresetName = "Varsayılan"))

        rule.onNodeWithTag("settings_presets").performClick()

        assertEquals(listOf("openPresets"), calls)
    }

    @Test
    fun backButton_goesBack() {
        show()

        rule.onNodeWithTag("settings_back").performClick()

        assertEquals(listOf("back"), calls)
    }

    // --- istisna listesi ---

    @Test
    fun withoutRules_showsTheEmptyHint() {
        show()
        scrollTo("settings_rules_empty")

        rule.onNodeWithTag("settings_rules_empty").assertIsDisplayed()
    }

    @Test
    fun rules_areListedWithTheirNameAndType() {
        show(
            SettingsUiState(
                rules = listOf(
                    RuleItem(ExceptionRule.Package("com.whatsapp"), "WhatsApp"),
                    RuleItem(ExceptionRule.Sender("Anne"), "Anne"),
                ),
            ),
        )

        scrollTo("settings_remove_Anne")
        rule.onNodeWithText("WhatsApp").assertIsDisplayed()
        rule.onNodeWithText(rule.str(R.string.settings_rule_type_app)).assertIsDisplayed()
        rule.onNodeWithText("Anne").assertIsDisplayed()
        rule.onNodeWithText(rule.str(R.string.settings_rule_type_sender)).assertIsDisplayed()
        rule.onNodeWithTag("settings_rules_empty").assertDoesNotExist()
    }

    @Test
    fun removeButton_reportsTheRuleToRemove() {
        show(SettingsUiState(rules = listOf(RuleItem(ExceptionRule.Sender("Anne"), "Anne"))))

        scrollTo("settings_remove_Anne")
        rule.onNodeWithTag("settings_remove_Anne").performClick()

        assertEquals(listOf("remove:Anne"), calls)
    }

    // --- gönderen ekleme ---

    @Test
    fun addSender_isDisabledUntilSomethingIsTyped() {
        show()
        scrollTo("settings_sender_add")

        rule.onNodeWithTag("settings_sender_add").assertIsNotEnabled()
        rule.onNodeWithTag("settings_sender_input").performTextInput("   ")
        rule.onNodeWithTag("settings_sender_add").assertIsNotEnabled()
        rule.onNodeWithTag("settings_sender_input").performTextInput("Anne")
        rule.onNodeWithTag("settings_sender_add").assertIsEnabled()
    }

    @Test
    fun addSender_reportsTheNameAndClearsTheField() {
        show()
        scrollTo("settings_sender_add")

        rule.onNodeWithTag("settings_sender_input").performTextInput("Anne")
        rule.onNodeWithTag("settings_sender_add").performClick()

        assertEquals(listOf("sender:Anne"), calls)
        // Alan temizlendi: boş olduğu için düğme yeniden pasif.
        rule.onNodeWithTag("settings_sender_add").assertIsNotEnabled()
    }

    // --- uygulama seçici ---

    @Test
    fun appPicker_listsTheSelectableApps() {
        show()
        scrollTo("settings_add_app")

        rule.onNodeWithTag("settings_add_app").performClick()

        rule.onNodeWithText(rule.str(R.string.settings_pick_app_title)).assertIsDisplayed()
        rule.onNodeWithTag("settings_app_com.whatsapp").assertIsDisplayed()
        rule.onNodeWithTag("settings_app_com.google.android.gm").assertIsDisplayed()
    }

    @Test
    fun appPicker_searchFiltersIgnoringCase() {
        show()
        scrollTo("settings_add_app")
        rule.onNodeWithTag("settings_add_app").performClick()

        rule.onNodeWithTag("settings_app_search").performTextInput("WHATS")

        rule.onNodeWithTag("settings_app_com.whatsapp").assertIsDisplayed()
        rule.onNodeWithTag("settings_app_com.google.android.gm").assertDoesNotExist()
    }

    @Test
    fun appPicker_saysSoWhenNothingMatches() {
        show()
        scrollTo("settings_add_app")
        rule.onNodeWithTag("settings_add_app").performClick()

        rule.onNodeWithTag("settings_app_search").performTextInput("zzz")

        rule.onNodeWithText(rule.str(R.string.settings_pick_app_empty)).assertIsDisplayed()
    }

    @Test
    fun appPicker_pickingAnAppReportsItAndClosesTheDialog() {
        show()
        scrollTo("settings_add_app")
        rule.onNodeWithTag("settings_add_app").performClick()

        rule.onNodeWithTag("settings_app_com.whatsapp").performClick()

        assertEquals(listOf("app:com.whatsapp"), calls)
        rule.onNodeWithText(rule.str(R.string.settings_pick_app_title)).assertDoesNotExist()
    }

    @Test
    fun appPicker_canBeClosedWithoutPicking() {
        show()
        scrollTo("settings_add_app")
        rule.onNodeWithTag("settings_add_app").performClick()

        rule.onNodeWithText(rule.str(R.string.settings_close)).performClick()

        assertEquals(emptyList<String>(), calls)
        rule.onNodeWithText(rule.str(R.string.settings_pick_app_title)).assertDoesNotExist()
    }

    // --- erişilebilirlik ---

    @Test
    fun stepperButtons_haveAtLeast48dpTouchTargets() {
        show()

        listOf("retention").forEach { row ->
            scrollTo("settings_${row}_inc")
            listOf("dec", "inc").forEach { direction ->
                rule.onNodeWithTag("settings_${row}_$direction")
                    .assertWidthIsAtLeast(48.dp)
                    .assertHeightIsAtLeast(48.dp)
            }
        }
    }

    @Test
    fun stepperValues_stayOnOneLineWithTheLargestFonts() {
        rule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale = 2f)) {
                PomoriTheme {
                    SettingsScreen(
                        state = SettingsUiState(retentionDays = 14),
                        onRetentionDaysChange = {},
                        onAddSender = {},
                        onAddApp = {},
                        onRemoveRule = {},
                        onBack = {},
                    )
                }
            }
        }

        // "14 gün" ikiye bölünürse yüksekliği tek satırın yaklaşık iki katına çıkar.
        scrollTo("settings_retention_value")
        val value = rule.onNodeWithTag("settings_retention_value").getUnclippedBoundsInRoot()
        assertTrue("Değer birden çok satıra bölünmüş (yükseklik ${value.height})", value.height < 56.dp)
    }

    // --- bildirimleri geri getir ---

    @Test
    fun restoreSwitch_showsTheCurrentSetting() {
        show(SettingsUiState(restoreNotifications = true))

        rule.onNodeWithTag("settings_restore").assertIsOn()
    }

    @Test
    fun restoreSwitch_isOffWhenTheSettingIsOff() {
        show(SettingsUiState(restoreNotifications = false))

        rule.onNodeWithTag("settings_restore").assertIsOff()
    }

    @Test
    fun restoreSwitch_reportsTheNewValueWhenTapped() {
        show(SettingsUiState(restoreNotifications = true))

        rule.onNodeWithTag("settings_restore").performClick()

        assertEquals(listOf("restore:false"), calls)
    }

    @Test
    fun restoreSwitch_turnsOnWhenItWasOff() {
        show(SettingsUiState(restoreNotifications = false))

        rule.onNodeWithTag("settings_restore").performClick()

        assertEquals(listOf("restore:true"), calls)
    }

    @Test
    fun restoreSwitch_explainsWhatItDoes() {
        show(SettingsUiState())

        rule.onNodeWithText(rule.str(R.string.settings_restore_title)).assertIsDisplayed()
        rule.onNodeWithText(rule.str(R.string.settings_restore_body)).assertIsDisplayed()
    }
}
