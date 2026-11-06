package com.sharemeal.app.core.validation

import androidx.annotation.StringRes
import com.sharemeal.app.R
import com.sharemeal.app.domain.model.DonationQuantityUnit
import java.util.Calendar

data class DonationValidationResult(
    val fieldErrors: Map<DonationField, Int> = emptyMap()
) {
    val isValid: Boolean
        get() = fieldErrors.isEmpty()
}

object DonationValidator {

    private val phonePattern = Regex("^[6-9]\\d{9}$")
    private val pinPattern = Regex("^[1-9]\\d{5}$")

    fun validate(form: DonationForm, nowMillis: Long = System.currentTimeMillis()): DonationValidationResult {
        val errors = linkedMapOf<DonationField, Int>()

        when {
            form.title.isBlank() ->
                errors[DonationField.TITLE] = R.string.donation_error_title_required
            form.title.trim().length < 3 ->
                errors[DonationField.TITLE] = R.string.donation_error_title_invalid
        }

        if (form.description.isBlank()) {
            errors[DonationField.DESCRIPTION] = R.string.donation_error_description_required
        }

        if (form.category == null) {
            errors[DonationField.CATEGORY] = R.string.donation_error_category_required
        }

        val quantity = form.quantity.trim().toIntOrNull()
        when {
            form.quantity.isBlank() ->
                errors[DonationField.QUANTITY] = R.string.donation_error_quantity_required
            quantity == null || quantity <= 0 ->
                errors[DonationField.QUANTITY] = R.string.donation_error_quantity_invalid
        }

        if (form.quantityUnit == null) {
            errors[DonationField.QUANTITY_UNIT] = R.string.donation_error_quantity_unit_required
        } else if (form.quantityUnit == DonationQuantityUnit.OTHER &&
            form.customQuantityUnit.isBlank()
        ) {
            errors[DonationField.CUSTOM_QUANTITY_UNIT] =
                R.string.donation_error_custom_unit_required
        }

        if (form.classification == null) {
            errors[DonationField.CLASSIFICATION] = R.string.donation_error_classification_required
        }

        validatePreparation(form, nowMillis, errors)
        validatePickupWindow(form, errors)

        if (form.storageConditions.isBlank()) {
            errors[DonationField.STORAGE_CONDITIONS] =
                R.string.donation_error_storage_required
        }
        if (form.packaging.isBlank()) {
            errors[DonationField.PACKAGING] = R.string.donation_error_packaging_required
        }
        if (form.allergenInformation.isBlank()) {
            errors[DonationField.ALLERGENS] = R.string.donation_error_allergens_required
        }
        if (!form.conditionDeclared) {
            errors[DonationField.CONDITION_DECLARATION] =
                R.string.donation_error_condition_required
        }

        if (form.pickupAddress.isBlank()) {
            errors[DonationField.PICKUP_ADDRESS] =
                R.string.donation_error_pickup_address_required
        }
        if (form.city.isBlank()) {
            errors[DonationField.CITY] = R.string.error_city_required
        }
        if (form.state.isBlank()) {
            errors[DonationField.STATE] = R.string.error_state_required
        }
        if (!pinPattern.matches(form.pinCode.trim())) {
            errors[DonationField.PIN_CODE] = R.string.error_pin_invalid
        }
        if (form.contactPerson.isBlank()) {
            errors[DonationField.CONTACT_PERSON] =
                R.string.donation_error_contact_person_required
        }
        validateContactPhone(form.contactPhone)?.let {
            errors[DonationField.CONTACT_PHONE] = it
        }

        return DonationValidationResult(errors)
    }

    private fun validatePreparation(
        form: DonationForm,
        nowMillis: Long,
        errors: MutableMap<DonationField, Int>
    ) {
        val preparationDate = form.preparationDateMillis
        val preparationTime = form.preparationTimeMinutes

        when {
            preparationDate == null ->
                errors[DonationField.PREPARATION_DATE] =
                    R.string.donation_error_preparation_date_required
            startOfDay(preparationDate) > startOfDay(nowMillis) ->
                errors[DonationField.PREPARATION_DATE] =
                    R.string.donation_error_preparation_date_future
        }

        when {
            preparationTime == null ->
                errors[DonationField.PREPARATION_TIME] =
                    R.string.donation_error_preparation_time_required
            preparationDate != null &&
                startOfDay(preparationDate) == startOfDay(nowMillis) &&
                preparationTime > minutesOfDay(nowMillis) ->
                errors[DonationField.PREPARATION_TIME] =
                    R.string.donation_error_preparation_time_future
        }
    }

    private fun validatePickupWindow(
        form: DonationForm,
        errors: MutableMap<DonationField, Int>
    ) {
        val pickupStart = form.pickupStartMinutes
        val pickupDeadline = form.pickupDeadlineMinutes
        val preparationTime = form.preparationTimeMinutes

        if (pickupStart == null) {
            errors[DonationField.PICKUP_START] =
                R.string.donation_error_pickup_start_required
        }
        if (pickupDeadline == null) {
            errors[DonationField.PICKUP_DEADLINE] =
                R.string.donation_error_pickup_deadline_required
        }

        if (pickupStart != null && pickupDeadline != null &&
            pickupDeadline <= pickupStart
        ) {
            errors[DonationField.PICKUP_DEADLINE] =
                R.string.donation_error_pickup_deadline_before_start
        }

        if (pickupStart != null && preparationTime != null &&
            pickupStart <= preparationTime
        ) {
            errors[DonationField.PICKUP_START] =
                R.string.donation_error_pickup_start_after_preparation
        }
    }

    @StringRes
    private fun validateContactPhone(phone: String): Int? {
        val normalized = normalizePhone(phone)
        return when {
            normalized.isBlank() -> R.string.donation_error_contact_phone_required
            !phonePattern.matches(normalized) ->
                R.string.donation_error_contact_phone_invalid
            else -> null
        }
    }

    private fun normalizePhone(phone: String): String {
        val digits = phone.filter(Char::isDigit)
        return if (digits.length > 10) digits.takeLast(10) else digits
    }

    private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun minutesOfDay(millis: Long): Int = Calendar.getInstance().apply {
        timeInMillis = millis
    }.let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
}
