package com.sharemeal.app.data.local.database

import com.sharemeal.app.core.media.ImageReference
import com.sharemeal.app.domain.model.DonationDraft
import com.sharemeal.app.domain.model.DonationQuantityUnit
import com.sharemeal.app.domain.model.FoodCategory
import com.sharemeal.app.domain.model.FoodClassification
import com.sharemeal.app.domain.model.SyncState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomEntityMapperTest {

    @Test
    fun draftRoundTripPreservesFormFields() {
        val draft = draft()
        val donation = RoomEntityMapper.toDomain(draft, donorId = "donor-1", id = "draft-1")
        val entity = RoomEntityMapper.toEntity(donation, isDraft = true)

        assertTrue(entity.isDraft)
        assertEquals(SyncState.PENDING.name, entity.sync.syncState)
        assertTrue(entity.sync.isDirty)
        assertEquals(draft, RoomEntityMapper.toDraft(entity))
    }

    @Test
    fun unknownEnumValuesUseSafeFallbacks() {
        val entity = RoomEntityMapper.toEntity(
            RoomEntityMapper.toDomain(draft(), donorId = "donor-1", id = "donation-1")
        ).copy(
            category = "MISSING_CATEGORY",
            quantityUnit = "PIECES",
            classification = "UNKNOWN"
        )

        val mapped = RoomEntityMapper.toDraft(entity)

        assertEquals(FoodCategory.OTHER, mapped.category)
        assertEquals(DonationQuantityUnit.OTHER, mapped.quantityUnit)
        assertEquals(FoodClassification.OTHER, mapped.classification)
    }

    private fun draft(): DonationDraft = DonationDraft(
        title = "Hot meals",
        description = "Freshly cooked lunch",
        category = FoodCategory.COOKED_MEAL,
        images = emptyList<ImageReference>(),
        quantity = 25,
        quantityUnit = DonationQuantityUnit.PLATES,
        customQuantityUnit = null,
        classification = FoodClassification.VEGETARIAN,
        preparedAt = 1_700_000_000_000L,
        pickupWindowStart = 1_700_003_600_000L,
        pickupDeadline = 1_700_007_200_000L,
        storageConditions = "Keep warm",
        packaging = "Sealed containers",
        allergenInformation = "Contains gluten",
        foodConditionDeclared = true,
        pickupAddress = "12 Main Street",
        city = "Bengaluru",
        state = "Karnataka",
        pinCode = "560001",
        contactPerson = "Asha",
        contactPhone = "9876543210",
        specialInstructions = "Call on arrival"
    )
}
