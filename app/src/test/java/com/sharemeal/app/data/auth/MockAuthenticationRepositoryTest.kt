package com.sharemeal.app.data.auth

import com.sharemeal.app.core.common.AppError
import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.domain.model.Address
import com.sharemeal.app.domain.model.DonorCategory
import com.sharemeal.app.domain.model.DonorRegistrationRequest
import com.sharemeal.app.domain.model.NgoRegistrationRequest
import com.sharemeal.app.domain.model.NgoVerificationStatus
import com.sharemeal.app.domain.model.PasswordResetResult
import com.sharemeal.app.domain.model.UserRole
import com.sharemeal.app.testutil.MockAuthTestHarness
import com.sharemeal.app.testutil.newHarnessWithSharedData
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MockAuthenticationRepositoryTest {

    private val address = Address(
        line1 = "10 Market Street",
        city = "Bengaluru",
        state = "Karnataka",
        pinCode = "560001"
    )

    @Test
    fun `demo donor login persists session`() = runTest {
        val harness = MockAuthTestHarness()

        val result = harness.authenticationRepository.login(
            email = "donor@sharemeal.dev",
            password = MockAccountSeeder.DemoPassword,
            rememberSession = true
        )

        assertTrue(result is AppResult.Success)
        val session = (result as AppResult.Success).data
        assertEquals(UserRole.DONOR, session.role)
        assertEquals(session, harness.sessionManager.session.value)
        assertEquals(session.userId, harness.preferences.savedSession()?.userId)
    }

    @Test
    fun `login fails with wrong password`() = runTest {
        val harness = MockAuthTestHarness()

        val result = harness.authenticationRepository.login(
            email = "donor@sharemeal.dev",
            password = "WrongPassword123",
            rememberSession = true
        )

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AppError.Unauthorized)
        assertNull(harness.sessionManager.session.value)
    }

    @Test
    fun `donor registration creates profile and session`() = runTest {
        val harness = MockAuthTestHarness()

        val result = harness.authenticationRepository.registerDonor(
            DonorRegistrationRequest(
                fullName = "Ananya Rao",
                organizationName = "Fresh Table",
                email = "ananya@example.com",
                mobile = "9876500000",
                password = "ShareMeal123!",
                category = DonorCategory.CATERING_SERVICE,
                address = address
            )
        )

        assertTrue(result is AppResult.Success)
        val session = (result as AppResult.Success).data
        assertEquals(UserRole.DONOR, session.role)

        val userResult = harness.userRepository.getCurrentUser()
        assertTrue(userResult is AppResult.Success)
        val user = (userResult as AppResult.Success).data
        assertEquals("Ananya Rao", user.displayName)
        assertEquals("Fresh Table", user.donorProfile?.organizationName)
        assertEquals(DonorCategory.CATERING_SERVICE, user.donorProfile?.category)
    }

    @Test
    fun `ngo registration starts with pending verification`() = runTest {
        val harness = MockAuthTestHarness()

        val result = harness.authenticationRepository.registerNgo(
            NgoRegistrationRequest(
                ngoName = "Community Kitchen",
                registrationNumber = "REG-100",
                contactPersonName = "Ravi Kumar",
                email = "ravi@example.com",
                mobile = "9876500001",
                password = "ShareMeal123!",
                address = address,
                operatingArea = "North Bengaluru",
                estimatedDistributionCapacity = "100 meals/day",
                verificationDocumentInfo = "Mock registration certificate"
            )
        )

        assertTrue(result is AppResult.Success)
        val userResult = harness.userRepository.getCurrentUser()
        val user = (userResult as AppResult.Success).data
        assertEquals(NgoVerificationStatus.PENDING, user.ngoProfile?.verificationStatus)
    }

    @Test
    fun `duplicate email is rejected`() = runTest {
        val harness = MockAuthTestHarness()
        val request = DonorRegistrationRequest(
            fullName = "Ananya Rao",
            organizationName = null,
            email = "ananya@example.com",
            mobile = "9876500000",
            password = "ShareMeal123!",
            category = DonorCategory.INDIVIDUAL_DONOR,
            address = address
        )

        val first = harness.authenticationRepository.registerDonor(request)
        val second = harness.authenticationRepository.registerDonor(
            request.copy(fullName = "Another Donor")
        )

        assertTrue(first is AppResult.Success)
        assertTrue(second is AppResult.Failure)
        assertTrue((second as AppResult.Failure).error is AppError.Conflict)
    }

    @Test
    fun `remembered session restores in a new repository instance`() = runTest {
        val first = MockAuthTestHarness()
        first.authenticationRepository.login(
            email = "ngo@sharemeal.dev",
            password = MockAccountSeeder.DemoPassword,
            rememberSession = true
        )

        val second = newHarnessWithSharedData(first)
        val restored = second.authenticationRepository.restoreSession()

        assertTrue(restored is AppResult.Success)
        val session = (restored as AppResult.Success).data
        assertNotNull(session)
        assertEquals(UserRole.NGO, session?.role)
    }

    @Test
    fun `logout clears session and persisted session`() = runTest {
        val harness = MockAuthTestHarness()
        harness.authenticationRepository.login(
            email = "donor@sharemeal.dev",
            password = MockAccountSeeder.DemoPassword,
            rememberSession = true
        )

        val result = harness.authenticationRepository.logout()

        assertTrue(result is AppResult.Success)
        assertNull(harness.sessionManager.session.value)
        assertNull(harness.preferences.savedSession())

        val second = newHarnessWithSharedData(harness)
        val restored = second.authenticationRepository.restoreSession()
        assertTrue(restored is AppResult.Success)
        assertNull((restored as AppResult.Success).data)
    }

    @Test
    fun `password reset clearly requires backend integration`() = runTest {
        val harness = MockAuthTestHarness()

        val result = harness.authenticationRepository.requestPasswordReset(
            "donor@sharemeal.dev"
        )

        assertTrue(result is AppResult.Success)
        assertEquals(
            PasswordResetResult.BACKEND_REQUIRED,
            (result as AppResult.Success).data
        )
    }
}
