package com.sharemeal.app.feature.profile

import com.sharemeal.app.R
import com.sharemeal.app.core.validation.AuthField
import com.sharemeal.app.data.auth.MockAccountSeeder
import com.sharemeal.app.testutil.MainDispatcherRule
import com.sharemeal.app.testutil.MockAuthTestHarness
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `profile loads current user`() = runTest(mainDispatcherRule.testDispatcher) {
        val harness = loggedInHarness()
        val viewModel = ProfileViewModel(
            harness.userRepository,
            harness.authenticationRepository
        )
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.user)
        assertEquals("Green Leaf Kitchen", viewModel.uiState.value.organizationName)
    }

    @Test
    fun `profile update saves edited values`() = runTest(mainDispatcherRule.testDispatcher) {
        val harness = loggedInHarness()
        val viewModel = ProfileViewModel(
            harness.userRepository,
            harness.authenticationRepository
        )
        advanceUntilIdle()

        viewModel.startEditing()
        viewModel.onFieldChange(AuthField.MOBILE, "9123456789")
        viewModel.onFieldChange(AuthField.FULL_NAME, "Aarav S Sharma")
        viewModel.saveProfile()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Aarav S Sharma", state.user?.displayName)
        assertEquals("9123456789", state.user?.mobile)
        assertEquals(R.string.profile_update_success, state.successMessageRes)
        assertTrue(!state.isEditing)
    }

    private suspend fun loggedInHarness(): MockAuthTestHarness {
        val harness = MockAuthTestHarness()
        harness.authenticationRepository.login(
            email = "donor@sharemeal.dev",
            password = MockAccountSeeder.DemoPassword,
            rememberSession = true
        )
        return harness
    }
}
