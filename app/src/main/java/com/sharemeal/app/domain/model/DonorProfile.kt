package com.sharemeal.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class DonorProfile(
    val fullName: String,
    val organizationName: String? = null,
    val category: DonorCategory,
    val address: Address
)
