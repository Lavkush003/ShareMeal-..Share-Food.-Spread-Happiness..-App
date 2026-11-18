package com.sharemeal.app.data.local.database

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "donation_requests",
    foreignKeys = [
        ForeignKey(
            entity = FoodDonationEntity::class,
            parentColumns = ["id"],
            childColumns = ["donation_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["ngo_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["donation_id"]),
        Index(value = ["ngo_id"]),
        Index(value = ["status"]),
        Index(value = ["idempotency_key"], unique = true),
        Index(value = ["sync_state"])
    ]
)
data class DonationRequestEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "donation_id") val donationId: String,
    @ColumnInfo(name = "ngo_id") val ngoId: String,
    @ColumnInfo(name = "ngo_name") val ngoName: String,
    @ColumnInfo(name = "requested_quantity") val requestedQuantity: Int,
    val status: String,
    @ColumnInfo(name = "requested_at") val requestedAt: Long,
    @ColumnInfo(name = "pickup_scheduled_time") val pickupScheduledTime: Long?,
    @ColumnInfo(name = "completed_at") val completedAt: Long?,
    val notes: String?,
    @ColumnInfo(name = "approved_quantity") val approvedQuantity: Int?,
    @ColumnInfo(name = "people_to_serve") val peopleToServe: Int?,
    @ColumnInfo(name = "expected_pickup_at") val expectedPickupAt: Long?,
    @ColumnInfo(name = "donor_id") val donorId: String?,
    @ColumnInfo(name = "donation_title") val donationTitle: String?,
    @ColumnInfo(name = "rejection_reason") val rejectionReason: String?,
    @ColumnInfo(name = "idempotency_key") val idempotencyKey: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: Long?,
    @Embedded(prefix = "sync_") val sync: SyncMetadata = SyncMetadata()
)
