package com.sharemeal.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class FoodCategory {
    COOKED_MEAL,
    BAKERY,
    PACKAGED_FOOD,
    FRUITS_VEGETABLES,
    DAIRY,
    BEVERAGES,
    GRAINS,
    OTHER
}
