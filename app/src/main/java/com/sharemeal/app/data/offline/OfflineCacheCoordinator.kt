package com.sharemeal.app.data.offline

import com.sharemeal.app.data.local.MockDonationStore
import com.sharemeal.app.data.local.database.DonationRequestEntity
import com.sharemeal.app.data.local.database.FoodDonationEntity
import com.sharemeal.app.data.local.database.PickupEntity
import com.sharemeal.app.data.local.database.RoomEntityMapper
import com.sharemeal.app.data.local.database.ShareMealDao
import com.sharemeal.app.data.local.database.SyncMetadata
import com.sharemeal.app.data.local.database.UserEntity
import com.sharemeal.app.data.notification.MockNotificationRepository
import com.sharemeal.app.data.pickup.MockPickupRepository
import com.sharemeal.app.data.request.MockDonationRequestRepository
import com.sharemeal.app.data.user.MockUserRepository
import com.sharemeal.app.domain.model.AppNotification
import com.sharemeal.app.domain.model.DistributionRecord
import com.sharemeal.app.domain.model.DonationRequest
import com.sharemeal.app.domain.model.FoodDonation
import com.sharemeal.app.domain.model.PickupRecord
import com.sharemeal.app.domain.model.SyncState
import com.sharemeal.app.domain.model.User
import com.sharemeal.app.domain.model.UserRole
import com.sharemeal.app.domain.model.WorkflowEvent
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

@Singleton
class OfflineCacheCoordinator @Inject constructor(
    private val dao: ShareMealDao,
    private val donationStore: MockDonationStore,
    private val userRepository: MockUserRepository,
    private val requestRepository: MockDonationRequestRepository,
    private val pickupRepository: MockPickupRepository,
    private val notificationRepository: MockNotificationRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val started = AtomicBoolean(false)

    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            combine(
                userRepository.currentUser,
                donationStore.donations,
                requestRepository.requests,
                pickupRepository.pickups,
                notificationRepository.notifications
            ) { user, donations, requests, pickups, notifications ->
                CacheState(
                    user = user,
                    donations = donations,
                    requests = requests,
                    pickups = pickups,
                    notifications = notifications,
                    distributions = emptyList(),
                    events = emptyList()
                )
            }
                .combine(donationStore.distributions) { state, distributions ->
                    state.copy(distributions = distributions)
                }
                .combine(donationStore.events) { state, events ->
                    state.copy(events = events)
                }
                .flowOn(Dispatchers.IO)
                .collect { state -> cache(state) }
        }
    }

    suspend fun cacheSnapshot() {
        cache(
            CacheState(
                user = userRepository.currentUser.value,
                donations = donationStore.donations.value,
                requests = requestRepository.requests.value,
                pickups = pickupRepository.pickups.value,
                notifications = notificationRepository.notifications.value,
                distributions = donationStore.distributions.value,
                events = donationStore.events.value
            )
        )
    }

    private suspend fun cache(state: CacheState) {
        cacheUsers(state)
        state.user?.let { user ->
            user.donorProfile?.let { profile ->
                dao.upsertDonorProfile(RoomEntityMapper.toEntity(user.id, profile))
            }
            user.ngoProfile?.let { profile ->
                dao.upsertNgoProfile(RoomEntityMapper.toEntity(user.id, profile))
            }
        }

        dao.upsertDonations(state.donations.map { preserveDonationSync(it) })
        dao.upsertRequests(state.requests.map { preserveRequestSync(it) })
        cachePickups(state.pickups, state.distributions)
        dao.upsertNotifications(state.notifications.map(RoomEntityMapper::toEntity))
        state.events.groupBy { it.donationId }.forEach { (donationId, events) ->
            dao.replaceHistoryForDonation(
                donationId,
                events.map(RoomEntityMapper::toEntity)
            )
        }
    }

    private suspend fun cacheUsers(state: CacheState) {
        val donors = state.donations.map { it.donorId }.toSet() +
            state.requests.mapNotNull { it.donorId }.toSet()
        val ngos = state.requests.map { it.ngoId }.toSet()
        val users = mutableListOf<UserEntity>()

        state.user?.let { users.add(preserveUserSync(it)) }
        donors.forEach { id -> users.add(placeholderUser(id, state.user, UserRole.DONOR)) }
        ngos.forEach { id -> users.add(placeholderUser(id, state.user, UserRole.NGO)) }

        dao.upsertUsers(users.distinctBy { it.id })
    }

    private fun placeholderUser(id: String, currentUser: User?, role: UserRole): UserEntity {
        if (currentUser?.id == id) return RoomEntityMapper.toEntity(currentUser)
        return UserEntity(
            id = id,
            email = "$id@cached.local",
            mobile = "",
            role = role.name,
            displayName = id,
            createdAt = 0L,
            sync = SyncMetadata(syncState = SyncState.SYNCED.name)
        )
    }

    private suspend fun cachePickups(
        pickups: List<PickupRecord>,
        distributions: List<DistributionRecord>
    ) {
        pickups.forEach { pickup ->
            var entity = preservePickupSync(pickup)
            distributions.firstOrNull { it.requestId == pickup.requestId }?.let { distribution ->
                entity = RoomEntityMapper.mergeDistribution(entity, distribution)
            }
            dao.upsertPickup(entity)
        }
    }

    private suspend fun preserveDonationSync(donation: FoodDonation): FoodDonationEntity {
        val entity = RoomEntityMapper.toEntity(donation)
        return dao.getDonation(donation.id)?.let { entity.copy(sync = it.sync) } ?: entity
    }

    private suspend fun preserveRequestSync(request: DonationRequest): DonationRequestEntity {
        val entity = RoomEntityMapper.toEntity(request)
        return dao.getRequest(request.id)?.let { entity.copy(sync = it.sync) } ?: entity
    }

    private suspend fun preservePickupSync(pickup: PickupRecord): PickupEntity {
        val entity = RoomEntityMapper.toEntity(pickup)
        return dao.getPickup(pickup.requestId)?.let { entity.copy(sync = it.sync) } ?: entity
    }

    private suspend fun preserveUserSync(user: User): UserEntity {
        val entity = RoomEntityMapper.toEntity(user)
        return dao.getUser(user.id)?.let { entity.copy(sync = it.sync) } ?: entity
    }

    private data class CacheState(
        val user: User?,
        val donations: List<FoodDonation>,
        val requests: List<DonationRequest>,
        val pickups: List<PickupRecord>,
        val notifications: List<AppNotification>,
        val distributions: List<DistributionRecord>,
        val events: List<WorkflowEvent>
    )
}
