package com.sharemeal.app.data.local.database

import androidx.room.Embedded
import androidx.room.Relation

data class DonorWithDonations(
    @Embedded val donor: UserEntity,
    @Relation(parentColumn = "id", entityColumn = "donor_id") val donations: List<FoodDonationEntity>
)
