package com.sharemeal.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class DonationRequest(
    val id: String,
    val donationId: String,
    val ngoId: String,
    val ngoName: String,
    val requestedQuantity: Int,
    val status: DonationRequestStatus = DonationRequestStatus.PENDING,
    val requestedAt: Long,
    val pickupScheduledTime: Long? = null,
    val completedAt: Long? = null,
    val notes: String? = null,
    val approvedQuantity: Int? = null,
    val peopleToServe: Int? = null,
    val expectedPickupAt: Long? = null,
    val donorId: String? = null,
    val donationTitle: String? = null,
    val rejectionReason: String? = null,
    val idempotencyKey: String? = null,
    val updatedAt: Long? = null,
    val syncPending: Boolean = false
)
