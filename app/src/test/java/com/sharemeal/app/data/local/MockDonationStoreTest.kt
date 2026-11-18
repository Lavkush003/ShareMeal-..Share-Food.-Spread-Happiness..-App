package com.sharemeal.app.data.local

import com.sharemeal.app.core.common.AppError
import com.sharemeal.app.domain.model.Address
import com.sharemeal.app.domain.model.DonationQuantityCalculator
import com.sharemeal.app.domain.model.DonationRequestStatus
import com.sharemeal.app.domain.model.DonationStatus
import com.sharemeal.app.domain.model.NgoProfile
import com.sharemeal.app.domain.model.NgoVerificationStatus
import com.sharemeal.app.domain.model.User
import com.sharemeal.app.domain.model.UserRole
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest

class MockDonationStoreTest {

    private val now = System.currentTimeMillis()
    private val address = Address("1 Test Road", "Bengaluru", "Karnataka", "560001")

    private fun ngo(id: String, verified: Boolean): User = User(
        id = id,
        email = "$id@test.dev",
        mobile = "9000000001",
        role = UserRole.NGO,
        displayName = "Test NGO $id",
        ngoProfile = NgoProfile(
            ngoName = "Test NGO $id",
            registrationNumber = "REG-$id",
            contactPersonName = "Test Contact",
            address = address,
            operatingArea = "Bengaluru",
            estimatedDistributionCapacity = "100 people",
            verificationDocumentInfo = "mock document",
            verificationStatus = if (verified) NgoVerificationStatus.VERIFIED
            else NgoVerificationStatus.PENDING
        ),
        createdAt = now
    )

    private fun donor(): User = User(
        id = MockDonationSeed.DemoDonorId,
        email = "donor@test.dev",
        mobile = "9000000002",
        role = UserRole.DONOR,
        displayName = "Test Donor",
        createdAt = now
    )

    private suspend fun assertSuspendThrows(block: suspend () -> Unit): Throwable? = try {
        block()
        null
    } catch (error: Throwable) {
        error
    }

    @Test
    fun `unverified NGO cannot submit a request`() = runTest {
        val store = MockDonationStore()
        val unverified = ngo("ngo-unverified", verified = false)

        val error = assertSuspendThrows {
            store.submitRequest(
                user = unverified,
                donationId = "sample_don_3",
                requestedQuantity = 5,
                peopleToServe = 10,
                expectedPickupAt = now + 60_000L,
                notes = null,
                idempotencyKey = null
            )
        }
        assertTrue(error is AppError.Forbidden)
        assertTrue(store.requests.value.none { it.ngoId == unverified.id })
    }

    @Test
    fun `pending request does not allocate quantity but notifies donor`() = runTest {
        val store = MockDonationStore()
        val verified = ngo("ngo-verified-1", verified = true)

        val request = store.submitRequest(
            user = verified,
            donationId = "sample_don_3",
            requestedQuantity = 10,
            peopleToServe = 20,
            expectedPickupAt = now + 60_000L,
            notes = "Community kitchen",
            idempotencyKey = "key-1"
        )

        assertEquals(DonationRequestStatus.PENDING, request.status)
        assertEquals(10, request.requestedQuantity)
        assertEquals(20, request.peopleToServe)
        assertEquals("sample_don_3", request.donationId)
        assertEquals(MockDonationSeed.DemoDonorId, request.donorId)
        assertEquals(
            35,
            store.donations.value.first { it.id == "sample_don_3" }.remainingQuantity
        )
        assertTrue(
            store.notifications.value.any {
                it.recipientId == MockDonationSeed.DemoDonorId && it.requestId == request.id
            }
        )
    }

    @Test
    fun `partial approval allocates only approved quantity`() = runTest {
        val store = MockDonationStore()
        val verified = ngo("ngo-verified-2", verified = true)
        val request = store.submitRequest(
            user = verified,
            donationId = "sample_don_3",
            requestedQuantity = 10,
            peopleToServe = 20,
            expectedPickupAt = now + 60_000L,
            notes = null,
            idempotencyKey = null
        )

        val approved = store.approveRequest(
            donorId = donor().id,
            requestId = request.id,
            approvedQuantity = 6,
            pickupScheduledTime = null
        )

        assertEquals(DonationRequestStatus.APPROVED, approved.status)
        assertEquals(6, approved.approvedQuantity)
        val donation = store.donations.value.first { it.id == "sample_don_3" }
        assertEquals(29, donation.remainingQuantity)
        assertEquals(DonationStatus.PARTIALLY_ALLOCATED, donation.status)
        assertTrue(
            store.notifications.value.any {
                it.recipientId == verified.id && it.requestId == request.id
            }
        )
    }

    @Test
    fun `duplicate active request is rejected idempotently`() = runTest {
        val store = MockDonationStore()
        val verified = ngo("ngo-verified-3", verified = true)
        store.submitRequest(
            user = verified,
            donationId = "sample_don_4",
            requestedQuantity = 5,
            peopleToServe = 10,
            expectedPickupAt = now + 60_000L,
            notes = null,
            idempotencyKey = "duplicate-key"
        )

        val error = assertSuspendThrows {
            store.submitRequest(
                user = verified,
                donationId = "sample_don_4",
                requestedQuantity = 5,
                peopleToServe = 10,
                expectedPickupAt = now + 60_000L,
                notes = null,
                idempotencyKey = "duplicate-key"
            )
        }
        assertTrue(error is AppError.Conflict)
        assertEquals(1, store.requests.value.count { it.ngoId == verified.id && it.donationId == "sample_don_4" })
    }

    @Test
    fun `non owner cannot approve request`() = runTest {
        val store = MockDonationStore()
        val verified = ngo("ngo-verified-4", verified = true)
        val request = store.submitRequest(
            user = verified,
            donationId = "sample_don_4",
            requestedQuantity = 5,
            peopleToServe = 10,
            expectedPickupAt = now + 60_000L,
            notes = null,
            idempotencyKey = null
        )

        val error = assertSuspendThrows {
            store.approveRequest("not-the-donor", request.id, 5, null)
        }
        assertTrue(error is AppError.Forbidden)
    }

    @Test
    fun `concurrent approvals cannot over allocate donation`() = runTest {
        val store = MockDonationStore()
        val ngoA = ngo("ngo-concurrent-a", verified = true)
        val ngoB = ngo("ngo-concurrent-b", verified = true)
        val requestA = store.submitRequest(
            ngoA, "sample_don_4", 12, 20, now + 60_000L, null, "concurrent-a"
        )
        val requestB = store.submitRequest(
            ngoB, "sample_don_4", 12, 20, now + 60_000L, null, "concurrent-b"
        )

        val results = awaitAll(
            async { runCatching { store.approveRequest(donor().id, requestA.id, 12, null) } },
            async { runCatching { store.approveRequest(donor().id, requestB.id, 12, null) } }
        )

        assertEquals(1, results.count { it.isSuccess })
        val donation = store.donations.value.first { it.id == "sample_don_4" }
        assertEquals(8, donation.remainingQuantity)
        assertEquals(12, DonationQuantityCalculator.allocatedBy(store.requests.value, donation.id))
        assertEquals(
            1,
            store.requests.value.count {
                it.donationId == donation.id && it.status == DonationRequestStatus.APPROVED
            }
        )
    }

    @Test
    fun `cancelling an approved request releases its allocation`() = runTest {
        val store = MockDonationStore()
        val verified = ngo("ngo-cancel", verified = true)
        val request = store.submitRequest(
            verified, "sample_don_4", 8, 15, now + 60_000L, null, "cancel-key"
        )
        store.approveRequest(donor().id, request.id, 8, null)

        val cancelled = store.cancelRequest(verified.id, request.id)

        assertEquals(DonationRequestStatus.CANCELLED, cancelled.status)
        val donation = store.donations.value.first { it.id == "sample_don_4" }
        assertEquals(20, donation.remainingQuantity)
        assertEquals(DonationStatus.PUBLISHED, donation.status)
        assertNotNull(store.notifications.value.firstOrNull { it.requestId == request.id })
    }

    @Test
    fun `request ids are unique`() = runTest {
        val store = MockDonationStore()
        val first = ngo("ngo-unique-1", verified = true)
        val second = ngo("ngo-unique-2", verified = true)
        val a = store.submitRequest(first, "sample_don_3", 3, 5, now + 60_000L, null, UUID.randomUUID().toString())
        val b = store.submitRequest(second, "sample_don_3", 3, 5, now + 60_000L, null, UUID.randomUUID().toString())

        assertTrue(a.id != b.id)
    }
}
