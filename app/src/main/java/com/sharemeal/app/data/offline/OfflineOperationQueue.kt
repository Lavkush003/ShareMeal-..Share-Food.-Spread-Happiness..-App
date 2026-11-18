package com.sharemeal.app.data.offline

import com.sharemeal.app.data.sync.SyncManager
import com.sharemeal.app.domain.model.DonationRequest
import com.sharemeal.app.domain.model.FoodDonation
import com.sharemeal.app.domain.model.PendingOperation
import com.sharemeal.app.domain.model.PickupRecord
import com.sharemeal.app.domain.model.SyncOperationType
import com.sharemeal.app.domain.model.User
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Singleton
class OfflineOperationQueue @Inject constructor(
    private val syncManager: SyncManager
) {
    suspend fun enqueueDonationCreated(donation: FoodDonation): Boolean = syncManager.enqueue(
        PendingOperation.create(
            type = SyncOperationType.CREATE_DONATION,
            entityType = "donation",
            entityId = donation.id,
            path = "donations",
            method = "POST",
            payloadJson = buildJsonObject {
                put("id", donation.id)
                put("title", donation.title)
                put("quantity", donation.quantity)
                put("pickupDeadline", donation.pickupDeadline)
            }.toString()
        )
    )

    suspend fun enqueueRequest(request: DonationRequest, type: SyncOperationType): Boolean =
        syncManager.enqueue(
            PendingOperation.create(
                type = type,
                entityType = "request",
                entityId = request.id,
                path = when (type) {
                    SyncOperationType.SUBMIT_REQUEST -> "requests"
                    SyncOperationType.APPROVE_REQUEST -> "requests/${request.id}/approve"
                    SyncOperationType.REJECT_REQUEST -> "requests/${request.id}/reject"
                    SyncOperationType.CANCEL_REQUEST -> "requests/${request.id}/cancel"
                    SyncOperationType.SCHEDULE_PICKUP -> "requests/${request.id}/schedule"
                    else -> "requests/${request.id}"
                },
                method = if (type == SyncOperationType.SUBMIT_REQUEST) "POST" else "PATCH",
                payloadJson = buildJsonObject {
                    put("id", request.id)
                    put("donationId", request.donationId)
                    put("status", request.status.name)
                    request.approvedQuantity?.let { put("approvedQuantity", it) }
                    request.pickupScheduledTime?.let { put("pickupScheduledTime", it) }
                    request.idempotencyKey?.let { put("idempotencyKey", it) }
                }.toString()
            )
        )

    suspend fun enqueuePickup(pickup: PickupRecord, type: SyncOperationType): Boolean =
        syncManager.enqueue(
            PendingOperation.create(
                type = type,
                entityType = "pickup",
                entityId = pickup.requestId,
                path = "pickups/${pickup.requestId}/${pathSegment(type)}",
                method = "POST",
                payloadJson = buildJsonObject {
                    put("requestId", pickup.requestId)
                    put("status", pickup.status.name)
                    put("scheduledAt", pickup.schedule.scheduledAt)
                }.toString()
            )
        )

    suspend fun enqueueProfile(user: User): Boolean = syncManager.enqueue(
        PendingOperation.create(
            type = SyncOperationType.UPDATE_PROFILE,
            entityType = "profile",
            entityId = user.id,
            path = "users/${user.id}/profile",
            method = "PATCH",
            payloadJson = buildJsonObject {
                put("id", user.id)
                put("displayName", user.displayName)
            }.toString()
        )
    )

    private fun pathSegment(type: SyncOperationType): String = when (type) {
        SyncOperationType.SCHEDULE_PICKUP -> "schedule"
        SyncOperationType.NGO_READY -> "ngo-ready"
        SyncOperationType.DONOR_READY -> "donor-ready"
        SyncOperationType.COLLECT -> "collect"
        SyncOperationType.DISTRIBUTE -> "distribute"
        SyncOperationType.COMPLETE_PICKUP -> "complete"
        SyncOperationType.RECORD_EXCEPTION -> "exception"
        else -> "status"
    }
}
