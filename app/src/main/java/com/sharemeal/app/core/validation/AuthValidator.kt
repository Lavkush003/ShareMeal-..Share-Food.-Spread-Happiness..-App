package com.sharemeal.app.core.validation

import androidx.annotation.StringRes
import com.sharemeal.app.R
import com.sharemeal.app.domain.model.Address
import com.sharemeal.app.domain.model.DonorCategory
import com.sharemeal.app.domain.model.UserRole

data class ValidationResult(
    val fieldErrors: Map<AuthField, Int> = emptyMap()
) {
    val isValid: Boolean
        get() = fieldErrors.isEmpty()
}

object AuthValidator {

    private val emailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val mobilePattern = Regex("^[6-9]\\d{9}$")
    private val pinPattern = Regex("^[1-9]\\d{5}$")
    private val letterPattern = Regex("[A-Za-z]")
    private val digitPattern = Regex("\\d")

    fun normalizeEmail(email: String): String = email.trim().lowercase()

    fun normalizeMobile(mobile: String): String {
        val digits = mobile.filter(Char::isDigit)
        return if (digits.length > 10) digits.takeLast(10) else digits
    }

    fun validateLogin(email: String, password: String): ValidationResult {
        val errors = mutableMapOf<AuthField, Int>()
        validateEmail(email)?.let { errors[AuthField.EMAIL] = it }
        if (password.isBlank()) {
            errors[AuthField.PASSWORD] = R.string.error_password_required
        }
        return ValidationResult(errors)
    }

    fun validateForgotPassword(email: String): ValidationResult {
        val errors = mutableMapOf<AuthField, Int>()
        validateEmail(email)?.let { errors[AuthField.EMAIL] = it }
        return ValidationResult(errors)
    }

    fun validateDonorRegistration(
        fullName: String,
        organizationName: String,
        email: String,
        mobile: String,
        password: String,
        confirmPassword: String,
        category: DonorCategory?,
        address: Address
    ): ValidationResult {
        val errors = mutableMapOf<AuthField, Int>()
        validatePersonName(fullName)?.let { errors[AuthField.FULL_NAME] = it }
        validateEmail(email)?.let { errors[AuthField.EMAIL] = it }
        validateMobile(mobile)?.let { errors[AuthField.MOBILE] = it }
        validatePassword(password)?.let { errors[AuthField.PASSWORD] = it }
        validatePasswordConfirmation(password, confirmPassword)?.let {
            errors[AuthField.CONFIRM_PASSWORD] = it
        }
        if (category == null) {
            errors[AuthField.DONOR_CATEGORY] = R.string.error_category_required
        }
        validateAddress(address, errors)
        return ValidationResult(errors)
    }

    fun validateNgoRegistration(
        ngoName: String,
        registrationNumber: String,
        contactPersonName: String,
        email: String,
        mobile: String,
        password: String,
        confirmPassword: String,
        address: Address,
        operatingArea: String,
        capacity: String,
        documentInfo: String
    ): ValidationResult {
        val errors = mutableMapOf<AuthField, Int>()
        validateOrganizationName(ngoName)?.let { errors[AuthField.NGO_NAME] = it }
        validatePersonName(contactPersonName)?.let { errors[AuthField.CONTACT_PERSON] = it }
        validateEmail(email)?.let { errors[AuthField.EMAIL] = it }
        validateMobile(mobile)?.let { errors[AuthField.MOBILE] = it }
        validatePassword(password)?.let { errors[AuthField.PASSWORD] = it }
        validatePasswordConfirmation(password, confirmPassword)?.let {
            errors[AuthField.CONFIRM_PASSWORD] = it
        }
        validateAddress(address, errors)
        if (operatingArea.isBlank()) {
            errors[AuthField.OPERATING_AREA] = R.string.error_operating_area_required
        }
        if (capacity.isBlank()) {
            errors[AuthField.CAPACITY] = R.string.error_capacity_required
        }
        if (documentInfo.isBlank()) {
            errors[AuthField.DOCUMENT_INFO] = R.string.error_document_info_required
        }
        if (registrationNumber.isNotBlank() && registrationNumber.trim().length < 3) {
            errors[AuthField.REGISTRATION_NUMBER] = R.string.error_registration_number_invalid
        }
        return ValidationResult(errors)
    }

    fun validateProfile(
        displayName: String,
        mobile: String,
        address: Address,
        role: UserRole,
        donorCategory: DonorCategory?,
        operatingArea: String,
        capacity: String
    ): ValidationResult {
        val errors = mutableMapOf<AuthField, Int>()
        validatePersonName(displayName)?.let { errors[AuthField.FULL_NAME] = it }
        validateMobile(mobile)?.let { errors[AuthField.MOBILE] = it }
        validateAddress(address, errors)
        if (role == UserRole.DONOR && donorCategory == null) {
            errors[AuthField.DONOR_CATEGORY] = R.string.error_category_required
        }
        if (role == UserRole.NGO) {
            if (operatingArea.isBlank()) {
                errors[AuthField.OPERATING_AREA] = R.string.error_operating_area_required
            }
            if (capacity.isBlank()) {
                errors[AuthField.CAPACITY] = R.string.error_capacity_required
            }
        }
        return ValidationResult(errors)
    }

    private fun validateAddress(
        address: Address,
        errors: MutableMap<AuthField, Int>
    ) {
        if (address.line1.isBlank()) {
            errors[AuthField.ADDRESS_LINE] = R.string.error_address_required
        }
        if (address.city.isBlank()) {
            errors[AuthField.CITY] = R.string.error_city_required
        }
        if (address.state.isBlank()) {
            errors[AuthField.STATE] = R.string.error_state_required
        }
        if (!pinPattern.matches(address.pinCode.trim())) {
            errors[AuthField.PIN_CODE] = R.string.error_pin_invalid
        }
    }

    @StringRes
    private fun validateEmail(email: String): Int? {
        val normalized = normalizeEmail(email)
        return when {
            normalized.isBlank() -> R.string.error_email_required
            !emailPattern.matches(normalized) -> R.string.error_email_invalid
            else -> null
        }
    }

    @StringRes
    private fun validateMobile(mobile: String): Int? {
        val normalized = normalizeMobile(mobile)
        return when {
            normalized.isBlank() -> R.string.error_mobile_required
            !mobilePattern.matches(normalized) -> R.string.error_mobile_invalid
            else -> null
        }
    }

    @StringRes
    private fun validatePassword(password: String): Int? {
        return when {
            password.isBlank() -> R.string.error_password_required
            password.length < 8 ||
                !letterPattern.containsMatchIn(password) ||
                !digitPattern.containsMatchIn(password) -> R.string.error_password_weak
            else -> null
        }
    }

    @StringRes
    private fun validatePasswordConfirmation(password: String, confirmation: String): Int? {
        return when {
            confirmation.isBlank() -> R.string.error_confirm_password_required
            password != confirmation -> R.string.error_password_mismatch
            else -> null
        }
    }

    @StringRes
    private fun validatePersonName(name: String): Int? {
        return when {
            name.isBlank() -> R.string.error_name_required
            name.trim().length < 2 -> R.string.error_name_invalid
            else -> null
        }
    }

    @StringRes
    private fun validateOrganizationName(name: String): Int? {
        return when {
            name.isBlank() -> R.string.error_ngo_name_required
            name.trim().length < 3 -> R.string.error_ngo_name_invalid
            else -> null
        }
    }
}
