package com.sharemeal.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val email: String,
    val mobile: String,
    val role: UserRole,
    val displayName: String,
    val donorProfile: DonorProfile? = null,
    val ngoProfile: NgoProfile? = null,
    val createdAt: Long
) {
    val organizationName: String?
        get() = donorProfile?.organizationName ?: ngoProfile?.ngoName

    val address: Address?
        get() = donorProfile?.address ?: ngoProfile?.address
}
