package com.sharemeal.app.feature.registration

import app.cash.turbine.test
import com.sharemeal.app.R
import com.sharemeal.app.core.validation.AuthField
import com.sharemeal.app.domain.model.DonorCategory
import com.sharemeal.app.domain.model.UserRole
import com.sharemeal.app.testutil.MainDispatcherRule
import com.sharemeal.app.testutil.MockAuthTestHarness
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegistrationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `role selection updates registration state`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = RegistrationViewModel(MockAuthTestHarness().authenticationRepository)

            viewModel.setRole(UserRole.NGO)

            assertEquals(UserRole.NGO, viewModel.uiState.value.selectedRole)
        }

    @Test
    fun `empty donor form shows required field errors`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = RegistrationViewModel(MockAuthTestHarness().authenticationRepository)

            viewModel.registerDonor()

            val errors = viewModel.uiState.value.donorFieldErrors
            assertEquals(R.string.error_name_required, errors[AuthField.FULL_NAME])
            assertEquals(R.string.error_email_required, errors[AuthField.EMAIL])
            assertEquals(R.string.error_password_required, errors[AuthField.PASSWORD])
            assertEquals(R.string.error_category_required, errors[AuthField.DONOR_CATEGORY])
        }

    @Test
    fun `empty ngo form shows required verification errors`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = RegistrationViewModel(MockAuthTestHarness().authenticationRepository)

            viewModel.registerNgo()

            val errors = viewModel.uiState.value.ngoFieldErrors
            assertEquals(R.string.error_ngo_name_required, errors[AuthField.NGO_NAME])
            assertEquals(R.string.error_name_required, errors[AuthField.CONTACT_PERSON])
            assertEquals(R.string.error_operating_area_required, errors[AuthField.OPERATING_AREA])
            assertEquals(R.string.error_capacity_required, errors[AuthField.CAPACITY])
            assertEquals(R.string.error_document_info_required, errors[AuthField.DOCUMENT_INFO])
        }

    @Test
    fun `valid donor registration emits registered event`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val harness = MockAuthTestHarness()
            val viewModel = RegistrationViewModel(harness.authenticationRepository)
            fillValidDonorForm(viewModel)

            viewModel.events.test {
                viewModel.registerDonor()
                advanceUntilIdle()
                assertEquals(
                    RegistrationEvent.Registered(UserRole.DONOR),
                    awaitItem()
                )
            }

            assertTrue(viewModel.uiState.value.donorFieldErrors.isEmpty())
        }

    @Test
    fun `valid ngo registration emits registered event`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val harness = MockAuthTestHarness()
            val viewModel = RegistrationViewModel(harness.authenticationRepository)
            fillValidNgoForm(viewModel)

            viewModel.events.test {
                viewModel.registerNgo()
                advanceUntilIdle()
                assertEquals(
                    RegistrationEvent.Registered(UserRole.NGO),
                    awaitItem()
                )
            }

            assertTrue(viewModel.uiState.value.ngoFieldErrors.isEmpty())
        }

    private fun fillValidDonorForm(viewModel: RegistrationViewModel) {
        viewModel.onDonorChange(AuthField.FULL_NAME, "Ananya Rao")
        viewModel.onDonorChange(AuthField.EMAIL, "ananya@example.com")
        viewModel.onDonorChange(AuthField.MOBILE, "9876500000")
        viewModel.onDonorChange(AuthField.PASSWORD, "ShareMeal123!")
        viewModel.onDonorChange(AuthField.CONFIRM_PASSWORD, "ShareMeal123!")
        viewModel.onDonorCategoryChange(DonorCategory.RESTAURANT)
        viewModel.onDonorChange(AuthField.ADDRESS_LINE, "10 Market Street")
        viewModel.onDonorChange(AuthField.CITY, "Bengaluru")
        viewModel.onDonorChange(AuthField.STATE, "Karnataka")
        viewModel.onDonorChange(AuthField.PIN_CODE, "560001")
    }

    private fun fillValidNgoForm(viewModel: RegistrationViewModel) {
        viewModel.onNgoChange(AuthField.NGO_NAME, "Community Kitchen")
        viewModel.onNgoChange(AuthField.CONTACT_PERSON, "Ravi Kumar")
        viewModel.onNgoChange(AuthField.EMAIL, "ravi@example.com")
        viewModel.onNgoChange(AuthField.MOBILE, "9876500001")
        viewModel.onNgoChange(AuthField.PASSWORD, "ShareMeal123!")
        viewModel.onNgoChange(AuthField.CONFIRM_PASSWORD, "ShareMeal123!")
        viewModel.onNgoChange(AuthField.ADDRESS_LINE, "10 Market Street")
        viewModel.onNgoChange(AuthField.CITY, "Bengaluru")
        viewModel.onNgoChange(AuthField.STATE, "Karnataka")
        viewModel.onNgoChange(AuthField.PIN_CODE, "560001")
        viewModel.onNgoChange(AuthField.OPERATING_AREA, "North Bengaluru")
        viewModel.onNgoChange(AuthField.CAPACITY, "100 meals/day")
        viewModel.onNgoChange(AuthField.DOCUMENT_INFO, "Mock registration certificate")
    }
}
