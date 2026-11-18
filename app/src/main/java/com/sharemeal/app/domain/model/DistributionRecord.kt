package com.sharemeal.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class DistributionRecord(
    val id: String,
    val requestId: String,
    val donationId: String,
    val actualReceivedQuantity: Int,
    val actualDistributedQuantity: Int,
    val beneficiariesServed: Int,
    val distributedAt: Long,
    val location: String,
    val remainingQuantity: Int,
    val notes: String? = null,
    val evidenceRefs: List<String> = emptyList(),
    val createdAt: Long
)
