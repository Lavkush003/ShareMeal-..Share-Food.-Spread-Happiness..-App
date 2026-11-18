package com.sharemeal.app.data.local.database

import androidx.room.Embedded
import androidx.room.Relation

data class NgoWithRequests(
    @Embedded val ngo: UserEntity,
    @Relation(parentColumn = "id", entityColumn = "ngo_id") val requests: List<DonationRequestEntity>
)
