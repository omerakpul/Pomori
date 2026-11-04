package com.pomori.ui.onboarding

import app.cash.turbine.test
import com.pomori.testutil.FakeSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `first launch reports onboarding as not done`() = runTest {
        val vm = OnboardingViewModel(FakeSettingsStore(initialOnboardingDone = false))
        vm.onboardingDone.test {
            assertEquals(false, expectMostRecentItem())
        }
    }

    @Test
    fun `completing onboarding flips the flag to done`() = runTest {
        val vm = OnboardingViewModel(FakeSettingsStore(initialOnboardingDone = false))
        vm.onboardingDone.test {
            assertEquals(false, expectMostRecentItem())
            vm.complete()
            assertEquals(true, awaitItem())
        }
    }

    @Test
    fun `returning user skips onboarding`() = runTest {
        val vm = OnboardingViewModel(FakeSettingsStore(initialOnboardingDone = true))
        vm.onboardingDone.test {
            assertEquals(true, expectMostRecentItem())
        }
    }
}
