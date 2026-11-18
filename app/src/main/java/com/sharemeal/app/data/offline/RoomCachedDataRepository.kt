package com.sharemeal.app.data.offline

import com.sharemeal.app.data.local.database.RoomEntityMapper
import com.sharemeal.app.data.local.database.ShareMealDao
import com.sharemeal.app.domain.model.AppNotification
import com.sharemeal.app.domain.model.DonationRequest
import com.sharemeal.app.domain.model.FoodDonation
import com.sharemeal.app.domain.model.PickupRecord
import com.sharemeal.app.domain.model.User
import com.sharemeal.app.domain.model.WorkflowEvent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RoomCachedDataRepository @Inject constructor(
    private val dao: ShareMealDao
) {
    fun observeActiveDonations(now: Long = System.currentTimeMillis()): Flow<List<FoodDonation>> =
        dao.observeActiveDonations(now).map { entities ->
            entities.map(RoomEntityMapper::toDomain)
        }

    suspend fun getDonation(donationId: String): FoodDonation? =
        dao.getDonation(donationId)?.let(RoomEntityMapper::toDomain)

    fun observeUser(userId: String): Flow<User?> =
        dao.observeUserWithProfiles(userId).map { value ->
            value?.let(RoomEntityMapper::toDomain)
        }

    fun observeDonationRequests(donationId: String): Flow<List<DonationRequest>> =
        dao.observeDonationRequests(donationId).map { entities ->
            entities.map(RoomEntityMapper::toDomain)
        }

    fun observeNgoRequests(ngoId: String): Flow<List<DonationRequest>> =
        dao.observeNgoRequests(ngoId).map { entities ->
            entities.map(RoomEntityMapper::toDomain)
        }

    fun observePickup(requestId: String): Flow<PickupRecord?> =
        dao.observePickup(requestId).map { entity ->
            entity?.let(RoomEntityMapper::toDomain)
        }

    fun observeNotifications(userId: String): Flow<List<AppNotification>> =
        dao.observeNotifications(userId).map { entities ->
            entities.map(RoomEntityMapper::toDomain)
        }

    fun observeHistory(donationId: String): Flow<List<WorkflowEvent>> =
        dao.observeHistory(donationId).map { entities ->
            entities.map(RoomEntityMapper::toDomain)
        }
}
