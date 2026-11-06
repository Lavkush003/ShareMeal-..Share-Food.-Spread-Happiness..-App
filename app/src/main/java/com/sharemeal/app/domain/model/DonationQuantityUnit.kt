package com.sharemeal.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class DonationQuantityUnit {
    MEALS,
    PLATES,
    KILOGRAMS,
    LITRES,
    PACKETS,
    OTHER
}
