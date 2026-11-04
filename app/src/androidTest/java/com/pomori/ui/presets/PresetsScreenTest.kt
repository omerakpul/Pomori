package com.pomori.ui.presets

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pomori.R
import com.pomori.domain.model.Preset
import com.pomori.ui.pomoriComposeRule
import com.pomori.ui.str
import com.pomori.ui.theme.PomoriTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PresetsScreenTest {

    @get:Rule
    val rule = pomoriComposeRule()

    private val saved = mutableListOf<Preset>()
    private val deleted = mutableListOf<Long>()
    private val calls = mutableListOf<String>()

    private val varsayilan = Preset(1, "Varsayılan", 25, 5, 15, 4)
    private val ders = Preset(2, "Ders", 40, 5, 15, 4)
    private val hizli = Preset(3, "Hızlı", 20, 2, 10, 3)

    private fun show(state: PresetsUiState) {
        rule.setContent {
            PomoriTheme {
                PresetsScreen(
                    state = state,
                    onSave = { saved += it },
                    onDelete = { deleted += it },
                    onBack = { calls += "back" },
                )
            }
        }
    }

    private fun populated(selected: Long = 1) =
        PresetsUiState(isLoading = false, presets = listOf(varsayilan, ders, hizli), selectedId = selected)

    // --- liste ---

    @Test
    fun loading_drawsNeitherTheListNorTheAddButton() {
        show(PresetsUiState(isLoading = true))

        rule.onNodeWithTag("presets_list").assertDoesNotExist()
        rule.onNodeWithTag("presets_add").assertDoesNotExist()
    }

    @Test
    fun everyPresetIsListedWithItsName() {
        show(populated())

        rule.onNodeWithText("Varsayılan").assertIsDisplayed()
        rule.onNodeWithText("Ders").assertIsDisplayed()
        rule.onNodeWithText("Hızlı").assertIsDisplayed()
    }

    @Test
    fun theSummaryShowsTheThreeDurations() {
        show(populated())

        rule.onNodeWithText(rule.str(R.string.presets_summary, 40, 5, 15)).assertIsDisplayed()
        rule.onNodeWithText(rule.str(R.string.presets_summary, 20, 2, 10)).assertIsDisplayed()
    }

    @Test
    fun onlyTheSelectedPresetIsMarkedActive() {
        show(populated(selected = 2))

        // Kartlar tıklanabilir olduğu için alt öğeler birleşir; etiketi görmek için birleşmemiş ağaca bakılır.
        rule.onNodeWithTag("preset_active_2", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithTag("preset_active_1", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithTag("preset_active_3", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun backButton_goesBack() {
        show(populated())

        rule.onNodeWithTag("presets_back").performClick()

        assertEquals(listOf("back"), calls)
    }

    // --- düzenleme ---

    @Test
    fun tappingAPreset_opensTheEditorWithItsValues() {
        show(populated())

        rule.onNodeWithTag("preset_card_2").performClick()

        rule.onNodeWithText(rule.str(R.string.preset_editor_edit)).assertIsDisplayed()
        rule.onNodeWithTag("preset_name_input").assertTextEquals(rule.str(R.string.preset_name_label), "Ders")
        rule.onNodeWithTag("preset_focus_value").assertTextEquals(rule.str(R.string.settings_minutes_format, 40))
        rule.onNodeWithTag("preset_break_value").assertTextEquals(rule.str(R.string.settings_minutes_format, 5))
        rule.onNodeWithTag("preset_long_break_value").assertTextEquals(rule.str(R.string.settings_minutes_format, 15))
        rule.onNodeWithTag("preset_every_value").assertTextEquals(rule.str(R.string.preset_every_value, 4))
    }

    @Test
    fun saving_reportsTheEditedPresetAndClosesTheEditor() {
        show(populated())
        rule.onNodeWithTag("preset_card_2").performClick()

        rule.onNodeWithTag("preset_focus_inc").performClick()
        rule.onNodeWithTag("preset_break_inc").performClick()
        rule.onNodeWithTag("preset_long_break_dec").performClick()
        rule.onNodeWithTag("preset_every_inc").performClick()
        rule.onNodeWithTag("preset_save").performScrollTo().performClick()

        assertEquals(listOf(ders.copy(focusMinutes = 45, breakMinutes = 6, longBreakMinutes = 10, longBreakEvery = 5)), saved)
        rule.onNodeWithTag("preset_editor").assertDoesNotExist()
    }

    @Test
    fun theName_canBeChanged() {
        show(populated())
        rule.onNodeWithTag("preset_card_2").performClick()

        rule.onNodeWithTag("preset_name_input").performTextReplacement("Ders 2")
        rule.onNodeWithTag("preset_save").performScrollTo().performClick()

        assertEquals("Ders 2", saved.single().name)
    }

    @Test
    fun closingTheEditorWithoutSaving_changesNothing() {
        show(populated())
        rule.onNodeWithTag("preset_card_2").performClick()
        rule.onNodeWithTag("preset_focus_inc").performClick()

        Espresso.pressBack()
        rule.waitForIdle()

        assertEquals(emptyList<Preset>(), saved)
        rule.onNodeWithTag("preset_editor").assertDoesNotExist()
    }

    @Test
    fun theEditorStartsFromTheStoredValuesEachTime() {
        show(populated())
        rule.onNodeWithTag("preset_card_2").performClick()
        rule.onNodeWithTag("preset_focus_inc").performClick()
        Espresso.pressBack()
        rule.waitForIdle()

        rule.onNodeWithTag("preset_card_2").performClick()

        // Vazgeçilen değişiklik taslakta kalmaz.
        rule.onNodeWithTag("preset_focus_value").assertTextEquals(rule.str(R.string.settings_minutes_format, 40))
    }

    // --- yeni mod ---

    @Test
    fun addButton_opensAnEmptyEditorWithTheDefaultDurations() {
        show(populated())

        rule.onNodeWithTag("presets_add").performClick()

        rule.onNodeWithText(rule.str(R.string.preset_editor_new)).assertIsDisplayed()
        rule.onNodeWithTag("preset_focus_value").assertTextEquals(rule.str(R.string.settings_minutes_format, 25))
        rule.onNodeWithTag("preset_every_value").assertTextEquals(rule.str(R.string.preset_every_value, 4))
    }

    @Test
    fun aNewPreset_cannotBeSavedWithoutAName() {
        show(populated())
        rule.onNodeWithTag("presets_add").performClick()

        rule.onNodeWithTag("preset_save").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("preset_name_input").performTextInput("   ")
        rule.onNodeWithTag("preset_save").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("preset_name_input").performTextInput("Okuma")
        rule.onNodeWithTag("preset_save").performScrollTo().assertIsEnabled()
    }

    @Test
    fun aNewPreset_isReportedWithoutAnIdAndWithTheEditedValues() {
        show(populated())
        rule.onNodeWithTag("presets_add").performClick()

        rule.onNodeWithTag("preset_name_input").performTextInput("Okuma")
        rule.onNodeWithTag("preset_focus_inc").performClick()
        rule.onNodeWithTag("preset_save").performScrollTo().performClick()

        val preset = saved.single()
        assertEquals(0L, preset.id) // id 0: henüz kayıtlı değil, depo ekler
        assertEquals("Okuma", preset.name)
        assertEquals(30, preset.focusMinutes)
        assertEquals(5, preset.breakMinutes)
        assertEquals(15, preset.longBreakMinutes)
        assertEquals(4, preset.longBreakEvery)
    }

    @Test
    fun theName_isLimitedToTheMaximumLength() {
        show(populated())
        rule.onNodeWithTag("presets_add").performClick()

        rule.onNodeWithTag("preset_name_input").performTextInput("x".repeat(40))
        rule.onNodeWithTag("preset_save").performScrollTo().performClick()

        assertEquals(Preset.NAME_MAX_LENGTH, saved.single().name.length)
    }

    // --- silme ---

    @Test
    fun delete_reportsThePresetAndClosesTheEditor() {
        show(populated())
        rule.onNodeWithTag("preset_card_3").performClick()

        rule.onNodeWithTag("preset_delete").performScrollTo().performClick()

        assertEquals(listOf(3L), deleted)
        rule.onNodeWithTag("preset_editor").assertDoesNotExist()
    }

    @Test
    fun delete_isHiddenForTheLastRemainingPreset() {
        show(PresetsUiState(isLoading = false, presets = listOf(varsayilan), selectedId = 1))
        rule.onNodeWithTag("preset_card_1").performClick()

        rule.onNodeWithTag("preset_delete").assertDoesNotExist()
    }

    @Test
    fun delete_isHiddenWhenCreatingANewPreset() {
        show(populated())
        rule.onNodeWithTag("presets_add").performClick()

        rule.onNodeWithTag("preset_delete").assertDoesNotExist()
    }

    // --- sınırlar ---

    @Test
    fun theLimits_disableTheMatchingButtons() {
        show(PresetsUiState(isLoading = false, presets = listOf(Preset(1, "Sınır", 1, 1, 1, 2)), selectedId = 1))
        rule.onNodeWithTag("preset_card_1").performClick()

        rule.onNodeWithTag("preset_focus_dec").assertIsNotEnabled()
        rule.onNodeWithTag("preset_break_dec").assertIsNotEnabled()
        rule.onNodeWithTag("preset_long_break_dec").assertIsNotEnabled()
        rule.onNodeWithTag("preset_every_dec").assertIsNotEnabled()
        rule.onNodeWithTag("preset_focus_inc").assertIsEnabled()
    }
}
