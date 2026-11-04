package com.pomori.ui.presets

import app.cash.turbine.test
import com.pomori.data.repository.PresetRepositoryImpl
import com.pomori.data.repository.toEntity
import com.pomori.domain.model.Preset
import com.pomori.domain.usecase.ObserveSelectedPreset
import com.pomori.testutil.FakePresetDao
import com.pomori.testutil.FakeSettingsStore
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
class PresetsViewModelTest {

    private val settings = FakeSettingsStore()
    private var dao = FakePresetDao()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(vararg initial: Preset): PresetsViewModel {
        dao = FakePresetDao(initial.map { it.toEntity() })
        val repo = PresetRepositoryImpl(dao)
        return PresetsViewModel(repo, settings, ObserveSelectedPreset(repo, settings))
    }

    private fun preset(id: Long = 1, name: String = "Varsayılan") = Preset(id, name, 25, 5, 15)

    private fun draft(name: String = "Ders") = Preset(name = name, focusMinutes = 40, breakMinutes = 5, longBreakMinutes = 15)

    @Test
    fun `loading ends with the stored presets and the selection`() = runTest {
        val vm = viewModel(preset(1, "A"), preset(2, "B"))
        vm.uiState.test {
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertEquals(listOf("A", "B"), state.presets.map { it.name })
            assertEquals(1L, state.selectedId)
        }
    }

    @Test
    fun `a new preset is added and becomes the selected one`() = runTest {
        val vm = viewModel(preset())
        vm.uiState.test {
            expectMostRecentItem()

            vm.save(draft("Ders"))

            val state = expectMostRecentItem()
            assertEquals(listOf("Varsayılan", "Ders"), state.presets.map { it.name })
            assertEquals(state.presets.last().id, state.selectedId)
        }
    }

    @Test
    fun `a blank name is not saved`() = runTest {
        val vm = viewModel(preset())
        vm.uiState.test {
            expectMostRecentItem()

            vm.save(draft("   "))

            assertEquals(1, vm.uiState.value.presets.size)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `editing an existing preset updates it and keeps the selection`() = runTest {
        val vm = viewModel(preset(1, "A"), preset(2, "B"))
        vm.uiState.test {
            expectMostRecentItem()

            vm.save(preset(2, "B").copy(name = "Ders", focusMinutes = 50))

            val state = expectMostRecentItem()
            assertEquals(listOf("A", "Ders"), state.presets.map { it.name })
            assertEquals(50, state.presets.last().focusMinutes)
            assertEquals(1L, state.selectedId)
        }
    }

    @Test
    fun `deleting a preset removes it`() = runTest {
        val vm = viewModel(preset(1, "A"), preset(2, "B"))
        vm.uiState.test {
            expectMostRecentItem()

            vm.delete(2)

            assertEquals(listOf("A"), expectMostRecentItem().presets.map { it.name })
        }
    }

    @Test
    fun `the last preset cannot be deleted and the screen knows it`() = runTest {
        val vm = viewModel(preset())
        vm.uiState.test {
            assertFalse(expectMostRecentItem().canDelete)

            vm.delete(1)

            assertEquals(1, vm.uiState.value.presets.size)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `deleting the selected preset selects the first one again`() = runTest {
        val vm = viewModel(preset(1, "A"), preset(2, "B"))
        settings.setSelectedPresetId(2)
        vm.uiState.test {
            assertEquals(2L, expectMostRecentItem().selectedId)

            vm.delete(2)

            assertEquals(1L, expectMostRecentItem().selectedId)
        }
    }

    @Test
    fun `deleting is allowed while more than one preset exists`() = runTest {
        val vm = viewModel(preset(1, "A"), preset(2, "B"))
        vm.uiState.test {
            assertTrue(expectMostRecentItem().canDelete)
        }
    }
}
