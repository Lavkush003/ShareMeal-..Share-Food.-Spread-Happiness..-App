package com.sharemeal.app.data.local.database

import androidx.room.Embedded
import androidx.room.Relation

data class DonationWithRequests(
    @Embedded val donation: FoodDonationEntity,
    @Relation(parentColumn = "id", entityColumn = "donation_id") val requests: List<DonationRequestEntity>
)
