package com.sharemeal.app.data.local.database

import androidx.room.Embedded
import androidx.room.Relation

data class RequestWithPickup(
    @Embedded val request: DonationRequestEntity,
    @Relation(parentColumn = "id", entityColumn = "request_id") val pickup: PickupEntity?
)
