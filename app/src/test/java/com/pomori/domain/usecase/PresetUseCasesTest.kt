package com.pomori.domain.usecase

import app.cash.turbine.test
import com.pomori.data.repository.PresetRepositoryImpl
import com.pomori.data.repository.toEntity
import com.pomori.domain.model.Preset
import com.pomori.testutil.FakePresetDao
import com.pomori.testutil.FakeSettingsStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PresetUseCasesTest {

    private val presetDao = FakePresetDao()
    private val presets = PresetRepositoryImpl(presetDao)

    // --- EnsureDefaultPreset ---

    @Test
    fun `the first launch creates the default preset from the old settings and selects it`() = runTest {
        val settings = FakeSettingsStore(focusMinutes = 40, breakMinutes = 8, longBreakMinutes = 20)

        EnsureDefaultPreset(presets, settings)()

        val created = presets.observeAll().first().single()
        assertEquals(Preset.DEFAULT_NAME, created.name)
        assertEquals(40, created.focusMinutes)
        assertEquals(8, created.breakMinutes)
        assertEquals(20, created.longBreakMinutes)
        assertEquals(created.id, settings.selectedPresetId.first())
    }

    @Test
    fun `nothing happens when a preset already exists`() = runTest {
        val settings = FakeSettingsStore()
        presets.add(Preset(name = "Ders", focusMinutes = 40, breakMinutes = 5, longBreakMinutes = 15))

        EnsureDefaultPreset(presets, settings)()

        assertEquals(listOf("Ders"), presets.observeAll().first().map { it.name })
        assertNull(settings.selectedPresetId.first())
    }

    @Test
    fun `calling it twice does not create a second default`() = runTest {
        val settings = FakeSettingsStore()
        val ensure = EnsureDefaultPreset(presets, settings)

        ensure()
        ensure()

        assertEquals(1, presets.count())
    }

    // --- ObserveSelectedPreset ---

    private fun preset(id: Long, name: String) = Preset(id, name, 25, 5, 15).toEntity()

    @Test
    fun `the stored selection is used`() = runTest {
        val dao = FakePresetDao(listOf(preset(1, "A"), preset(2, "B")))

        ObserveSelectedPreset(PresetRepositoryImpl(dao), FakeSettingsStore(selectedPresetId = 2))().test {
            assertEquals("B", awaitItem()?.name)
        }
    }

    @Test
    fun `without a stored selection the first preset is used`() = runTest {
        val dao = FakePresetDao(listOf(preset(1, "A"), preset(2, "B")))

        ObserveSelectedPreset(PresetRepositoryImpl(dao), FakeSettingsStore())().test {
            assertEquals("A", awaitItem()?.name)
        }
    }

    @Test
    fun `a selection that points to a deleted preset falls back to the first one`() = runTest {
        val dao = FakePresetDao(listOf(preset(1, "A"), preset(2, "B")))

        ObserveSelectedPreset(PresetRepositoryImpl(dao), FakeSettingsStore(selectedPresetId = 99))().test {
            assertEquals("A", awaitItem()?.name)
        }
    }

    @Test
    fun `there is no selected preset before any preset exists`() = runTest {
        ObserveSelectedPreset(presets, FakeSettingsStore())().test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun `selecting another preset and deleting the selected one are both reflected`() = runTest {
        val dao = FakePresetDao(listOf(preset(1, "A"), preset(2, "B")))
        val settings = FakeSettingsStore()
        val repo = PresetRepositoryImpl(dao)

        ObserveSelectedPreset(repo, settings)().test {
            assertEquals("A", awaitItem()?.name)

            settings.setSelectedPresetId(2)
            assertEquals("B", awaitItem()?.name)

            repo.delete(2)
            assertEquals("A", awaitItem()?.name)
        }
    }
}
