package com.sharemeal.app.data.local

import com.sharemeal.app.domain.model.DonationQuantityUnit
import com.sharemeal.app.domain.model.DonationStatus
import com.sharemeal.app.domain.model.FoodCategory
import com.sharemeal.app.domain.model.FoodClassification
import com.sharemeal.app.domain.model.FoodDonation

/** Shared Phase 3 seed listings. There is exactly one donor record source: [MockDonationStore]. */
object MockDonationSeed {

    fun create(now: Long = System.currentTimeMillis()): List<FoodDonation> {
        val hour = 3_600_000L
        return listOf(
            FoodDonation(
                id = "sample_don_1",
                donorId = DemoDonorId,
                title = "Fresh Veg Thali & Curry",
                description = "Surplus wholesome vegetarian meal packages containing rice, dal, roti, and mixed vegetable curry. Prepared in clean commercial kitchen.",
                category = FoodCategory.COOKED_MEAL,
                images = emptyList(),
                quantity = 50,
                remainingQuantity = 40,
                status = DonationStatus.PARTIALLY_ALLOCATED,
                quantityUnit = DonationQuantityUnit.MEALS,
                customQuantityUnit = null,
                classification = FoodClassification.VEGETARIAN,
                preparedAt = now - hour,
                pickupWindowStart = now - hour,
                pickupDeadline = now + 4 * hour,
                storageConditions = "Stored in insulated food containers at 65°C.",
                packaging = "Packed in sealed eco-friendly meal boxes.",
                allergenInformation = "Contains dairy (ghee) and gluten (wheat roti).",
                foodConditionDeclared = true,
                pickupAddress = "12, 4th Block, Jayanagar",
                city = "Bengaluru",
                state = "Karnataka",
                pinCode = "560011",
                contactPerson = "Aarav Sharma",
                contactPhone = "9876543210",
                specialInstructions = "Please collect from rear entrance gate 2.",
                createdAt = now - hour
            ),
            FoodDonation(
                id = "sample_don_2",
                donorId = DemoDonorId,
                title = "Assorted Fresh Pastries & Bread",
                description = "Freshly baked artisan bread loaves, sandwich rolls, and eggless pastries from today's batch.",
                category = FoodCategory.BAKERY,
                images = emptyList(),
                quantity = 25,
                quantityUnit = DonationQuantityUnit.PACKETS,
                customQuantityUnit = null,
                classification = FoodClassification.VEGETARIAN,
                preparedAt = now - 3 * hour,
                pickupWindowStart = now - 2 * hour,
                pickupDeadline = now + 6 * hour,
                storageConditions = "Stored at room temperature in bakery crates.",
                packaging = "Individual paper bag packaging.",
                allergenInformation = "Contains wheat, gluten, and nuts.",
                foodConditionDeclared = true,
                pickupAddress = "88, MG Road, Central District",
                city = "Bengaluru",
                state = "Karnataka",
                pinCode = "560001",
                contactPerson = "Priya Nair",
                contactPhone = "9876543212",
                specialInstructions = "Park in visitor bay A.",
                createdAt = now - 3 * hour
            ),
            FoodDonation(
                id = "sample_don_3",
                donorId = DemoDonorId,
                title = "Chicken Biryani & Salan Boxes",
                description = "High-quality chicken biryani meal portions remaining from lunch catering event.",
                category = FoodCategory.COOKED_MEAL,
                images = emptyList(),
                quantity = 35,
                quantityUnit = DonationQuantityUnit.MEALS,
                customQuantityUnit = null,
                classification = FoodClassification.NON_VEGETARIAN,
                preparedAt = now - 2 * hour,
                pickupWindowStart = now - hour,
                pickupDeadline = now + 3 * hour,
                storageConditions = "Kept hot in stainless steel insulated dispensers.",
                packaging = "Packed in heavy-duty foil containers.",
                allergenInformation = "Contains chicken, spices, cashew nuts, and dairy.",
                foodConditionDeclared = true,
                pickupAddress = "202, Indiranagar 100ft Road",
                city = "Bengaluru",
                state = "Karnataka",
                pinCode = "560038",
                contactPerson = "Karan Verma",
                contactPhone = "9876543213",
                specialInstructions = "Call contact person upon arrival for entry pass.",
                createdAt = now - 2 * hour
            ),
            FoodDonation(
                id = "sample_don_4",
                donorId = DemoDonorId,
                title = "Fresh Organic Fruit Crates",
                description = "Assorted fresh seasonal fruits including apples, bananas, oranges, and pomegranates.",
                category = FoodCategory.FRUITS_VEGETABLES,
                images = emptyList(),
                quantity = 20,
                quantityUnit = DonationQuantityUnit.KILOGRAMS,
                customQuantityUnit = null,
                classification = FoodClassification.VEGAN,
                preparedAt = now - 5 * hour,
                pickupWindowStart = now - 4 * hour,
                pickupDeadline = now + 12 * hour,
                storageConditions = "Refrigerated display storage.",
                packaging = "Packed in ventilated wooden crates.",
                allergenInformation = "None",
                foodConditionDeclared = true,
                pickupAddress = "15, Koramangala 5th Block",
                city = "Bengaluru",
                state = "Karnataka",
                pinCode = "560095",
                contactPerson = "Sunil Mehta",
                contactPhone = "9876543214",
                specialInstructions = "Loading dock assistance available.",
                createdAt = now - 5 * hour
            )
        )
    }

    const val DemoDonorId = "mock-donor-demo"
}
