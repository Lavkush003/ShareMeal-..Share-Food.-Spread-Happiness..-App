package com.sharemeal.app.data.donation

import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.domain.model.DonationDraft
import com.sharemeal.app.domain.model.FoodDonation
import com.sharemeal.app.domain.repository.DonationRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Singleton
class MockDonationRepository @Inject constructor() : DonationRepository {

    private val _donations = MutableStateFlow<List<FoodDonation>>(emptyList())
    override val donations: StateFlow<List<FoodDonation>> = _donations.asStateFlow()

    override suspend fun publishDonation(draft: DonationDraft): AppResult<FoodDonation> {
        val donation = draft.toFoodDonation()
        _donations.update { current -> current + donation }
        return AppResult.Success(donation)
    }

    private fun DonationDraft.toFoodDonation(): FoodDonation = FoodDonation(
        id = "don_${UUID.randomUUID()}",
        donorId = "mock-donor",
        title = title,
        description = description,
        category = category,
        images = images,
        quantity = quantity,
        quantityUnit = quantityUnit,
        customQuantityUnit = customQuantityUnit,
        classification = classification,
        preparedAt = preparedAt,
        pickupWindowStart = pickupWindowStart,
        pickupDeadline = pickupDeadline,
        storageConditions = storageConditions,
        packaging = packaging,
        allergenInformation = allergenInformation,
        foodConditionDeclared = foodConditionDeclared,
        pickupAddress = pickupAddress,
        city = city,
        state = state,
        pinCode = pinCode,
        contactPerson = contactPerson,
        contactPhone = contactPhone,
        specialInstructions = specialInstructions,
        createdAt = System.currentTimeMillis()
    )
}
