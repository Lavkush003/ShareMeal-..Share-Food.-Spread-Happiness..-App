package com.sharemeal.app.core.validation

import com.sharemeal.app.R
import com.sharemeal.app.domain.model.DonationQuantityUnit
import com.sharemeal.app.domain.model.FoodCategory
import com.sharemeal.app.domain.model.FoodClassification
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DonationValidatorTest {

    private val nowMillis: Long = Calendar.getInstance().apply {
        set(2026, Calendar.JANUARY, 15, 12, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private val validForm = DonationForm(
        title = "Fresh vegetable curry",
        description = "Cooked this morning with seasonal vegetables.",
        category = FoodCategory.COOKED_MEAL,
        quantity = "40",
        quantityUnit = DonationQuantityUnit.MEALS,
        classification = FoodClassification.VEGETARIAN,
        preparationDateMillis = dayOffset(-1),
        preparationTimeMinutes = 9 * 60,
        pickupStartMinutes = 10 * 60,
        pickupDeadlineMinutes = 11 * 60,
        storageConditions = "Kept in insulated containers.",
        packaging = "Sealed food-grade containers.",
        allergenInformation = "Contains dairy.",
        conditionDeclared = true,
        pickupAddress = "12 Market Street",
        city = "Bengaluru",
        state = "Karnataka",
        pinCode = "560001",
        contactPerson = "Ananya Rao",
        contactPhone = "9876543210"
    )

    @Test
    fun `complete form is valid`() {
        val result = DonationValidator.validate(validForm, nowMillis)

        assertTrue(result.isValid)
    }

    @Test
    fun `blank form reports every mandatory field`() {
        val result = DonationValidator.validate(DonationForm(), nowMillis)

        assertFalse(result.isValid)
        assertEquals(R.string.donation_error_title_required, result.fieldErrors[DonationField.TITLE])
        assertEquals(
            R.string.donation_error_description_required,
            result.fieldErrors[DonationField.DESCRIPTION]
        )
        assertEquals(
            R.string.donation_error_category_required,
            result.fieldErrors[DonationField.CATEGORY]
        )
        assertEquals(
            R.string.donation_error_quantity_required,
            result.fieldErrors[DonationField.QUANTITY]
        )
        assertEquals(
            R.string.donation_error_quantity_unit_required,
            result.fieldErrors[DonationField.QUANTITY_UNIT]
        )
        assertEquals(
            R.string.donation_error_classification_required,
            result.fieldErrors[DonationField.CLASSIFICATION]
        )
        assertEquals(
            R.string.donation_error_preparation_date_required,
            result.fieldErrors[DonationField.PREPARATION_DATE]
        )
        assertEquals(
            R.string.donation_error_preparation_time_required,
            result.fieldErrors[DonationField.PREPARATION_TIME]
        )
        assertEquals(
            R.string.donation_error_pickup_start_required,
            result.fieldErrors[DonationField.PICKUP_START]
        )
        assertEquals(
            R.string.donation_error_pickup_deadline_required,
            result.fieldErrors[DonationField.PICKUP_DEADLINE]
        )
        assertEquals(
            R.string.donation_error_storage_required,
            result.fieldErrors[DonationField.STORAGE_CONDITIONS]
        )
        assertEquals(
            R.string.donation_error_packaging_required,
            result.fieldErrors[DonationField.PACKAGING]
        )
        assertEquals(
            R.string.donation_error_allergens_required,
            result.fieldErrors[DonationField.ALLERGENS]
        )
        assertEquals(
            R.string.donation_error_condition_required,
            result.fieldErrors[DonationField.CONDITION_DECLARATION]
        )
        assertEquals(
            R.string.donation_error_pickup_address_required,
            result.fieldErrors[DonationField.PICKUP_ADDRESS]
        )
        assertEquals(R.string.error_city_required, result.fieldErrors[DonationField.CITY])
        assertEquals(R.string.error_state_required, result.fieldErrors[DonationField.STATE])
        assertEquals(R.string.error_pin_invalid, result.fieldErrors[DonationField.PIN_CODE])
        assertEquals(
            R.string.donation_error_contact_person_required,
            result.fieldErrors[DonationField.CONTACT_PERSON]
        )
        assertEquals(
            R.string.donation_error_contact_phone_required,
            result.fieldErrors[DonationField.CONTACT_PHONE]
        )
    }

    @Test
    fun `future preparation date is rejected`() {
        val result = DonationValidator.validate(
            validForm.copy(preparationDateMillis = dayOffset(1)),
            nowMillis
        )

        assertEquals(
            R.string.donation_error_preparation_date_future,
            result.fieldErrors[DonationField.PREPARATION_DATE]
        )
    }

    @Test
    fun `preparation time later today is rejected`() {
        val result = DonationValidator.validate(
            validForm.copy(
                preparationDateMillis = dayOffset(0),
                preparationTimeMinutes = 13 * 60,
                pickupStartMinutes = 14 * 60,
                pickupDeadlineMinutes = 15 * 60
            ),
            nowMillis
        )

        assertEquals(
            R.string.donation_error_preparation_time_future,
            result.fieldErrors[DonationField.PREPARATION_TIME]
        )
    }

    @Test
    fun `pickup must start after preparation`() {
        val result = DonationValidator.validate(
            validForm.copy(
                preparationTimeMinutes = 10 * 60,
                pickupStartMinutes = 9 * 60,
                pickupDeadlineMinutes = 11 * 60
            ),
            nowMillis
        )

        assertEquals(
            R.string.donation_error_pickup_start_after_preparation,
            result.fieldErrors[DonationField.PICKUP_START]
        )
    }

    @Test
    fun `pickup deadline must be after pickup start`() {
        val result = DonationValidator.validate(
            validForm.copy(
                pickupStartMinutes = 10 * 60,
                pickupDeadlineMinutes = 10 * 60
            ),
            nowMillis
        )

        assertEquals(
            R.string.donation_error_pickup_deadline_before_start,
            result.fieldErrors[DonationField.PICKUP_DEADLINE]
        )
    }

    @Test
    fun `other quantity unit requires a custom unit`() {
        val result = DonationValidator.validate(
            validForm.copy(
                quantityUnit = DonationQuantityUnit.OTHER,
                customQuantityUnit = " "
            ),
            nowMillis
        )

        assertEquals(
            R.string.donation_error_custom_unit_required,
            result.fieldErrors[DonationField.CUSTOM_QUANTITY_UNIT]
        )
    }

    @Test
    fun `quantity must be a positive integer`() {
        val result = DonationValidator.validate(
            validForm.copy(quantity = "0"),
            nowMillis
        )

        assertEquals(
            R.string.donation_error_quantity_invalid,
            result.fieldErrors[DonationField.QUANTITY]
        )
    }

    @Test
    fun `invalid phone and pin are rejected`() {
        val result = DonationValidator.validate(
            validForm.copy(
                pinCode = "0123",
                contactPhone = "12345"
            ),
            nowMillis
        )

        assertEquals(R.string.error_pin_invalid, result.fieldErrors[DonationField.PIN_CODE])
        assertEquals(
            R.string.donation_error_contact_phone_invalid,
            result.fieldErrors[DonationField.CONTACT_PHONE]
        )
        assertNull(result.fieldErrors[DonationField.TITLE])
    }

    private fun dayOffset(days: Int): Long = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        add(Calendar.DAY_OF_MONTH, days)
    }.timeInMillis
}
