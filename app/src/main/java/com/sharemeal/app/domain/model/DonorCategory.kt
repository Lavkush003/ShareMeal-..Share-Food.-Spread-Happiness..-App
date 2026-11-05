package com.sharemeal.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class DonorCategory {
    WEDDING_FUNCTION,
    RESTAURANT,
    HOTEL,
    CATERING_SERVICE,
    COLLEGE_MESS,
    HOSTEL_KITCHEN,
    FOOD_PROCESSING_UNIT,
    INDIVIDUAL_DONOR,
    OTHER
}
