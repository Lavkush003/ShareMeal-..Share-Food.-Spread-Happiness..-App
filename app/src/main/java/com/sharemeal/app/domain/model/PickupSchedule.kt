package com.sharemeal.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PickupSchedule(
    val scheduledAt: Long,
    val contactPerson: String,
    val collectionInstructions: String,
    val volunteerName: String,
    val volunteerPhone: String,
    val expectedQuantity: Int
)
