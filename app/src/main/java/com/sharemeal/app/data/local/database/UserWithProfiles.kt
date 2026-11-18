package com.sharemeal.app.data.local.database

import androidx.room.Embedded
import androidx.room.Relation

data class UserWithProfiles(
    @Embedded val user: UserEntity,
    @Relation(parentColumn = "id", entityColumn = "user_id") val donorProfile: DonorProfileEntity?,
    @Relation(parentColumn = "id", entityColumn = "user_id") val ngoProfile: NgoProfileEntity?
)
