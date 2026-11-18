package com.sharemeal.app.data.local.database

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "donor_profiles",
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
data class DonorProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "full_name") val fullName: String,
    @ColumnInfo(name = "organization_name") val organizationName: String?,
    val category: String,
    @ColumnInfo(name = "address_line1") val addressLine1: String,
    val city: String,
    val state: String,
    @ColumnInfo(name = "pin_code") val pinCode: String,
    @Embedded(prefix = "sync_") val sync: SyncMetadata = SyncMetadata()
)
