package com.sharemeal.app.core.validation

import com.sharemeal.app.R
import com.sharemeal.app.domain.model.Address
import com.sharemeal.app.domain.model.DonorCategory
import com.sharemeal.app.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidatorTest {

    private val validAddress = Address(
        line1 = "12 Main Road",
        city = "Bengaluru",
        state = "Karnataka",
        pinCode = "560001"
    )

    @Test
    fun `login accepts valid credentials`() {
        val result = AuthValidator.validateLogin("donor@sharemeal.dev", "ShareMeal123!")
        assertTrue(result.isValid)
    }

    @Test
    fun `login rejects malformed email and blank password`() {
        val result = AuthValidator.validateLogin("not-an-email", "")
        assertEquals(R.string.error_email_invalid, result.fieldErrors[AuthField.EMAIL])
        assertEquals(R.string.error_password_required, result.fieldErrors[AuthField.PASSWORD])
    }

    @Test
    fun `donor registration rejects weak password and missing category`() {
        val result = AuthValidator.validateDonorRegistration(
            fullName = "Aarav",
            organizationName = "",
            email = "aarav@example.com",
            mobile = "9876543210",
            password = "weak",
            confirmPassword = "weak",
            category = null,
            address = validAddress
        )
        assertEquals(R.string.error_password_weak, result.fieldErrors[AuthField.PASSWORD])
        assertEquals(
            R.string.error_category_required,
            result.fieldErrors[AuthField.DONOR_CATEGORY]
        )
    }

    @Test
    fun `donor registration accepts complete valid form`() {
        val result = AuthValidator.validateDonorRegistration(
            fullName = "Aarav Sharma",
            organizationName = "Green Leaf Kitchen",
            email = "aarav@example.com",
            mobile = "+91 98765 43210",
            password = "ShareMeal123!",
            confirmPassword = "ShareMeal123!",
            category = DonorCategory.RESTAURANT,
            address = validAddress
        )
        assertTrue(result.isValid)
    }

    @Test
    fun `registration rejects password mismatch and invalid pin`() {
        val result = AuthValidator.validateDonorRegistration(
            fullName = "Aarav Sharma",
            organizationName = "",
            email = "aarav@example.com",
            mobile = "9876543210",
            password = "ShareMeal123!",
            confirmPassword = "Different123",
            category = DonorCategory.INDIVIDUAL_DONOR,
            address = validAddress.copy(pinCode = "0123")
        )
        assertEquals(
            R.string.error_password_mismatch,
            result.fieldErrors[AuthField.CONFIRM_PASSWORD]
        )
        assertEquals(R.string.error_pin_invalid, result.fieldErrors[AuthField.PIN_CODE])
    }

    @Test
    fun `ngo registration requires verification details`() {
        val result = AuthValidator.validateNgoRegistration(
            ngoName = "Hope Foundation",
            registrationNumber = "",
            contactPersonName = "Meera Rao",
            email = "ngo@example.com",
            mobile = "9876543211",
            password = "ShareMeal123!",
            confirmPassword = "ShareMeal123!",
            address = validAddress,
            operatingArea = "",
            capacity = "",
            documentInfo = ""
        )
        assertFalse(result.isValid)
        assertEquals(
            R.string.error_operating_area_required,
            result.fieldErrors[AuthField.OPERATING_AREA]
        )
        assertEquals(R.string.error_capacity_required, result.fieldErrors[AuthField.CAPACITY])
        assertEquals(
            R.string.error_document_info_required,
            result.fieldErrors[AuthField.DOCUMENT_INFO]
        )
    }

    @Test
    fun `profile validation requires ngo operating details`() {
        val result = AuthValidator.validateProfile(
            displayName = "Hope Foundation",
            mobile = "9876543211",
            address = validAddress,
            role = UserRole.NGO,
            donorCategory = null,
            operatingArea = "",
            capacity = ""
        )
        assertEquals(
            R.string.error_operating_area_required,
            result.fieldErrors[AuthField.OPERATING_AREA]
        )
        assertEquals(R.string.error_capacity_required, result.fieldErrors[AuthField.CAPACITY])
    }
}
