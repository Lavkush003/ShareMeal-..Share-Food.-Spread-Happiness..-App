package com.sharemeal.app.data.local.database

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pickups",
    foreignKeys = [
        ForeignKey(
            entity = DonationRequestEntity::class,
            parentColumns = ["id"],
            childColumns = ["request_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FoodDonationEntity::class,
            parentColumns = ["id"],
            childColumns = ["donation_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["request_id"], unique = true),
        Index(value = ["donation_id"]),
        Index(value = ["status"]),
        Index(value = ["sync_state"])
    ]
)
data class PickupEntity(
    @PrimaryKey
    @ColumnInfo(name = "request_id")
    val requestId: String,
    @ColumnInfo(name = "donation_id") val donationId: String,
    @ColumnInfo(name = "scheduled_at") val scheduledAt: Long,
    @ColumnInfo(name = "contact_person") val contactPerson: String,
    @ColumnInfo(name = "collection_instructions") val collectionInstructions: String,
    @ColumnInfo(name = "volunteer_name") val volunteerName: String,
    @ColumnInfo(name = "volunteer_phone") val volunteerPhone: String,
    @ColumnInfo(name = "expected_quantity") val expectedQuantity: Int,
    val status: String,
    @ColumnInfo(name = "ngo_ready_at") val ngoReadyAt: Long?,
    @ColumnInfo(name = "donor_ready_at") val donorReadyAt: Long?,
    @ColumnInfo(name = "collected_at") val collectedAt: Long?,
    @ColumnInfo(name = "actual_collected_quantity") val actualCollectedQuantity: Int?,
    @ColumnInfo(name = "quantity_discrepancy") val quantityDiscrepancy: Int?,
    @ColumnInfo(name = "discrepancy_reason") val discrepancyReason: String?,
    @ColumnInfo(name = "exception_reason") val exceptionReason: String?,
    @ColumnInfo(name = "exception_note") val exceptionNote: String?,
    @ColumnInfo(name = "distribution_confirmed_at") val distributionConfirmedAt: Long?,
    @ColumnInfo(name = "actual_received_quantity") val actualReceivedQuantity: Int?,
    @ColumnInfo(name = "actual_distributed_quantity") val actualDistributedQuantity: Int?,
    @ColumnInfo(name = "beneficiaries_served") val beneficiariesServed: Int?,
    @ColumnInfo(name = "distributed_at") val distributedAt: Long?,
    @ColumnInfo(name = "distribution_location") val distributionLocation: String?,
    @ColumnInfo(name = "remaining_distribution_quantity") val remainingDistributionQuantity: Int?,
    @ColumnInfo(name = "distribution_notes") val distributionNotes: String?,
    @ColumnInfo(name = "evidence_refs") val evidenceRefs: List<String>,
    @ColumnInfo(name = "completed_at") val completedAt: Long?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @Embedded(prefix = "sync_") val sync: SyncMetadata = SyncMetadata()
)
