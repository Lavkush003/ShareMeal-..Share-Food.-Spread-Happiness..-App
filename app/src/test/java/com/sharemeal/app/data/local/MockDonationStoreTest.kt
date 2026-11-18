package com.sharemeal.app.data.local

import com.sharemeal.app.core.common.AppError
import com.sharemeal.app.domain.model.Address
import com.sharemeal.app.domain.model.AppNotificationType
import com.sharemeal.app.domain.model.DistributionInput
import com.sharemeal.app.domain.model.DonationQuantityCalculator
import com.sharemeal.app.domain.model.DonationRequestStatus
import com.sharemeal.app.domain.model.DonationStatus
import com.sharemeal.app.domain.model.NgoProfile
import com.sharemeal.app.domain.model.NgoVerificationStatus
import com.sharemeal.app.domain.model.PickupExceptionReason
import com.sharemeal.app.domain.model.PickupSchedule
import com.sharemeal.app.domain.model.PickupWorkflowStatus
import com.sharemeal.app.domain.model.User
import com.sharemeal.app.domain.model.UserRole
import com.sharemeal.app.domain.model.WorkflowStage
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

    private suspend fun createScheduledRequest(
        store: MockDonationStore,
        ngoUser: User,
        quantity: Int = 10
    ): String {
        val request = store.submitRequest(
            user = ngoUser,
            donationId = "sample_don_4",
            requestedQuantity = quantity,
            peopleToServe = quantity * 2,
            expectedPickupAt = now + 60_000L,
            notes = "Community kitchen",
            idempotencyKey = UUID.randomUUID().toString()
        )
        store.approveRequest(donor().id, request.id, quantity, null)
        store.schedulePickupWorkflow(
            user = donor(),
            requestId = request.id,
            schedule = PickupSchedule(
                scheduledAt = System.currentTimeMillis() + 3_600_000L,
                contactPerson = "Sunil Mehta",
                collectionInstructions = "Loading dock assistance available.",
                volunteerName = "Ravi Kumar",
                volunteerPhone = "9000000001",
                expectedQuantity = quantity
            )
        )
        return request.id
    }

    @Test
    fun `collection requires both parties to confirm readiness`() = runTest {
        val store = MockDonationStore()
        val ngoUser = ngo("ngo-pickup-1", verified = true)
        val requestId = createScheduledRequest(store, ngoUser)

        val earlyError = assertSuspendThrows {
            store.confirmCollection(ngoUser, requestId, 10, null, null)
        }
        assertTrue(earlyError is AppError.Conflict)

        store.confirmNgoReady(ngoUser, requestId, "Ravi Kumar", "9000000001", null)
        store.confirmDonorReady(donor(), requestId, "Sunil Mehta", "Use loading dock", null)
        val collected = store.confirmCollection(ngoUser, requestId, 8, "Damaged boxes", null)

        assertEquals(PickupWorkflowStatus.COLLECTED, collected.status)
        assertEquals(8, collected.actualCollectedQuantity)
        assertEquals(2, collected.quantityDiscrepancy)
        assertEquals(DonationRequestStatus.COLLECTED, store.requests.value.first { it.id == requestId }.status)
        val donation = store.donations.value.first { it.id == "sample_don_4" }
        assertEquals(12, donation.remainingQuantity)
        assertEquals(DonationStatus.COLLECTED, donation.status)
        assertTrue(store.events.value.any { it.requestId == requestId && it.stage == WorkflowStage.FOOD_COLLECTED })
        assertTrue(
            store.notifications.value.any {
                it.requestId == requestId && it.type == AppNotificationType.PICKUP_COLLECTED
            }
        )
    }

    @Test
    fun `duplicate collection confirmation is rejected`() = runTest {
        val store = MockDonationStore()
        val ngoUser = ngo("ngo-pickup-2", verified = true)
        val requestId = createScheduledRequest(store, ngoUser)
        store.confirmNgoReady(ngoUser, requestId, "Ravi Kumar", "9000000001", null)
        store.confirmDonorReady(donor(), requestId, "Sunil Mehta", "Use loading dock", null)
        store.confirmCollection(ngoUser, requestId, 10, null, null)

        val error = assertSuspendThrows {
            store.confirmCollection(ngoUser, requestId, 10, null, null)
        }

        assertTrue(error is AppError.Conflict)
        assertEquals(1, store.events.value.count { it.requestId == requestId && it.stage == WorkflowStage.FOOD_COLLECTED })
    }

    @Test
    fun `distribution and completion record aggregate impact`() = runTest {
        val store = MockDonationStore()
        val ngoUser = ngo("ngo-pickup-3", verified = true)
        val requestId = createScheduledRequest(store, ngoUser)
        store.confirmNgoReady(ngoUser, requestId, "Ravi Kumar", "9000000001", null)
        store.confirmDonorReady(donor(), requestId, "Sunil Mehta", "Use loading dock", null)
        store.confirmCollection(ngoUser, requestId, 10, null, null)

        store.confirmDistribution(
            user = ngoUser,
            requestId = requestId,
            input = DistributionInput(
                actualReceivedQuantity = 10,
                actualDistributedQuantity = 8,
                beneficiariesServed = 12,
                distributedAt = System.currentTimeMillis(),
                location = "Community shelter",
                remainingQuantity = 2,
                notes = "Aggregate distribution only",
                evidenceRefs = listOf("photo-ref-1")
            )
        )
        val completed = store.completePickup(donor(), requestId)

        val distribution = store.distributions.value.first { it.requestId == requestId }
        assertEquals(10, distribution.actualReceivedQuantity)
        assertEquals(8, distribution.actualDistributedQuantity)
        assertEquals(12, distribution.beneficiariesServed)
        assertEquals(2, distribution.remainingQuantity)
        assertEquals(listOf("photo-ref-1"), distribution.evidenceRefs)
        assertEquals(PickupWorkflowStatus.COMPLETED, completed.status)
        assertEquals(DonationRequestStatus.COMPLETED, store.requests.value.first { it.id == requestId }.status)
        assertEquals(DonationStatus.COMPLETED, store.donations.value.first { it.id == "sample_don_4" }.status)
        assertTrue(store.events.value.any { it.requestId == requestId && it.stage == WorkflowStage.DISTRIBUTION_CONFIRMED })
        assertTrue(store.events.value.any { it.requestId == requestId && it.stage == WorkflowStage.COMPLETED })
    }

    @Test
    fun `readiness confirmations are role restricted`() = runTest {
        val store = MockDonationStore()
        val ngoUser = ngo("ngo-pickup-4", verified = true)
        val requestId = createScheduledRequest(store, ngoUser)

        val ngoReadyByDonor = assertSuspendThrows {
            store.confirmNgoReady(donor(), requestId, "Ravi Kumar", "9000000001", null)
        }
        val donorReadyByNgo = assertSuspendThrows {
            store.confirmDonorReady(ngoUser, requestId, "Sunil Mehta", "Use loading dock", null)
        }

        assertTrue(ngoReadyByDonor is AppError.Forbidden)
        assertTrue(donorReadyByNgo is AppError.Forbidden)
    }

    @Test
    fun `pickup exception releases allocation and records audit event`() = runTest {
        val store = MockDonationStore()
        val ngoUser = ngo("ngo-pickup-5", verified = true)
        val requestId = createScheduledRequest(store, ngoUser, quantity = 6)

        val exception = store.recordPickupException(
            user = ngoUser,
            requestId = requestId,
            reason = PickupExceptionReason.MISSED_PICKUP,
            note = "Vehicle breakdown"
        )

        assertEquals(PickupWorkflowStatus.MISSED, exception.status)
        assertEquals(DonationRequestStatus.CANCELLED, store.requests.value.first { it.id == requestId }.status)
        val donation = store.donations.value.first { it.id == "sample_don_4" }
        assertEquals(20, donation.remainingQuantity)
        assertEquals(DonationStatus.PUBLISHED, donation.status)
        assertTrue(store.events.value.any { it.requestId == requestId && it.stage == WorkflowStage.MISSED })
        assertTrue(
            store.notifications.value.any {
                it.requestId == requestId && it.type == AppNotificationType.PICKUP_EXCEPTION
            }
        )
    }
}
