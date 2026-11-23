package com.sharemeal.app.data.local.database

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sharemeal.app.core.media.ImageReference

@Entity(
    tableName = "food_donations",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["donor_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["donor_id"]),
        Index(value = ["status"]),
        Index(value = ["pickup_deadline"]),
        Index(value = ["is_draft"]),
        Index(value = ["sync_state"])
    ]
)
data class FoodDonationEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "donor_id") val donorId: String,
    val title: String,
    val description: String,
    val category: String,
    val images: List<ImageReference>,
    val quantity: Int,
    @ColumnInfo(name = "quantity_unit") val quantityUnit: String,
    @ColumnInfo(name = "custom_quantity_unit") val customQuantityUnit: String?,
    val classification: String,
    @ColumnInfo(name = "prepared_at") val preparedAt: Long,
    @ColumnInfo(name = "pickup_window_start") val pickupWindowStart: Long,
    @ColumnInfo(name = "pickup_deadline") val pickupDeadline: Long,
    @ColumnInfo(name = "storage_conditions") val storageConditions: String,
    val packaging: String,
    @ColumnInfo(name = "allergen_information") val allergenInformation: String,
    @ColumnInfo(name = "food_condition_declared") val foodConditionDeclared: Boolean,
    @ColumnInfo(name = "pickup_address") val pickupAddress: String,
    val city: String,
    val state: String,
    @ColumnInfo(name = "pin_code") val pinCode: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @ColumnInfo(name = "contact_person") val contactPerson: String,
    @ColumnInfo(name = "contact_phone") val contactPhone: String,
    @ColumnInfo(name = "special_instructions") val specialInstructions: String?,
    val status: String,
    @ColumnInfo(name = "remaining_quantity") val remainingQuantity: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "is_draft") val isDraft: Boolean = false,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = createdAt,
    @Embedded(prefix = "sync_") val sync: SyncMetadata = SyncMetadata()
)
