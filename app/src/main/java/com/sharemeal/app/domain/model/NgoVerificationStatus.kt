package com.sharemeal.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class NgoVerificationStatus {
    PENDING,
    VERIFIED,
    REJECTED
}
