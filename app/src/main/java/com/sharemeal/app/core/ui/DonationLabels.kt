package com.sharemeal.app.core.ui

import androidx.annotation.StringRes
import com.sharemeal.app.R
import com.sharemeal.app.domain.model.DonationQuantityUnit
import com.sharemeal.app.domain.model.FoodCategory
import com.sharemeal.app.domain.model.FoodClassification

@StringRes
fun FoodCategory.labelRes(): Int = when (this) {
    FoodCategory.COOKED_MEAL -> R.string.donation_category_cooked_meal
    FoodCategory.BAKERY -> R.string.donation_category_bakery
    FoodCategory.PACKAGED_FOOD -> R.string.donation_category_packaged_food
    FoodCategory.FRUITS_VEGETABLES -> R.string.donation_category_fruits_vegetables
    FoodCategory.DAIRY -> R.string.donation_category_dairy
    FoodCategory.BEVERAGES -> R.string.donation_category_beverages
    FoodCategory.GRAINS -> R.string.donation_category_grains
    FoodCategory.OTHER -> R.string.donation_category_other
}

@StringRes
fun FoodClassification.labelRes(): Int = when (this) {
    FoodClassification.VEGETARIAN -> R.string.donation_classification_vegetarian
    FoodClassification.NON_VEGETARIAN -> R.string.donation_classification_non_vegetarian
    FoodClassification.VEGAN -> R.string.donation_classification_vegan
    FoodClassification.OTHER -> R.string.donation_classification_other
}

@StringRes
fun DonationQuantityUnit.labelRes(): Int = when (this) {
    DonationQuantityUnit.MEALS -> R.string.donation_unit_meals
    DonationQuantityUnit.PLATES -> R.string.donation_unit_plates
    DonationQuantityUnit.KILOGRAMS -> R.string.donation_unit_kilograms
    DonationQuantityUnit.LITRES -> R.string.donation_unit_litres
    DonationQuantityUnit.PACKETS -> R.string.donation_unit_packets
    DonationQuantityUnit.OTHER -> R.string.donation_unit_other
}
