package com.sharemeal.app.feature.splash

import com.sharemeal.app.data.auth.MockAccountSeeder
import com.sharemeal.app.testutil.MainDispatcherRule
import com.sharemeal.app.testutil.MockAuthTestHarness
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `new user is routed to onboarding`() = runTest(mainDispatcherRule.testDispatcher) {
        val harness = MockAuthTestHarness()
        val viewModel = SplashViewModel(
            harness.authenticationRepository,
            harness.preferences
        )
        advanceUntilIdle()

        assertEquals(SplashDestination.ONBOARDING, viewModel.uiState.value.destination)
    }

    @Test
    fun `completed onboarding is routed to welcome`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val harness = MockAuthTestHarness()
            harness.preferences.setOnboardingCompleted()
            val viewModel = SplashViewModel(
                harness.authenticationRepository,
                harness.preferences
            )
            advanceUntilIdle()

            assertEquals(SplashDestination.WELCOME, viewModel.uiState.value.destination)
        }

    @Test
    fun `logged in donor is routed to donor dashboard`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val harness = MockAuthTestHarness()
            harness.authenticationRepository.login(
                email = "donor@sharemeal.dev",
                password = MockAccountSeeder.DemoPassword,
                rememberSession = true
            )
            val viewModel = SplashViewModel(
                harness.authenticationRepository,
                harness.preferences
            )
            advanceUntilIdle()

            assertEquals(
                SplashDestination.DONOR_DASHBOARD,
                viewModel.uiState.value.destination
            )
        }
}
