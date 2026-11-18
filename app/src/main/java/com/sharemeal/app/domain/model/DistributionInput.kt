package com.sharemeal.app.domain.model

data class DistributionInput(
    val actualReceivedQuantity: Int,
    val actualDistributedQuantity: Int,
    val beneficiariesServed: Int,
    val distributedAt: Long,
    val location: String,
    val remainingQuantity: Int,
    val notes: String? = null,
    val evidenceRefs: List<String> = emptyList()
)
