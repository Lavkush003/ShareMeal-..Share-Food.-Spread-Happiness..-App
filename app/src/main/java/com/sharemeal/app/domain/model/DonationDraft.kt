package com.sharemeal.app.domain.model

import com.sharemeal.app.core.media.ImageReference

data class DonationDraft(
    val title: String,
    val description: String,
    val category: FoodCategory,
    val images: List<ImageReference>,
    val quantity: Int,
    val quantityUnit: DonationQuantityUnit,
    val customQuantityUnit: String?,
    val classification: FoodClassification,
    val preparedAt: Long,
    val pickupWindowStart: Long,
    val pickupDeadline: Long,
    val storageConditions: String,
    val packaging: String,
    val allergenInformation: String,
    val foodConditionDeclared: Boolean,
    val pickupAddress: String,
    val city: String,
    val state: String,
    val pinCode: String,
    val contactPerson: String,
    val contactPhone: String,
    val specialInstructions: String?
)
