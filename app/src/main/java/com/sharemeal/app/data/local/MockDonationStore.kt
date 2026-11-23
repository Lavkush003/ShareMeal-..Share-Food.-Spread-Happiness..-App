package com.sharemeal.app.data.local

import com.sharemeal.app.core.common.AppError
import com.sharemeal.app.domain.model.AppNotification
import com.sharemeal.app.domain.model.AppNotificationType
import com.sharemeal.app.domain.model.DonationDraft
import com.sharemeal.app.domain.model.DonationQuantityCalculator
import com.sharemeal.app.domain.model.DonationRequest
import com.sharemeal.app.domain.model.DonationRequestStatus
import com.sharemeal.app.domain.model.DistributionInput
import com.sharemeal.app.domain.model.DistributionRecord
import com.sharemeal.app.domain.model.DonationStatus
import com.sharemeal.app.domain.model.FoodDonation
import com.sharemeal.app.domain.model.NgoVerificationStatus
import com.sharemeal.app.domain.model.PickupExceptionReason
import com.sharemeal.app.domain.model.PickupRecord
import com.sharemeal.app.domain.model.PickupSchedule
import com.sharemeal.app.domain.model.PickupWorkflowRules
import com.sharemeal.app.domain.model.PickupWorkflowStatus
import com.sharemeal.app.domain.model.User
import com.sharemeal.app.domain.model.UserRole
import com.sharemeal.app.domain.model.WorkflowEvent
import com.sharemeal.app.domain.model.WorkflowStage
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * One in-process source of truth for donations, requests and local notifications.
 *
 * Every mutation runs under a single [Mutex], which gives local transactional
 * semantics: availability check + request write + allocation write + notifications
 * are applied together or not at all. This does NOT provide multi-device
 * consistency; a production backend must perform the same operation atomically.
 */
@Singleton
class MockDonationStore @Inject constructor() {

    private val _donations = MutableStateFlow(MockDonationSeed.create())
    val donations: StateFlow<List<FoodDonation>> = _donations.asStateFlow()

    private val _requests = MutableStateFlow(createInitialRequests())
    val requests: StateFlow<List<DonationRequest>> = _requests.asStateFlow()

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _pickups = MutableStateFlow(createInitialPickups())
    val pickups: StateFlow<List<PickupRecord>> = _pickups.asStateFlow()

    private val _distributions = MutableStateFlow(createInitialDistributions())
    val distributions: StateFlow<List<DistributionRecord>> = _distributions.asStateFlow()

    private val _events = MutableStateFlow(createInitialEvents())
    val events: StateFlow<List<WorkflowEvent>> = _events.asStateFlow()

    private val mutex = Mutex()

    suspend fun publish(draft: DonationDraft, donorId: String): FoodDonation = mutex.withLock {
        val donation = FoodDonation(
            id = "don_${UUID.randomUUID()}",
            donorId = donorId,
            title = draft.title,
            description = draft.description,
            category = draft.category,
            images = draft.images,
            quantity = draft.quantity,
            quantityUnit = draft.quantityUnit,
            customQuantityUnit = draft.customQuantityUnit,
            classification = draft.classification,
            preparedAt = draft.preparedAt,
            pickupWindowStart = draft.pickupWindowStart,
            pickupDeadline = draft.pickupDeadline,
            storageConditions = draft.storageConditions,
            packaging = draft.packaging,
            allergenInformation = draft.allergenInformation,
            foodConditionDeclared = draft.foodConditionDeclared,
            pickupAddress = draft.pickupAddress,
            city = draft.city,
            state = draft.state,
            pinCode = draft.pinCode,
            contactPerson = draft.contactPerson,
            contactPhone = draft.contactPhone,
            specialInstructions = draft.specialInstructions,
            createdAt = System.currentTimeMillis()
        )
        _donations.update { it + donation }
        donation
    }

    suspend fun getDonation(donationId: String): FoodDonation? = mutex.withLock {
        _donations.value.firstOrNull { it.id == donationId }
    }

    suspend fun refresh(): Unit = mutex.withLock {}

    suspend fun setAllocation(
        donationId: String,
        remainingQuantity: Int,
        status: DonationStatus
    ): FoodDonation = mutex.withLock {
        val donation = findDonation(donationId)
        require(remainingQuantity in 0..donation.quantity) {
            "Remaining quantity must be between zero and the total quantity."
        }
        val updated = donation.copy(remainingQuantity = remainingQuantity, status = status)
        _donations.update { list -> list.map { if (it.id == donationId) updated else it } }
        updated
    }

    suspend fun submitRequest(
        user: User,
        donationId: String,
        requestedQuantity: Int,
        peopleToServe: Int,
        expectedPickupAt: Long,
        notes: String?,
        idempotencyKey: String?
    ): DonationRequest = mutex.withLock {
        val profile = user.ngoProfile
        if (user.role != UserRole.NGO || profile == null) {
            throw AppError.Forbidden("Only registered NGOs can request donations.")
        }
        if (profile.verificationStatus != NgoVerificationStatus.VERIFIED) {
            throw AppError.Forbidden(
                "Verification pending: only administrator-verified NGOs can submit requests."
            )
        }
        val donation = _donations.value.firstOrNull { it.id == donationId }
            ?: throw AppError.NotFound("Donation listing not found.")
        if (donation.status != DonationStatus.PUBLISHED &&
            donation.status != DonationStatus.PARTIALLY_ALLOCATED
        ) {
            throw AppError.Conflict("This donation is no longer accepting requests.")
        }
        if (donation.pickupDeadline <= System.currentTimeMillis()) {
            throw AppError.Conflict("The pickup deadline for this donation has passed.")
        }
        if (requestedQuantity <= 0) {
            throw AppError.Validation(message = "Requested quantity must be greater than zero.")
        }
        if (peopleToServe <= 0) {
            throw AppError.Validation(message = "Number of people to serve must be greater than zero.")
        }
        if (expectedPickupAt <= 0L) {
            throw AppError.Validation(message = "Expected pickup date and time are required.")
        }

        val current = _requests.value
        val duplicate = current.any { request ->
            val sameKey = idempotencyKey != null && request.idempotencyKey == idempotencyKey
            val sameActiveRequest = request.donationId == donationId &&
                request.ngoId == user.id &&
                request.status.isActive
            sameKey || sameActiveRequest
        }
        if (duplicate) {
            throw AppError.Conflict("An active request for this donation already exists.")
        }

        val allocated = DonationQuantityCalculator.allocatedBy(current, donationId)
        val remaining = donation.quantity - allocated
        if (requestedQuantity > remaining) {
            throw AppError.Conflict(
                "Only $remaining ${donation.quantityUnit.name.lowercase()} are still available."
            )
        }

        val request = DonationRequest(
            id = "req_${UUID.randomUUID()}",
            donationId = donationId,
            ngoId = user.id,
            ngoName = profile.ngoName,
            requestedQuantity = requestedQuantity,
            status = DonationRequestStatus.PENDING,
            requestedAt = System.currentTimeMillis(),
            peopleToServe = peopleToServe,
            expectedPickupAt = expectedPickupAt,
            notes = notes,
            donorId = donation.donorId,
            donationTitle = donation.title,
            idempotencyKey = idempotencyKey,
            updatedAt = System.currentTimeMillis()
        )
        _requests.update { it + request }
        addEvent(
            request = request,
            stage = WorkflowStage.DONATION_PUBLISHED,
            actorId = donation.donorId,
            actorRole = UserRole.DONOR,
            title = "Donation published",
            quantity = donation.quantity
        )
        addEvent(
            request = request,
            stage = WorkflowStage.NGO_REQUESTED,
            actorId = user.id,
            actorRole = UserRole.NGO,
            title = "NGO requested food",
            note = notes,
            quantity = requestedQuantity
        )
        notify(
            recipientId = donation.donorId,
            type = AppNotificationType.NEW_REQUEST,
            title = "New donation request",
            body = "${profile.ngoName} requested $requestedQuantity ${donation.quantityUnit.name.lowercase()} for ${donation.title}.",
            requestId = request.id,
            donationId = donationId
        )
        request
    }

    suspend fun approveRequest(
        donorId: String,
        requestId: String,
        approvedQuantity: Int,
        pickupScheduledTime: Long?
    ): DonationRequest = mutex.withLock {
        val request = findRequest(requestId)
        val donation = findDonation(request.donationId)
        if (donation.donorId != donorId) {
            throw AppError.Forbidden("Only the donation owner can approve requests.")
        }
        if (request.status != DonationRequestStatus.PENDING) {
            throw AppError.Conflict("Only pending requests can be approved.")
        }
        if (approvedQuantity <= 0) {
            throw AppError.Validation(message = "Approved quantity must be greater than zero.")
        }
        if (approvedQuantity > request.requestedQuantity) {
            throw AppError.Validation(message = "Approved quantity cannot exceed requested quantity.")
        }

        val allocatedByOthers = DonationQuantityCalculator.allocatedBy(_requests.value, request.donationId)
        if (allocatedByOthers + approvedQuantity > donation.quantity) {
            throw AppError.Conflict("Approving this quantity would over-allocate the donation.")
        }

        val now = System.currentTimeMillis()
        val updated = request.copy(
            status = DonationRequestStatus.APPROVED,
            approvedQuantity = approvedQuantity,
            pickupScheduledTime = pickupScheduledTime,
            updatedAt = now
        )
        _requests.update { list -> list.map { if (it.id == requestId) updated else it } }
        updateDonationAllocation(donation.id, allocatedByOthers + approvedQuantity)
        addEvent(
            request = updated,
            stage = WorkflowStage.DONOR_APPROVED,
            actorId = donorId,
            actorRole = UserRole.DONOR,
            title = "Donor approved request",
            quantity = approvedQuantity
        )
        notify(
            recipientId = request.ngoId,
            type = AppNotificationType.REQUEST_APPROVED,
            title = "Request approved",
            body = "${donation.title}: $approvedQuantity ${donation.quantityUnit.name.lowercase()} approved.",
            requestId = requestId,
            donationId = donation.id
        )
        updated
    }

    suspend fun rejectRequest(
        donorId: String,
        requestId: String,
        reason: String?
    ): DonationRequest = mutex.withLock {
        val request = findRequest(requestId)
        val donation = findDonation(request.donationId)
        if (donation.donorId != donorId) {
            throw AppError.Forbidden("Only the donation owner can reject requests.")
        }
        if (request.status != DonationRequestStatus.PENDING) {
            throw AppError.Conflict("Only pending requests can be rejected.")
        }
        val updated = request.copy(
            status = DonationRequestStatus.REJECTED,
            rejectionReason = reason?.trim()?.takeIf { it.isNotEmpty() },
            updatedAt = System.currentTimeMillis()
        )
        _requests.update { list -> list.map { if (it.id == requestId) updated else it } }
        notify(
            recipientId = request.ngoId,
            type = AppNotificationType.REQUEST_REJECTED,
            title = "Request rejected",
            body = listOfNotNull(
                "${donation.title} request was rejected.",
                updated.rejectionReason?.let { "Reason: $it" }
            ).joinToString(" "),
            requestId = requestId,
            donationId = donation.id
        )
        updated
    }

    suspend fun cancelRequest(userId: String, requestId: String): DonationRequest = mutex.withLock {
        val request = findRequest(requestId)
        if (request.ngoId != userId) {
            throw AppError.Forbidden("Only the requesting NGO can cancel this request.")
        }
        if (request.status != DonationRequestStatus.PENDING &&
            request.status != DonationRequestStatus.APPROVED &&
            request.status != DonationRequestStatus.PICKUP_SCHEDULED
        ) {
            throw AppError.Conflict("This request can no longer be cancelled.")
        }
        val wasAllocating = request.status.allocatesQuantity
        val updated = request.copy(
            status = DonationRequestStatus.CANCELLED,
            updatedAt = System.currentTimeMillis()
        )
        _requests.update { list -> list.map { if (it.id == requestId) updated else it } }
        if (wasAllocating) {
            updateDonationAllocation(request.donationId, DonationQuantityCalculator.allocatedBy(_requests.value, request.donationId))
        }
        addEvent(
            request = updated,
            stage = WorkflowStage.CANCELLED,
            actorId = userId,
            actorRole = UserRole.NGO,
            title = "Request cancelled"
        )
        notify(
            recipientId = request.donorId ?: findDonation(request.donationId).donorId,
            type = AppNotificationType.REQUEST_CANCELLED,
            title = "Request cancelled",
            body = "${request.ngoName} cancelled the request for ${request.donationTitle.orEmpty()}.",
            requestId = requestId,
            donationId = request.donationId
        )
        updated
    }

    suspend fun schedulePickup(
        donorId: String,
        requestId: String,
        pickupTime: Long
    ): DonationRequest = mutex.withLock {
        val request = findRequest(requestId)
        val donation = findDonation(request.donationId)
        if (donation.donorId != donorId) {
            throw AppError.Forbidden("Only the donation owner can schedule pickup.")
        }
        if (request.status != DonationRequestStatus.APPROVED) {
            throw AppError.Conflict("Only approved requests can be scheduled for pickup.")
        }
        if (pickupTime <= 0L) {
            throw AppError.Validation(message = "Pickup time is required.")
        }
        val updated = request.copy(
            status = DonationRequestStatus.PICKUP_SCHEDULED,
            pickupScheduledTime = pickupTime,
            updatedAt = System.currentTimeMillis()
        )
        _requests.update { list -> list.map { if (it.id == requestId) updated else it } }
        val schedule = PickupSchedule(
            scheduledAt = pickupTime,
            contactPerson = donation.contactPerson,
            collectionInstructions = donation.specialInstructions.orEmpty(),
            volunteerName = request.ngoName,
            volunteerPhone = "",
            expectedQuantity = request.approvedQuantity ?: request.requestedQuantity
        )
        upsertPickup(updated, schedule, PickupWorkflowStatus.SCHEDULED)
        addEvent(
            request = updated,
            stage = WorkflowStage.PICKUP_SCHEDULED,
            actorId = donorId,
            actorRole = UserRole.DONOR,
            title = "Pickup scheduled",
            quantity = schedule.expectedQuantity
        )
        notify(
            recipientId = request.ngoId,
            type = AppNotificationType.PICKUP_SCHEDULED,
            title = "Pickup scheduled",
            body = "Pickup for ${donation.title} has been scheduled.",
            requestId = requestId,
            donationId = donation.id
        )
        updated
    }

    suspend fun markCollected(userId: String, requestId: String): DonationRequest = mutex.withLock {
        val request = findRequest(requestId)
        if (request.ngoId != userId) {
            throw AppError.Forbidden("Only the requesting NGO can confirm collection.")
        }
        collectInternal(
            actorId = userId,
            actorRole = UserRole.NGO,
            requestId = requestId,
            actualCollectedQuantity = request.approvedQuantity ?: request.requestedQuantity,
            discrepancyReason = null,
            note = null
        )
        findRequest(requestId)
    }

    suspend fun markCompleted(userId: String, requestId: String): DonationRequest = mutex.withLock {
        completeInternal(
            user = User(
                id = userId,
                email = "",
                mobile = "",
                role = UserRole.DONOR,
                displayName = "",
                donorProfile = null,
                ngoProfile = null,
                createdAt = 0L
            ),
            requestId = requestId
        )
        findRequest(requestId)
    }

    suspend fun schedulePickupWorkflow(
        user: User,
        requestId: String,
        schedule: PickupSchedule
    ): PickupRecord = mutex.withLock {
        val request = findRequest(requestId)
        val donation = findDonation(request.donationId)
        requireParty(user, request, donation)
        if (request.status != DonationRequestStatus.APPROVED &&
            request.status != DonationRequestStatus.PICKUP_SCHEDULED
        ) {
            throw AppError.Conflict("Only approved requests can be scheduled for pickup.")
        }
        if (schedule.scheduledAt <= System.currentTimeMillis()) {
            throw AppError.Validation(message = "Pickup time must be in the future.")
        }
        val expected = request.approvedQuantity ?: request.requestedQuantity
        if (schedule.expectedQuantity <= 0 || schedule.expectedQuantity > expected) {
            throw AppError.Validation(message = "Expected quantity must be between 1 and $expected.")
        }
        if (schedule.contactPerson.isBlank() || schedule.volunteerName.isBlank() ||
            schedule.collectionInstructions.isBlank()
        ) {
            throw AppError.Validation(message = "Pickup contact, volunteer and instructions are required.")
        }
        val existing = findPickupOrNull(requestId)
        if (existing != null && !existing.status.isActive) {
            throw AppError.Conflict("This pickup can no longer be rescheduled.")
        }
        val now = System.currentTimeMillis()
        val updatedRequest = request.copy(
            status = DonationRequestStatus.PICKUP_SCHEDULED,
            pickupScheduledTime = schedule.scheduledAt,
            updatedAt = now
        )
        _requests.update { list -> list.map { if (it.id == requestId) updatedRequest else it } }
        val record = upsertPickup(updatedRequest, schedule, PickupWorkflowStatus.SCHEDULED)
        addEvent(
            request = updatedRequest,
            stage = WorkflowStage.PICKUP_SCHEDULED,
            actorId = user.id,
            actorRole = user.role,
            title = "Pickup scheduled",
            note = schedule.collectionInstructions,
            quantity = schedule.expectedQuantity
        )
        notify(
            recipientId = if (user.role == UserRole.DONOR) request.ngoId else donation.donorId,
            type = AppNotificationType.PICKUP_SCHEDULED,
            title = "Pickup scheduled",
            body = "${donation.title}: pickup scheduled for ${schedule.scheduledAt}.",
            requestId = requestId,
            donationId = donation.id
        )
        record
    }

    suspend fun confirmNgoReady(
        user: User,
        requestId: String,
        volunteerName: String,
        volunteerPhone: String,
        note: String?
    ): PickupRecord = mutex.withLock {
        val request = findRequest(requestId)
        val donation = findDonation(request.donationId)
        if (user.role != UserRole.NGO || user.id != request.ngoId) {
            throw AppError.Forbidden("Only the requesting NGO can confirm pickup readiness.")
        }
        val pickup = findPickup(requestId)
        requireTransition(pickup.status, PickupWorkflowStatus.NGO_READY)
        if (volunteerName.isBlank()) {
            throw AppError.Validation(message = "Assigned volunteer is required.")
        }
        val updated = pickup.copy(
            status = PickupWorkflowStatus.NGO_READY,
            schedule = pickup.schedule.copy(
                volunteerName = volunteerName.trim(),
                volunteerPhone = volunteerPhone.trim()
            ),
            updatedAt = System.currentTimeMillis()
        ).also(::replacePickup)
        addEvent(
            request = request,
            stage = WorkflowStage.NGO_READY,
            actorId = user.id,
            actorRole = UserRole.NGO,
            title = "NGO ready for pickup",
            note = note ?: "Volunteer: ${volunteerName.trim()}"
        )
        notify(
            recipientId = donation.donorId,
            type = AppNotificationType.PICKUP_NGO_READY,
            title = "NGO ready for pickup",
            body = "${request.ngoName} is ready to collect ${donation.title}.",
            requestId = requestId,
            donationId = donation.id
        )
        updated
    }

    suspend fun confirmDonorReady(
        user: User,
        requestId: String,
        contactPerson: String,
        collectionInstructions: String,
        note: String?
    ): PickupRecord = mutex.withLock {
        val request = findRequest(requestId)
        val donation = findDonation(request.donationId)
        if (user.role != UserRole.DONOR || donation.donorId != user.id) {
            throw AppError.Forbidden("Only the donation owner can confirm handover readiness.")
        }
        val pickup = findPickup(requestId)
        requireTransition(pickup.status, PickupWorkflowStatus.DONOR_READY)
        if (contactPerson.isBlank() || collectionInstructions.isBlank()) {
            throw AppError.Validation(message = "Pickup contact and collection instructions are required.")
        }
        val updated = pickup.copy(
            status = PickupWorkflowStatus.DONOR_READY,
            schedule = pickup.schedule.copy(
                contactPerson = contactPerson.trim(),
                collectionInstructions = collectionInstructions.trim()
            ),
            updatedAt = System.currentTimeMillis()
        ).also(::replacePickup)
        addEvent(
            request = request,
            stage = WorkflowStage.DONOR_READY,
            actorId = user.id,
            actorRole = UserRole.DONOR,
            title = "Donor ready for handover",
            note = note ?: collectionInstructions.trim()
        )
        notify(
            recipientId = request.ngoId,
            type = AppNotificationType.PICKUP_DONOR_READY,
            title = "Ready for collection",
            body = "${donation.title} is ready for physical collection.",
            requestId = requestId,
            donationId = donation.id
        )
        updated
    }

    suspend fun confirmCollection(
        user: User,
        requestId: String,
        actualCollectedQuantity: Int,
        discrepancyReason: String?,
        note: String?
    ): PickupRecord = mutex.withLock {
        if (user.role != UserRole.NGO || user.id != findRequest(requestId).ngoId) {
            throw AppError.Forbidden("Only the requesting NGO can confirm physical collection.")
        }
        collectInternal(user.id, user.role, requestId, actualCollectedQuantity, discrepancyReason, note)
    }

    suspend fun confirmDistribution(
        user: User,
        requestId: String,
        input: DistributionInput
    ): PickupRecord = mutex.withLock {
        val request = findRequest(requestId)
        val donation = findDonation(request.donationId)
        if (user.role != UserRole.NGO || user.id != request.ngoId) {
            throw AppError.Forbidden("Only the requesting NGO can confirm distribution.")
        }
        val pickup = findPickup(requestId)
        requireTransition(pickup.status, PickupWorkflowStatus.DISTRIBUTED)
        val received = pickup.actualCollectedQuantity
            ?: throw AppError.Conflict("Collection must be confirmed before distribution.")
        if (input.actualReceivedQuantity <= 0 || input.actualReceivedQuantity > received) {
            throw AppError.Validation(
                message = "Actual received quantity must be between 1 and the collected quantity ($received)."
            )
        }
        if (input.actualDistributedQuantity <= 0 ||
            input.actualDistributedQuantity > input.actualReceivedQuantity
        ) {
            throw AppError.Validation(message = "Distributed quantity cannot exceed received quantity.")
        }
        if (input.beneficiariesServed <= 0) {
            throw AppError.Validation(message = "Number of beneficiaries is required.")
        }
        if (input.location.isBlank()) {
            throw AppError.Validation(message = "Distribution location is required.")
        }
        if (input.remainingQuantity < 0 ||
            input.remainingQuantity != input.actualReceivedQuantity - input.actualDistributedQuantity
        ) {
            throw AppError.Validation(message = "Remaining quantity must equal received minus distributed.")
        }

        val now = System.currentTimeMillis()
        val distribution = DistributionRecord(
            id = "dst_${UUID.randomUUID()}",
            requestId = requestId,
            donationId = donation.id,
            actualReceivedQuantity = input.actualReceivedQuantity,
            actualDistributedQuantity = input.actualDistributedQuantity,
            beneficiariesServed = input.beneficiariesServed,
            distributedAt = input.distributedAt,
            location = input.location.trim(),
            remainingQuantity = input.remainingQuantity,
            notes = input.notes?.trim()?.takeIf { it.isNotEmpty() },
            evidenceRefs = input.evidenceRefs,
            createdAt = now
        )
        _distributions.update { it + distribution }
        val updatedPickup = pickup.copy(
            status = PickupWorkflowStatus.DISTRIBUTED,
            distributionConfirmedAt = now,
            updatedAt = now
        ).also(::replacePickup)
        val updatedRequest = request.copy(
            status = DonationRequestStatus.DISTRIBUTED,
            updatedAt = now
        )
        _requests.update { list -> list.map { if (it.id == requestId) updatedRequest else it } }
        setDonationStatus(donation.id, DonationStatus.DISTRIBUTED)
        addEvent(
            request = updatedRequest,
            stage = WorkflowStage.DISTRIBUTION_CONFIRMED,
            actorId = user.id,
            actorRole = UserRole.NGO,
            title = "Distribution confirmed",
            note = input.notes,
            quantity = input.actualDistributedQuantity
        )
        notify(
            recipientId = donation.donorId,
            type = AppNotificationType.DISTRIBUTION_CONFIRMED,
            title = "Distribution confirmed",
            body = "${donation.title}: ${input.actualDistributedQuantity} distributed to ${input.beneficiariesServed} beneficiaries.",
            requestId = requestId,
            donationId = donation.id
        )
        updatedPickup
    }

    suspend fun completePickup(user: User, requestId: String): PickupRecord = mutex.withLock {
        completeInternal(user, requestId)
    }

    private fun completeInternal(user: User, requestId: String): PickupRecord {
        val request = findRequest(requestId)
        val donation = findDonation(request.donationId)
        if (user.role != UserRole.DONOR || donation.donorId != user.id) {
            throw AppError.Forbidden("Only the donation owner can complete the workflow.")
        }
        val pickup = findPickup(requestId)
        requireTransition(pickup.status, PickupWorkflowStatus.COMPLETED)
        val now = System.currentTimeMillis()
        val updatedPickup = pickup.copy(
            status = PickupWorkflowStatus.COMPLETED,
            completedAt = now,
            updatedAt = now
        ).also(::replacePickup)
        val updatedRequest = request.copy(
            status = DonationRequestStatus.COMPLETED,
            completedAt = request.completedAt ?: now,
            updatedAt = now
        )
        _requests.update { list -> list.map { if (it.id == requestId) updatedRequest else it } }
        setDonationStatus(donation.id, DonationStatus.COMPLETED)
        addEvent(
            request = updatedRequest,
            stage = WorkflowStage.COMPLETED,
            actorId = user.id,
            actorRole = UserRole.DONOR,
            title = "Donation completed"
        )
        notify(
            recipientId = request.ngoId,
            type = AppNotificationType.DONATION_COMPLETED,
            title = "Donation completed",
            body = "${donation.title} is now marked complete.",
            requestId = requestId,
            donationId = donation.id
        )
        return updatedPickup
    }

    suspend fun recordPickupException(
        user: User,
        requestId: String,
        reason: PickupExceptionReason,
        note: String?
    ): PickupRecord = mutex.withLock {
        val request = findRequest(requestId)
        val donation = findDonation(request.donationId)
        requireParty(user, request, donation)
        val pickup = findPickup(requestId)
        val targetStatus = when (reason) {
            PickupExceptionReason.MISSED_PICKUP -> PickupWorkflowStatus.MISSED
            PickupExceptionReason.CANCELLED_PICKUP -> PickupWorkflowStatus.CANCELLED
            PickupExceptionReason.DONOR_UNAVAILABLE -> PickupWorkflowStatus.DONOR_UNAVAILABLE
            PickupExceptionReason.NGO_UNABLE_TO_COLLECT -> PickupWorkflowStatus.NGO_UNABLE_TO_COLLECT
            PickupExceptionReason.SAFETY_REJECTED -> PickupWorkflowStatus.SAFETY_REJECTED
        }
        requireTransition(pickup.status, targetStatus)
        val now = System.currentTimeMillis()
        val updatedPickup = pickup.copy(
            status = targetStatus,
            exceptionReason = reason,
            exceptionNote = note?.trim()?.takeIf { it.isNotEmpty() },
            updatedAt = now
        ).also(::replacePickup)
        val updatedRequest = request.copy(
            status = DonationRequestStatus.CANCELLED,
            rejectionReason = note?.trim()?.takeIf { it.isNotEmpty() },
            updatedAt = now
        )
        _requests.update { list -> list.map { if (it.id == requestId) updatedRequest else it } }
        if (reason == PickupExceptionReason.SAFETY_REJECTED) {
            setDonationStatus(donation.id, DonationStatus.SAFETY_HOLD)
        } else {
            releaseAllocationFor(donation.id)
        }
        addEvent(
            request = updatedRequest,
            stage = when (reason) {
                PickupExceptionReason.MISSED_PICKUP -> WorkflowStage.MISSED
                PickupExceptionReason.CANCELLED_PICKUP -> WorkflowStage.CANCELLED
                PickupExceptionReason.DONOR_UNAVAILABLE -> WorkflowStage.DONOR_UNAVAILABLE
                PickupExceptionReason.NGO_UNABLE_TO_COLLECT -> WorkflowStage.NGO_UNABLE_TO_COLLECT
                PickupExceptionReason.SAFETY_REJECTED -> WorkflowStage.SAFETY_REJECTED
            },
            actorId = user.id,
            actorRole = user.role,
            title = "Pickup exception: ${reason.name}",
            note = note
        )
        notify(
            recipientId = if (user.role == UserRole.DONOR) request.ngoId else donation.donorId,
            type = AppNotificationType.PICKUP_EXCEPTION,
            title = "Pickup exception",
            body = "${donation.title}: ${reason.name.lowercase().replace('_', ' ')}.",
            requestId = requestId,
            donationId = donation.id
        )
        updatedPickup
    }

    suspend fun expireOverdueRequests(): Int = mutex.withLock {
        val now = System.currentTimeMillis()
        val expiredIds = _requests.value
            .filter { it.status == DonationRequestStatus.PENDING }
            .filter { request ->
                _donations.value.firstOrNull { it.id == request.donationId }?.pickupDeadline?.let { it <= now } == true
            }
            .map { it.id }
        if (expiredIds.isEmpty()) return@withLock 0
        _requests.update { list ->
            list.map { if (it.id in expiredIds) it.copy(
                status = DonationRequestStatus.EXPIRED,
                updatedAt = now
            ) else it }
        }
        expiredIds.size
    }

    suspend fun markNotificationRead(notificationId: String): Unit = mutex.withLock {
        _notifications.update { list ->
            list.map { if (it.id == notificationId) it.copy(isRead = true) else it }
        }
    }

    suspend fun markAllNotificationsRead(userId: String): Unit = mutex.withLock {
        _notifications.update { list ->
            list.map { if (it.recipientId == userId) it.copy(isRead = true) else it }
        }
    }

    private fun findRequest(requestId: String): DonationRequest =
        _requests.value.firstOrNull { it.id == requestId }
            ?: throw AppError.NotFound("Request not found.")

    private fun findDonation(donationId: String): FoodDonation =
        _donations.value.firstOrNull { it.id == donationId }
            ?: throw AppError.NotFound("Donation listing not found.")

    private fun updateDonationAllocation(donationId: String, allocated: Int) {
        _donations.update { list ->
            list.map { donation ->
                if (donation.id != donationId) {
                    donation
                } else {
                    val remaining = (donation.quantity - allocated).coerceAtLeast(0)
                    val status = when {
                        remaining <= 0 -> DonationStatus.FULLY_ALLOCATED
                        allocated > 0 -> DonationStatus.PARTIALLY_ALLOCATED
                        else -> DonationStatus.PUBLISHED
                    }
                    donation.copy(remainingQuantity = remaining, status = status)
                }
            }
        }
    }

    fun addNotification(
        recipientId: String,
        type: AppNotificationType,
        title: String,
        body: String,
        requestId: String? = null,
        donationId: String? = null
    ): AppNotification {
        val notification = AppNotification(
            id = "ntf_${UUID.randomUUID()}",
            recipientId = recipientId,
            type = type,
            title = title,
            body = body,
            requestId = requestId,
            donationId = donationId,
            createdAt = System.currentTimeMillis()
        )
        _notifications.update { it + notification }
        return notification
    }

    private fun notify(
        recipientId: String,
        type: AppNotificationType,
        title: String,
        body: String,
        requestId: String? = null,
        donationId: String? = null
    ) {
        addNotification(recipientId, type, title, body, requestId, donationId)
    }

    private fun requireParty(user: User, request: DonationRequest, donation: FoodDonation) {
        val allowed = (user.role == UserRole.NGO && user.id == request.ngoId) ||
            (user.role == UserRole.DONOR && user.id == donation.donorId)
        if (!allowed) {
            throw AppError.Forbidden("Only the donor or requesting NGO can perform this action.")
        }
    }

    private fun requireTransition(from: PickupWorkflowStatus, to: PickupWorkflowStatus) {
        if (!PickupWorkflowRules.canTransition(from, to)) {
            throw AppError.Conflict("Invalid pickup transition: ${from.name} to ${to.name}.")
        }
    }

    private fun findPickup(requestId: String): PickupRecord =
        findPickupOrNull(requestId)
            ?: throw AppError.NotFound("Pickup record not found.")

    private fun findPickupOrNull(requestId: String): PickupRecord? =
        _pickups.value.firstOrNull { it.requestId == requestId }

    private fun collectInternal(
        actorId: String,
        actorRole: UserRole,
        requestId: String,
        actualCollectedQuantity: Int,
        discrepancyReason: String?,
        note: String?
    ): PickupRecord {
        val request = findRequest(requestId)
        val donation = findDonation(request.donationId)
        val pickup = findPickup(requestId)
        if (!pickup.status.isCollectionReady) {
            throw AppError.Conflict("Both parties must confirm readiness before collection.")
        }
        requireTransition(pickup.status, PickupWorkflowStatus.COLLECTED)
        val approved = request.approvedQuantity ?: request.requestedQuantity
        if (actualCollectedQuantity <= 0) {
            throw AppError.Validation(message = "Collected quantity must be greater than zero.")
        }
        if (actualCollectedQuantity > approved) {
            throw AppError.Validation(
                message = "Collected quantity cannot exceed the approved quantity ($approved)."
            )
        }
        val discrepancy = approved - actualCollectedQuantity
        val now = System.currentTimeMillis()
        val updatedPickup = pickup.copy(
            status = PickupWorkflowStatus.COLLECTED,
            collectedAt = now,
            actualCollectedQuantity = actualCollectedQuantity,
            quantityDiscrepancy = discrepancy.takeIf { it != 0 },
            discrepancyReason = discrepancyReason?.trim()?.takeIf { it.isNotEmpty() },
            updatedAt = now
        ).also(::replacePickup)
        val updatedRequest = request.copy(
            status = DonationRequestStatus.COLLECTED,
            updatedAt = now
        )
        _requests.update { list -> list.map { if (it.id == requestId) updatedRequest else it } }
        val allocatedToOthers = _requests.value
            .filter {
                it.id != requestId && it.donationId == donation.id && it.status.allocatesQuantity
            }
            .sumOf { it.approvedQuantity ?: 0 }
        updateDonationAllocationWithStatus(
            donationId = donation.id,
            allocated = allocatedToOthers + actualCollectedQuantity,
            status = DonationStatus.COLLECTED
        )
        addEvent(
            request = updatedRequest,
            stage = WorkflowStage.FOOD_COLLECTED,
            actorId = actorId,
            actorRole = actorRole,
            title = "Food collected",
            note = listOfNotNull(
                discrepancyReason?.takeIf { it.isNotBlank() },
                note?.takeIf { it.isNotBlank() },
                discrepancy.takeIf { it != 0 }?.let { "Quantity discrepancy: $it" }
            ).joinToString(". ").ifBlank { null },
            quantity = actualCollectedQuantity
        )
        notify(
            recipientId = donation.donorId,
            type = AppNotificationType.PICKUP_COLLECTED,
            title = "Food collected",
            body = "${donation.title}: $actualCollectedQuantity collected by ${request.ngoName}.",
            requestId = requestId,
            donationId = donation.id
        )
        return updatedPickup
    }

    private fun upsertPickup(
        request: DonationRequest,
        schedule: PickupSchedule,
        status: PickupWorkflowStatus
    ): PickupRecord {
        val now = System.currentTimeMillis()
        val existing = findPickupOrNull(request.id)
        val record = PickupRecord(
            requestId = request.id,
            donationId = request.donationId,
            schedule = schedule,
            status = status,
            ngoReadyAt = existing?.ngoReadyAt,
            donorReadyAt = existing?.donorReadyAt,
            collectedAt = existing?.collectedAt,
            actualCollectedQuantity = existing?.actualCollectedQuantity,
            quantityDiscrepancy = existing?.quantityDiscrepancy,
            discrepancyReason = existing?.discrepancyReason,
            exceptionReason = existing?.exceptionReason,
            exceptionNote = existing?.exceptionNote,
            distributionConfirmedAt = existing?.distributionConfirmedAt,
            completedAt = existing?.completedAt,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        replacePickup(record)
        return record
    }

    private fun replacePickup(record: PickupRecord) {
        _pickups.update { list ->
            if (list.any { it.requestId == record.requestId }) {
                list.map { if (it.requestId == record.requestId) record else it }
            } else {
                list + record
            }
        }
    }

    private fun addEvent(
        request: DonationRequest,
        stage: WorkflowStage,
        actorId: String,
        actorRole: UserRole,
        title: String,
        note: String? = null,
        quantity: Int? = null
    ) {
        val event = WorkflowEvent(
            id = "evt_${UUID.randomUUID()}",
            requestId = request.id,
            donationId = request.donationId,
            stage = stage,
            actorId = actorId,
            actorRole = actorRole,
            title = title,
            note = note,
            quantity = quantity,
            createdAt = System.currentTimeMillis()
        )
        _events.update { it + event }
    }

    private fun setDonationStatus(donationId: String, status: DonationStatus) {
        _donations.update { list ->
            list.map { if (it.id == donationId) it.copy(status = status) else it }
        }
    }

    private fun releaseAllocationFor(donationId: String) {
        updateDonationAllocation(
            donationId,
            DonationQuantityCalculator.allocatedBy(_requests.value, donationId)
        )
    }

    private fun updateDonationAllocationWithStatus(
        donationId: String,
        allocated: Int,
        status: DonationStatus
    ) {
        val donation = findDonation(donationId)
        val remaining = (donation.quantity - allocated).coerceIn(0, donation.quantity)
        _donations.update { list ->
            list.map { if (it.id == donationId) it.copy(remainingQuantity = remaining, status = status) else it }
        }
    }

    private companion object {
        fun createInitialPickups(): List<PickupRecord> {
            val now = System.currentTimeMillis()
            val hour = 3_600_000L
            return listOf(
                PickupRecord(
                    requestId = "req_initial_1",
                    donationId = "sample_don_1",
                    schedule = PickupSchedule(
                        scheduledAt = now + 2 * hour,
                        contactPerson = "Aarav Sharma",
                        collectionInstructions = "Please collect from rear entrance gate 2.",
                        volunteerName = "Ravi Kumar",
                        volunteerPhone = "9000000001",
                        expectedQuantity = 10
                    ),
                    status = PickupWorkflowStatus.SCHEDULED,
                    createdAt = now - 2 * hour,
                    updatedAt = now - 2 * hour
                ),
                PickupRecord(
                    requestId = "req_initial_2",
                    donationId = "sample_don_2",
                    schedule = PickupSchedule(
                        scheduledAt = now - 25 * hour,
                        contactPerson = "Priya Nair",
                        collectionInstructions = "Park in visitor bay A.",
                        volunteerName = "Ravi Kumar",
                        volunteerPhone = "9000000001",
                        expectedQuantity = 5
                    ),
                    status = PickupWorkflowStatus.COMPLETED,
                    ngoReadyAt = now - 26 * hour,
                    donorReadyAt = now - 25 * hour,
                    collectedAt = now - 24 * hour,
                    actualCollectedQuantity = 5,
                    distributionConfirmedAt = now - 22 * hour,
                    completedAt = now - 20 * hour,
                    createdAt = now - 28 * hour,
                    updatedAt = now - 20 * hour
                )
            )
        }

        fun createInitialDistributions(): List<DistributionRecord> {
            val now = System.currentTimeMillis()
            val hour = 3_600_000L
            return listOf(
                DistributionRecord(
                    id = "dst_initial_2",
                    requestId = "req_initial_2",
                    donationId = "sample_don_2",
                    actualReceivedQuantity = 5,
                    actualDistributedQuantity = 5,
                    beneficiariesServed = 8,
                    distributedAt = now - 22 * hour,
                    location = "Community shelter, Jayanagar",
                    remainingQuantity = 0,
                    notes = "Distributed at community shelter.",
                    createdAt = now - 22 * hour
                )
            )
        }

        fun createInitialEvents(): List<WorkflowEvent> {
            val now = System.currentTimeMillis()
            val hour = 3_600_000L
            val ngoId = "mock-ngo-demo"
            val donorId = MockDonationSeed.DemoDonorId
            return listOf(
                WorkflowEvent(
                    id = "evt_initial_1_published",
                    requestId = "req_initial_1",
                    donationId = "sample_don_1",
                    stage = WorkflowStage.DONATION_PUBLISHED,
                    actorId = donorId,
                    actorRole = UserRole.DONOR,
                    title = "Donation published",
                    quantity = 50,
                    createdAt = now - 5 * hour
                ),
                WorkflowEvent(
                    id = "evt_initial_1_requested",
                    requestId = "req_initial_1",
                    donationId = "sample_don_1",
                    stage = WorkflowStage.NGO_REQUESTED,
                    actorId = ngoId,
                    actorRole = UserRole.NGO,
                    title = "NGO requested food",
                    quantity = 10,
                    createdAt = now - 4 * hour
                ),
                WorkflowEvent(
                    id = "evt_initial_1_approved",
                    requestId = "req_initial_1",
                    donationId = "sample_don_1",
                    stage = WorkflowStage.DONOR_APPROVED,
                    actorId = donorId,
                    actorRole = UserRole.DONOR,
                    title = "Donor approved request",
                    quantity = 10,
                    createdAt = now - 3 * hour
                ),
                WorkflowEvent(
                    id = "evt_initial_1_scheduled",
                    requestId = "req_initial_1",
                    donationId = "sample_don_1",
                    stage = WorkflowStage.PICKUP_SCHEDULED,
                    actorId = donorId,
                    actorRole = UserRole.DONOR,
                    title = "Pickup scheduled",
                    createdAt = now - 2 * hour
                ),
                WorkflowEvent(
                    id = "evt_initial_2_published",
                    requestId = "req_initial_2",
                    donationId = "sample_don_2",
                    stage = WorkflowStage.DONATION_PUBLISHED,
                    actorId = donorId,
                    actorRole = UserRole.DONOR,
                    title = "Donation published",
                    quantity = 25,
                    createdAt = now - 32 * hour
                ),
                WorkflowEvent(
                    id = "evt_initial_2_requested",
                    requestId = "req_initial_2",
                    donationId = "sample_don_2",
                    stage = WorkflowStage.NGO_REQUESTED,
                    actorId = ngoId,
                    actorRole = UserRole.NGO,
                    title = "NGO requested food",
                    quantity = 5,
                    createdAt = now - 28 * hour
                ),
                WorkflowEvent(
                    id = "evt_initial_2_approved",
                    requestId = "req_initial_2",
                    donationId = "sample_don_2",
                    stage = WorkflowStage.DONOR_APPROVED,
                    actorId = donorId,
                    actorRole = UserRole.DONOR,
                    title = "Donor approved request",
                    quantity = 5,
                    createdAt = now - 27 * hour
                ),
                WorkflowEvent(
                    id = "evt_initial_2_scheduled",
                    requestId = "req_initial_2",
                    donationId = "sample_don_2",
                    stage = WorkflowStage.PICKUP_SCHEDULED,
                    actorId = donorId,
                    actorRole = UserRole.DONOR,
                    title = "Pickup scheduled",
                    createdAt = now - 26 * hour
                ),
                WorkflowEvent(
                    id = "evt_initial_2_ngo_ready",
                    requestId = "req_initial_2",
                    donationId = "sample_don_2",
                    stage = WorkflowStage.NGO_READY,
                    actorId = ngoId,
                    actorRole = UserRole.NGO,
                    title = "NGO ready for pickup",
                    createdAt = now - 26 * hour
                ),
                WorkflowEvent(
                    id = "evt_initial_2_donor_ready",
                    requestId = "req_initial_2",
                    donationId = "sample_don_2",
                    stage = WorkflowStage.DONOR_READY,
                    actorId = donorId,
                    actorRole = UserRole.DONOR,
                    title = "Donor ready for handover",
                    createdAt = now - 25 * hour
                ),
                WorkflowEvent(
                    id = "evt_initial_2_collected",
                    requestId = "req_initial_2",
                    donationId = "sample_don_2",
                    stage = WorkflowStage.FOOD_COLLECTED,
                    actorId = ngoId,
                    actorRole = UserRole.NGO,
                    title = "Food collected",
                    quantity = 5,
                    createdAt = now - 24 * hour
                ),
                WorkflowEvent(
                    id = "evt_initial_2_distributed",
                    requestId = "req_initial_2",
                    donationId = "sample_don_2",
                    stage = WorkflowStage.DISTRIBUTION_CONFIRMED,
                    actorId = ngoId,
                    actorRole = UserRole.NGO,
                    title = "Distribution confirmed",
                    quantity = 5,
                    createdAt = now - 22 * hour
                ),
                WorkflowEvent(
                    id = "evt_initial_2_completed",
                    requestId = "req_initial_2",
                    donationId = "sample_don_2",
                    stage = WorkflowStage.COMPLETED,
                    actorId = donorId,
                    actorRole = UserRole.DONOR,
                    title = "Donation completed",
                    createdAt = now - 20 * hour
                )
            )
        }

        fun createInitialRequests(): List<DonationRequest> {
            val now = System.currentTimeMillis()
            val hour = 3_600_000L
            return listOf(
                DonationRequest(
                    id = "req_initial_1",
                    donationId = "sample_don_1",
                    ngoId = "mock-ngo-demo",
                    ngoName = "Hope Foundation",
                    requestedQuantity = 10,
                    approvedQuantity = 10,
                    status = DonationRequestStatus.APPROVED,
                    requestedAt = now - 4 * hour,
                    pickupScheduledTime = now + 2 * hour,
                    notes = "Pickup vehicle scheduled for 5 PM.",
                    donorId = MockDonationSeed.DemoDonorId,
                    donationTitle = "Fresh Veg Thali & Curry",
                    updatedAt = now - 2 * hour
                ),
                DonationRequest(
                    id = "req_initial_2",
                    donationId = "sample_don_2",
                    ngoId = "mock-ngo-demo",
                    ngoName = "Hope Foundation",
                    requestedQuantity = 5,
                    approvedQuantity = 5,
                    status = DonationRequestStatus.COMPLETED,
                    requestedAt = now - 28 * hour,
                    completedAt = now - 20 * hour,
                    notes = "Distributed at community shelter.",
                    donorId = MockDonationSeed.DemoDonorId,
                    donationTitle = "Assorted Fresh Pastries & Bread",
                    updatedAt = now - 20 * hour
                )
            )
        }
    }
}
