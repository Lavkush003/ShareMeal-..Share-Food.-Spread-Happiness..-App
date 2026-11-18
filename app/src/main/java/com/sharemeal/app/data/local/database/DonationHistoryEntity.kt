package com.sharemeal.app.data.local.database

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "donation_history",
    foreignKeys = [
        ForeignKey(
            entity = FoodDonationEntity::class,
            parentColumns = ["id"],
            childColumns = ["donation_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["donation_id", "created_at"]),
        Index(value = ["request_id"]),
        Index(value = ["sync_state"])
    ]
)
data class DonationHistoryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "donation_id") val donationId: String,
    @ColumnInfo(name = "request_id") val requestId: String,
    val stage: String,
    @ColumnInfo(name = "actor_id") val actorId: String,
    @ColumnInfo(name = "actor_role") val actorRole: String,
    val title: String,
    val note: String?,
    val quantity: Int?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @Embedded(prefix = "sync_") val sync: SyncMetadata = SyncMetadata()
)
