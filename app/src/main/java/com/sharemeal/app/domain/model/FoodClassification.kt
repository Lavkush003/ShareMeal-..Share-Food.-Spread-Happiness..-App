package com.sharemeal.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class FoodClassification {
    VEGETARIAN,
    NON_VEGETARIAN,
    VEGAN,
    OTHER
}
