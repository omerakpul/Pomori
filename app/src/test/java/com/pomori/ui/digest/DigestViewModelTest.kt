package com.pomori.ui.digest

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.pomori.domain.model.CapturedNotification
import com.pomori.domain.usecase.GetDigest
import com.pomori.testutil.FakeNotificationRepository
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
class DigestViewModelTest {

    private val repo = FakeNotificationRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(sessionId: Long = 1) = DigestViewModel(
        SavedStateHandle(mapOf(DigestViewModel.ARG_SESSION_ID to sessionId)),
        GetDigest(repo),
    )

    private suspend fun insert(pkg: String, label: String = pkg, sessionId: Long = 1, postedAt: Long = 1_000) {
        repo.insert(
            CapturedNotification(
                sessionId = sessionId,
                packageName = pkg,
                appLabel = label,
                title = "başlık",
                text = null,
                postedAt = postedAt,
            ),
        )
    }

    /** Yükleme durumunu atlayıp ilk gerçek durumu verir. */
    private suspend fun ReceiveTurbine<DigestUiState>.awaitLoaded(): DigestUiState {
        var state = awaitItem()
        while (state.isLoading) state = awaitItem()
        return state
    }

    @Test
    fun `the initial ui state is loading and not yet empty`() = runTest {
        val initial = DigestUiState()

        assertTrue(initial.isLoading)
        assertFalse(initial.isEmpty)
    }

    @Test
    fun `a session without notifications is empty once loaded`() = runTest {
        viewModel().uiState.test {
            val state = awaitLoaded()
            assertTrue(state.isEmpty)
            assertEquals(0, state.total)
        }
    }

    @Test
    fun `apps are listed busiest first with the total`() = runTest {
        insert("gmail", "Gmail")
        insert("wa", "WhatsApp")
        insert("wa", "WhatsApp")

        viewModel().uiState.test {
            val state = awaitLoaded()
            assertFalse(state.isEmpty)
            assertEquals(3, state.total)
            assertEquals(listOf("WhatsApp", "Gmail"), state.apps.map { it.appLabel })
            assertEquals(listOf(2, 1), state.apps.map { it.count })
        }
    }

    @Test
    fun `only the session from the navigation argument is shown`() = runTest {
        insert("a", sessionId = 1)
        insert("b", sessionId = 2)

        viewModel(sessionId = 2).uiState.test {
            assertEquals(listOf("b"), awaitLoaded().apps.map { it.packageName })
        }
    }

    @Test
    fun `all apps start collapsed`() = runTest {
        insert("a")

        viewModel().uiState.test {
            val state = awaitLoaded()
            assertFalse(state.isExpanded("a"))
            assertTrue(state.expanded.isEmpty())
        }
    }

    @Test
    fun `toggling expands and collapses one app without touching the others`() = runTest {
        insert("a")
        insert("b")
        val vm = viewModel()

        vm.uiState.test {
            awaitLoaded()

            vm.toggleExpanded("a")
            val expanded = awaitItem()
            assertTrue(expanded.isExpanded("a"))
            assertFalse(expanded.isExpanded("b"))

            vm.toggleExpanded("b")
            assertEquals(setOf("a", "b"), awaitItem().expanded)

            vm.toggleExpanded("a")
            val collapsed = awaitItem()
            assertFalse(collapsed.isExpanded("a"))
            assertTrue(collapsed.isExpanded("b"))
        }
    }

    @Test
    fun `a new notification updates the list and keeps the expanded apps`() = runTest {
        insert("a")
        val vm = viewModel()

        vm.uiState.test {
            awaitLoaded()
            vm.toggleExpanded("a")
            awaitItem()

            insert("a")
            val state = awaitItem()
            assertEquals(2, state.total)
            assertTrue(state.isExpanded("a"))
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `missing session id argument fails fast`() {
        DigestViewModel(SavedStateHandle(), GetDigest(repo))
    }
}
