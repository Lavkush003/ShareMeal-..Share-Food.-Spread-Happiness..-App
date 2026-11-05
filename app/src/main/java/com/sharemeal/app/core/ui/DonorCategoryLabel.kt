package com.sharemeal.app.core.ui

import androidx.annotation.StringRes
import com.sharemeal.app.R
import com.sharemeal.app.domain.model.DonorCategory

@StringRes
fun DonorCategory.labelRes(): Int = when (this) {
    DonorCategory.WEDDING_FUNCTION -> R.string.donor_category_wedding
    DonorCategory.RESTAURANT -> R.string.donor_category_restaurant
    DonorCategory.HOTEL -> R.string.donor_category_hotel
    DonorCategory.CATERING_SERVICE -> R.string.donor_category_catering
    DonorCategory.COLLEGE_MESS -> R.string.donor_category_college_mess
    DonorCategory.HOSTEL_KITCHEN -> R.string.donor_category_hostel_kitchen
    DonorCategory.FOOD_PROCESSING_UNIT -> R.string.donor_category_food_processing
    DonorCategory.INDIVIDUAL_DONOR -> R.string.donor_category_individual
    DonorCategory.OTHER -> R.string.donor_category_other
}
