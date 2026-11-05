package com.sharemeal.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class NgoProfile(
    val ngoName: String,
    val registrationNumber: String? = null,
    val contactPersonName: String,
    val address: Address,
    val operatingArea: String,
    val estimatedDistributionCapacity: String,
    val verificationDocumentInfo: String,
    val verificationStatus: NgoVerificationStatus = NgoVerificationStatus.PENDING
)
