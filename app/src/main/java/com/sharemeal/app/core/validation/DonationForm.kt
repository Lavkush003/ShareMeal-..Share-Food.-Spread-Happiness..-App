package com.sharemeal.app.core.validation

import com.sharemeal.app.core.media.ImageReference
import com.sharemeal.app.domain.model.DonationQuantityUnit
import com.sharemeal.app.domain.model.FoodCategory
import com.sharemeal.app.domain.model.FoodClassification

data class DonationForm(
    val title: String = "",
    val description: String = "",
    val category: FoodCategory? = null,
    val images: List<ImageReference> = emptyList(),
    val quantity: String = "",
    val quantityUnit: DonationQuantityUnit? = null,
    val customQuantityUnit: String = "",
    val classification: FoodClassification? = null,
    val preparationDateMillis: Long? = null,
    val preparationTimeMinutes: Int? = null,
    val pickupStartMinutes: Int? = null,
    val pickupDeadlineMinutes: Int? = null,
    val storageConditions: String = "",
    val packaging: String = "",
    val allergenInformation: String = "",
    val conditionDeclared: Boolean = false,
    val pickupAddress: String = "",
    val city: String = "",
    val state: String = "",
    val pinCode: String = "",
    val contactPerson: String = "",
    val contactPhone: String = "",
    val specialInstructions: String = ""
)
