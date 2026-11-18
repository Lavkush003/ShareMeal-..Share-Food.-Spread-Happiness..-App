package com.sharemeal.app.data.local.database

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ngo_profiles",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["user_id"], unique = true)]
)
data class NgoProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "ngo_name") val ngoName: String,
    @ColumnInfo(name = "registration_number") val registrationNumber: String?,
    @ColumnInfo(name = "contact_person_name") val contactPersonName: String,
    @ColumnInfo(name = "address_line1") val addressLine1: String,
    val city: String,
    val state: String,
    @ColumnInfo(name = "pin_code") val pinCode: String,
    @ColumnInfo(name = "operating_area") val operatingArea: String,
    @ColumnInfo(name = "estimated_distribution_capacity") val estimatedDistributionCapacity: String,
    @ColumnInfo(name = "verification_document_info") val verificationDocumentInfo: String,
    @ColumnInfo(name = "verification_status") val verificationStatus: String,
    @Embedded(prefix = "sync_") val sync: SyncMetadata = SyncMetadata()
)
