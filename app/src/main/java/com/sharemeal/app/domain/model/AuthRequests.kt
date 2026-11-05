package com.sharemeal.app.domain.model

data class DonorRegistrationRequest(
    val fullName: String,
    val organizationName: String?,
    val email: String,
    val mobile: String,
    val password: String,
    val category: DonorCategory,
    val address: Address
)

data class NgoRegistrationRequest(
    val ngoName: String,
    val registrationNumber: String?,
    val contactPersonName: String,
    val email: String,
    val mobile: String,
    val password: String,
    val address: Address,
    val operatingArea: String,
    val estimatedDistributionCapacity: String,
    val verificationDocumentInfo: String
)

data class ProfileUpdate(
    val displayName: String,
    val mobile: String,
    val organizationName: String?,
    val contactPersonName: String?,
    val donorCategory: DonorCategory?,
    val operatingArea: String?,
    val estimatedDistributionCapacity: String?,
    val address: Address
)
