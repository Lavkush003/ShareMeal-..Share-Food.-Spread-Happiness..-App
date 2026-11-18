package com.sharemeal.app.data.local

import com.sharemeal.app.core.common.AppError
import com.sharemeal.app.domain.model.AppNotification
import com.sharemeal.app.domain.model.AppNotificationType
import com.sharemeal.app.domain.model.DonationDraft
import com.sharemeal.app.domain.model.DonationQuantityCalculator
import com.sharemeal.app.domain.model.DonationRequest
import com.sharemeal.app.domain.model.DonationRequestStatus
import com.sharemeal.app.domain.model.DonationStatus
import com.sharemeal.app.domain.model.FoodDonation
import com.sharemeal.app.domain.model.NgoVerificationStatus
import com.sharemeal.app.domain.model.User
import com.sharemeal.app.domain.model.UserRole
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
        if (request.status != DonationRequestStatus.PICKUP_SCHEDULED &&
            request.status != DonationRequestStatus.APPROVED
        ) {
            throw AppError.Conflict("This request is not ready for collection confirmation.")
        }
        val updated = request.copy(
            status = DonationRequestStatus.COLLECTED,
            completedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        _requests.update { list -> list.map { if (it.id == requestId) updated else it } }
        updated
    }

    suspend fun markCompleted(userId: String, requestId: String): DonationRequest = mutex.withLock {
        val request = findRequest(requestId)
        val donation = findDonation(request.donationId)
        if (donation.donorId != userId) {
            throw AppError.Forbidden("Only the donation owner can confirm completion.")
        }
        if (request.status != DonationRequestStatus.COLLECTED &&
            request.status != DonationRequestStatus.DISTRIBUTED
        ) {
            throw AppError.Conflict("Only collected requests can be completed.")
        }
        val updated = request.copy(
            status = DonationRequestStatus.COMPLETED,
            completedAt = request.completedAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        _requests.update { list -> list.map { if (it.id == requestId) updated else it } }
        val remaining = donation.quantity - DonationQuantityCalculator.allocatedBy(_requests.value, donation.id)
        if (remaining == 0 && _requests.value
                .filter { it.donationId == donation.id && it.status.allocatesQuantity }
                .all { it.status == DonationRequestStatus.COMPLETED ||
                    it.status == DonationRequestStatus.COLLECTED ||
                    it.status == DonationRequestStatus.DISTRIBUTED }
        ) {
            _donations.update { list ->
                list.map { if (it.id == donation.id) it.copy(status = DonationStatus.COMPLETED) else it }
            }
        }
        updated
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

    private fun notify(
        recipientId: String,
        type: AppNotificationType,
        title: String,
        body: String,
        requestId: String,
        donationId: String
    ) {
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
    }

    private companion object {
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
