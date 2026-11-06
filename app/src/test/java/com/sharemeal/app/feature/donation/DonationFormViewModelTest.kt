package com.sharemeal.app.feature.donation

import app.cash.turbine.test
import com.sharemeal.app.R
import com.sharemeal.app.core.validation.DonationField
import com.sharemeal.app.data.donation.MockDonationRepository
import com.sharemeal.app.data.media.LocalPreviewImageRepository
import com.sharemeal.app.domain.model.DonationQuantityUnit
import com.sharemeal.app.domain.model.FoodCategory
import com.sharemeal.app.domain.model.FoodClassification
import com.sharemeal.app.testutil.MainDispatcherRule
import java.util.Calendar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DonationFormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `publishing an empty form reports validation errors`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = MockDonationRepository()
            val viewModel = DonationFormViewModel(
                repository,
                LocalPreviewImageRepository()
            )

            viewModel.publishDonation()

            val errors = viewModel.uiState.value.fieldErrors
            assertEquals(R.string.donation_error_title_required, errors[DonationField.TITLE])
            assertEquals(
                R.string.donation_error_quantity_required,
                errors[DonationField.QUANTITY]
            )
            assertEquals(
                R.string.donation_error_preparation_date_required,
                errors[DonationField.PREPARATION_DATE]
            )
            assertTrue(repository.donations.value.isEmpty())
        }

    @Test
    fun `valid form publishes a donation and emits an event`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = MockDonationRepository()
            val viewModel = DonationFormViewModel(
                repository,
                LocalPreviewImageRepository()
            )
            fillValidForm(viewModel)

            viewModel.events.test {
                viewModel.publishDonation()
                advanceUntilIdle()

                val event = awaitItem()
                assertTrue(event is DonationFormEvent.Published)
                val donation = (event as DonationFormEvent.Published).donation
                assertEquals("Fresh vegetable curry", donation.title)
                assertEquals("content://test/food", donation.images.single().localUri)
                assertNull(donation.images.single().remoteUrl)
            }

            assertTrue(viewModel.uiState.value.fieldErrors.isEmpty())
            assertEquals(1, repository.donations.value.size)
            assertEquals("Fresh vegetable curry", repository.donations.value.first().title)
        }

    @Test
    fun `selected images are kept as local previews up to the limit`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = DonationFormViewModel(
                MockDonationRepository(),
                LocalPreviewImageRepository()
            )

            viewModel.onImagesSelected(
                listOf(
                    "content://test/1",
                    "content://test/2",
                    "content://test/3",
                    "content://test/4",
                    "content://test/5",
                    "content://test/6"
                )
            )

            val images = viewModel.uiState.value.form.images
            assertEquals(DonationFormViewModel.MAX_IMAGES, images.size)
            assertTrue(images.all { it.isLocalOnly })
        }

    @Test
    fun `images can be replaced and removed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = DonationFormViewModel(
                MockDonationRepository(),
                LocalPreviewImageRepository()
            )
            viewModel.onImagesSelected(
                listOf("content://test/one", "content://test/two")
            )

            viewModel.onImageReplaced(1, "content://test/replacement")

            assertEquals(
                listOf("content://test/one", "content://test/replacement"),
                viewModel.uiState.value.form.images.map { it.localUri }
            )

            val imageId = viewModel.uiState.value.form.images.first().id
            viewModel.onImageRemoved(imageId)

            assertEquals(
                listOf("content://test/replacement"),
                viewModel.uiState.value.form.images.map { it.localUri }
            )
        }

    @Test
    fun `editing a field clears its validation error`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = DonationFormViewModel(
                MockDonationRepository(),
                LocalPreviewImageRepository()
            )

            viewModel.publishDonation()
            assertTrue(viewModel.uiState.value.fieldErrors.containsKey(DonationField.TITLE))

            viewModel.onFieldChange(DonationField.TITLE, "Fresh vegetable curry")

            assertTrue(!viewModel.uiState.value.fieldErrors.containsKey(DonationField.TITLE))
        }

    private fun fillValidForm(viewModel: DonationFormViewModel) {
        val preparationDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, -1)
        }.timeInMillis

        viewModel.onFieldChange(DonationField.TITLE, "Fresh vegetable curry")
        viewModel.onFieldChange(
            DonationField.DESCRIPTION,
            "Cooked this morning with seasonal vegetables."
        )
        viewModel.onCategoryChange(FoodCategory.COOKED_MEAL)
        viewModel.onImagesSelected(listOf("content://test/food"))
        viewModel.onFieldChange(DonationField.QUANTITY, "40")
        viewModel.onQuantityUnitChange(DonationQuantityUnit.MEALS)
        viewModel.onClassificationChange(FoodClassification.VEGETARIAN)
        viewModel.onPreparationDateSelected(preparationDate)
        viewModel.onPreparationTimeSelected(9 * 60)
        viewModel.onPickupStartSelected(10 * 60)
        viewModel.onPickupDeadlineSelected(11 * 60)
        viewModel.onFieldChange(
            DonationField.STORAGE_CONDITIONS,
            "Kept in insulated containers."
        )
        viewModel.onFieldChange(DonationField.PACKAGING, "Sealed food-grade containers.")
        viewModel.onFieldChange(DonationField.ALLERGENS, "Contains dairy.")
        viewModel.onConditionDeclaredChange(true)
        viewModel.onFieldChange(DonationField.PICKUP_ADDRESS, "12 Market Street")
        viewModel.onFieldChange(DonationField.CITY, "Bengaluru")
        viewModel.onFieldChange(DonationField.STATE, "Karnataka")
        viewModel.onFieldChange(DonationField.PIN_CODE, "560001")
        viewModel.onFieldChange(DonationField.CONTACT_PERSON, "Ananya Rao")
        viewModel.onFieldChange(DonationField.CONTACT_PHONE, "9876543210")
        viewModel.onSpecialInstructionsChange("Call on arrival.")
    }
}
