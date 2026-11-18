package com.sharemeal.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PickupRecord(
    val requestId: String,
    val donationId: String,
    val schedule: PickupSchedule,
    val status: PickupWorkflowStatus,
    val ngoReadyAt: Long? = null,
    val donorReadyAt: Long? = null,
    val collectedAt: Long? = null,
    val actualCollectedQuantity: Int? = null,
    val quantityDiscrepancy: Int? = null,
    val discrepancyReason: String? = null,
    val exceptionReason: PickupExceptionReason? = null,
    val exceptionNote: String? = null,
    val distributionConfirmedAt: Long? = null,
    val completedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
) {
    val isComplete: Boolean get() = status == PickupWorkflowStatus.COMPLETED
}
