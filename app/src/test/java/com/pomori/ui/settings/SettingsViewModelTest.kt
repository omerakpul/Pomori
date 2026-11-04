package com.pomori.ui.settings

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.pomori.domain.model.ExceptionRule
import com.pomori.domain.model.InstalledApp
import com.pomori.testutil.FakeExceptionRepository
import com.pomori.testutil.FakeInstalledApps
import com.pomori.testutil.FakeSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val settings = FakeSettingsStore()
    private val exceptions = FakeExceptionRepository()
    private val apps = FakeInstalledApps(
        listOf(
            InstalledApp("com.google.android.gm", "Gmail"),
            InstalledApp("com.whatsapp", "WhatsApp"),
        ),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = SettingsViewModel(settings, exceptions, apps)

    /** Yüklenen uygulamalar ve ilk ayarlar gelene kadar bekleyip son durumu verir. */
    private suspend fun ReceiveTurbine<SettingsUiState>.latest(): SettingsUiState = expectMostRecentItem()

    @Test
    fun `starts with the stored settings`() = runTest {
        val vm = SettingsViewModel(
            FakeSettingsStore(focusMinutes = 40, breakMinutes = 8, retentionDays = 14),
            exceptions,
            apps,
        )
        vm.uiState.test {
            val state = latest()
            assertEquals(40, state.focusMinutes)
            assertEquals(8, state.breakMinutes)
            assertEquals(14, state.retentionDays)
        }
    }

    @Test
    fun `changing a setting writes it and updates the state`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            latest()

            vm.setFocusMinutes(45)
            vm.setBreakMinutes(10)
            vm.setRetentionDays(21)

            val state = latest()
            assertEquals(45, state.focusMinutes)
            assertEquals(10, state.breakMinutes)
            assertEquals(21, state.retentionDays)
        }
    }

    @Test
    fun `values beyond the limits are clamped`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            latest()

            vm.setFocusMinutes(0)
            assertEquals(1, latest().focusMinutes)
            vm.setFocusMinutes(500)
            assertEquals(120, latest().focusMinutes)
            vm.setRetentionDays(99)
            assertEquals(30, latest().retentionDays)
        }
    }

    @Test
    fun `adding a sender shows it in the rules`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            latest()

            vm.addSender("Anne")

            val rules = latest().rules
            assertEquals(listOf(ExceptionRule.Sender("Anne")), rules.map { it.rule })
            assertEquals("Anne", rules.single().displayName)
        }
    }

    @Test
    fun `a blank sender is not added`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            latest()

            vm.addSender("   ")

            // Durum değişmediği için yeni bir yayın olmaz.
            expectNoEvents()
            assertTrue(vm.uiState.value.rules.isEmpty())
        }
    }

    @Test
    fun `adding an app shows its label instead of the package name`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            latest()

            vm.addApp(InstalledApp("com.whatsapp", "WhatsApp"))

            val item = latest().rules.single()
            assertEquals(ExceptionRule.Package("com.whatsapp"), item.rule)
            assertEquals("WhatsApp", item.displayName)
        }
    }

    @Test
    fun `a rule for an app that is not visible falls back to the package name`() = runTest {
        exceptions.add(ExceptionRule.Package("com.removed.app"))
        val vm = viewModel()
        vm.uiState.test {
            assertEquals("com.removed.app", latest().rules.single().displayName)
        }
    }

    @Test
    fun `apps that are already excepted are no longer selectable`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            assertEquals(listOf("Gmail", "WhatsApp"), latest().selectableApps.map { it.label })

            vm.addApp(InstalledApp("com.whatsapp", "WhatsApp"))

            assertEquals(listOf("Gmail"), latest().selectableApps.map { it.label })
        }
    }

    @Test
    fun `a sender rule does not hide an app from the selectable list`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            latest()

            vm.addSender("WhatsApp")

            assertEquals(2, latest().selectableApps.size)
        }
    }

    @Test
    fun `removing a rule makes the app selectable again`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            latest()
            vm.addApp(InstalledApp("com.whatsapp", "WhatsApp"))
            assertEquals(1, latest().selectableApps.size)

            vm.removeRule(ExceptionRule.Package("com.whatsapp"))

            val state = latest()
            assertTrue(state.rules.isEmpty())
            assertEquals(2, state.selectableApps.size)
        }
    }

    @Test
    fun `the same rule twice is listed once`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            latest()

            vm.addSender("Anne")
            vm.addSender(" Anne ")

            assertEquals(1, latest().rules.size)
        }
    }

    @Test
    fun `rules are listed in the order they were added`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            latest()

            vm.addSender("Zeynep")
            vm.addApp(InstalledApp("com.whatsapp", "WhatsApp"))
            vm.addSender("Ali")

            assertEquals(listOf("Zeynep", "WhatsApp", "Ali"), latest().rules.map { it.displayName })
        }
    }
}
