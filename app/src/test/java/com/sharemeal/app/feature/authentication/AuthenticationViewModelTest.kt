package com.sharemeal.app.feature.authentication

import app.cash.turbine.test
import com.sharemeal.app.R
import com.sharemeal.app.core.config.AppConfig
import com.sharemeal.app.core.validation.AuthField
import com.sharemeal.app.data.auth.MockAccountSeeder
import com.sharemeal.app.domain.model.UserRole
import com.sharemeal.app.testutil.MainDispatcherRule
import com.sharemeal.app.testutil.MockAuthTestHarness
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthenticationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `invalid login shows field errors without submitting`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val harness = MockAuthTestHarness()
            val viewModel = AuthenticationViewModel(harness.authenticationRepository, AppConfig())

            viewModel.onEmailChange("invalid-email")
            viewModel.onPasswordChange("")
            viewModel.login()

            val state = viewModel.uiState.value
            assertEquals(R.string.error_email_invalid, state.fieldErrors[AuthField.EMAIL])
            assertEquals(R.string.error_password_required, state.fieldErrors[AuthField.PASSWORD])
            assertFalse(state.isSubmitting)
        }

    @Test
    fun `valid login emits authenticated event with role`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val harness = MockAuthTestHarness()
            val viewModel = AuthenticationViewModel(harness.authenticationRepository, AppConfig())

            viewModel.events.test {
                viewModel.onEmailChange("donor@sharemeal.dev")
                viewModel.onPasswordChange(MockAccountSeeder.DemoPassword)
                viewModel.login()
                advanceUntilIdle()

                assertEquals(
                    AuthenticationEvent.Authenticated(UserRole.DONOR),
                    awaitItem()
                )
            }
            assertFalse(viewModel.uiState.value.isSubmitting)
        }

    @Test
    fun `forgot password validates email`() = runTest(mainDispatcherRule.testDispatcher) {
        val harness = MockAuthTestHarness()
        val viewModel = AuthenticationViewModel(harness.authenticationRepository, AppConfig())

        viewModel.onResetEmailChange("bad")
        viewModel.requestPasswordReset()

        assertEquals(
            R.string.error_email_invalid,
            viewModel.uiState.value.resetFieldErrorRes
        )
    }

    @Test
    fun `forgot password never claims message was sent`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val harness = MockAuthTestHarness()
            val viewModel = AuthenticationViewModel(harness.authenticationRepository, AppConfig())

            viewModel.onResetEmailChange("donor@sharemeal.dev")
            viewModel.requestPasswordReset()
            advanceUntilIdle()

            assertEquals(
                R.string.forgot_password_backend_required,
                viewModel.uiState.value.resetMessageRes
            )
        }
}
