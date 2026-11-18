package com.sharemeal.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class FoodSortOrder {
    NEAREST,
    EARLIEST_DEADLINE,
    NEWEST
}
