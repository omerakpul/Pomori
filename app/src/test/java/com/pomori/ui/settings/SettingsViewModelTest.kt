package com.pomori.ui.settings

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.pomori.domain.model.CapturedNotification
import com.pomori.domain.model.ExceptionRule
import com.pomori.data.repository.PresetRepositoryImpl
import com.pomori.data.repository.toEntity
import com.pomori.domain.model.InstalledApp
import com.pomori.domain.model.Preset
import com.pomori.domain.usecase.CleanupOldNotifications
import com.pomori.domain.usecase.ObserveSelectedPreset
import com.pomori.testutil.FakeClock
import com.pomori.testutil.FakeExceptionRepository
import com.pomori.testutil.FakeInstalledApps
import com.pomori.testutil.FakeNotificationRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val day = 24 * 60 * 60 * 1000L
    private val now = 100 * day
    private val settings = FakeSettingsStore()
    private val exceptions = FakeExceptionRepository()
    private val notifications = FakeNotificationRepository()
    private val clock = FakeClock(nowMs = now)
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

    private var presetDao = FakePresetDao(listOf(Preset(1, "Varsayılan", 25, 5, 15).toEntity()))

    private fun viewModel(store: FakeSettingsStore = settings): SettingsViewModel {
        val presets = PresetRepositoryImpl(presetDao)
        return SettingsViewModel(
            store,
            exceptions,
            CleanupOldNotifications(store, notifications, clock),
            io.mockk.mockk(relaxed = true),
            apps,
            presets,
            ObserveSelectedPreset(presets, store),
        )
    }

    private suspend fun notificationAgo(days: Long) {
        notifications.insert(
            CapturedNotification(
                sessionId = 1,
                packageName = "com.whatsapp",
                appLabel = "WhatsApp",
                title = null,
                text = null,
                postedAt = now - days * day,
            ),
        )
    }

    /** Yüklenen uygulamalar ve ilk ayarlar gelene kadar bekleyip son durumu verir. */
    private suspend fun ReceiveTurbine<SettingsUiState>.latest(): SettingsUiState = expectMostRecentItem()

    @Test
    fun `starts with the stored retention period`() = runTest {
        val vm = viewModel(FakeSettingsStore(retentionDays = 14))
        vm.uiState.test {
            assertEquals(14, latest().retentionDays)
        }
    }

    @Test
    fun `the retention period is written and shown`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            latest()

            vm.setRetentionDays(21)

            assertEquals(21, latest().retentionDays)
        }
    }

    @Test
    fun `values beyond the limits are clamped`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            latest()

            vm.setRetentionDays(99)
            assertEquals(30, latest().retentionDays)
            vm.setRetentionDays(0)
            assertEquals(1, latest().retentionDays)
        }
    }

    @Test
    fun `the presets row shows how many modes there are and which one is active`() = runTest {
        presetDao = FakePresetDao(
            listOf(
                Preset(1, "Varsayılan", 25, 5, 15).toEntity(),
                Preset(2, "Ders", 40, 5, 15).toEntity(),
            ),
        )
        val vm = viewModel(FakeSettingsStore(selectedPresetId = 2))
        vm.uiState.test {
            val state = latest()
            assertEquals(2, state.presetCount)
            assertEquals("Ders", state.selectedPresetName)
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

    // --- saklama süresi değişince hemen temizlik ---

    @Test
    fun `shortening the retention period deletes the notifications that fall outside it`() = runTest {
        notificationAgo(days = 2)
        notificationAgo(days = 5)
        notificationAgo(days = 6)
        val vm = viewModel()
        vm.uiState.test {
            latest()

            vm.setRetentionDays(3)

            // Yalnızca 3 günden eski olanlar silinir.
            assertEquals(1, notifications.rows.size)
            assertEquals(3, latest().retentionDays)
        }
    }

    @Test
    fun `lengthening the retention period deletes nothing`() = runTest {
        notificationAgo(days = 2)
        notificationAgo(days = 6)
        val vm = viewModel()
        vm.uiState.test {
            latest()

            vm.setRetentionDays(30)

            assertEquals(30, latest().retentionDays)
            assertEquals(2, notifications.rows.size)
        }
    }

    @Test
    fun `the new value is what the cleanup uses even for out of range input`() = runTest {
        notificationAgo(days = 2)
        val vm = viewModel()
        vm.uiState.test {
            latest()

            vm.setRetentionDays(0) // depo 1 güne çeker

            assertEquals(0, notifications.rows.size)
            assertEquals(1, latest().retentionDays)
        }
    }

    @Test
    fun `the restore setting is on by default and can be switched off and on`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            assertTrue(latest().restoreNotifications)

            vm.setRestoreNotifications(false)
            assertEquals(false, latest().restoreNotifications)

            vm.setRestoreNotifications(true)
            assertTrue(latest().restoreNotifications)
        }
    }
}
