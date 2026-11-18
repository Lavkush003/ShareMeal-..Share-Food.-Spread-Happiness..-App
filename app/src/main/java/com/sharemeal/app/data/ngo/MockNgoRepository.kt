package com.sharemeal.app.data.ngo

import com.sharemeal.app.core.common.AppError
import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.data.local.MockDonationStore
import com.sharemeal.app.domain.model.DonationRequest
import com.sharemeal.app.domain.model.DonationRequestStatus
import com.sharemeal.app.domain.model.DonationStatus
import com.sharemeal.app.domain.model.NgoActivity
import com.sharemeal.app.domain.model.NgoActivityType
import com.sharemeal.app.domain.model.NgoDashboardStats
import com.sharemeal.app.domain.model.NgoVerificationStatus
import com.sharemeal.app.domain.repository.DonationRequestRepository
import com.sharemeal.app.domain.repository.NgoRepository
import com.sharemeal.app.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow

@Singleton
class MockNgoRepository @Inject constructor(
    private val store: MockDonationStore,
    private val requestRepository: DonationRequestRepository,
    private val userRepository: UserRepository
) : NgoRepository {

    override val donationRequests: StateFlow<List<DonationRequest>> = store.requests

    override suspend fun getDashboardStats(ngoId: String): AppResult<NgoDashboardStats> {
        val user = userRepository.currentUser.value
            ?: when (val result = userRepository.getCurrentUser()) {
                is AppResult.Success -> result.data
                is AppResult.Failure -> return AppResult.Failure(result.error)
            }
        val profile = user.ngoProfile
            ?: return AppResult.Failure(AppError.Validation(message = "Current user is not an NGO."))

        val donations = store.donations.value
        val requests = store.requests.value.filter { it.ngoId == ngoId }

        val available = donations.count {
            (it.status == DonationStatus.PUBLISHED || it.status == DonationStatus.PARTIALLY_ALLOCATED) &&
                it.remainingQuantity > 0
        }
        val pending = requests.count { it.status == DonationRequestStatus.PENDING }
        val approved = requests.count {
            it.status == DonationRequestStatus.APPROVED || it.status == DonationRequestStatus.PICKUP_SCHEDULED
        }
        val upcomingPickups = requests.count {
            it.status == DonationRequestStatus.PICKUP_SCHEDULED &&
                (it.pickupScheduledTime ?: 0L) > System.currentTimeMillis()
        }
        val completed = requests.count {
            it.status == DonationRequestStatus.COMPLETED || it.status == DonationRequestStatus.DISTRIBUTED
        }
        val totalMeals = requests
            .filter {
                it.status == DonationRequestStatus.COLLECTED ||
                    it.status == DonationRequestStatus.COMPLETED ||
                    it.status == DonationRequestStatus.DISTRIBUTED
            }
            .sumOf { it.approvedQuantity ?: it.requestedQuantity }

        return AppResult.Success(
            NgoDashboardStats(
                ngoName = profile.ngoName,
                verificationStatus = profile.verificationStatus,
                availableNearbyDonationsCount = available,
                pendingRequestsCount = pending,
                approvedCollectionsCount = approved,
                upcomingPickupsCount = upcomingPickups,
                completedDistributionsCount = completed,
                totalMealsCollected = totalMeals,
                recentActivities = buildRecentActivities(profile.verificationStatus, requests)
            )
        )
    }

    override suspend fun requestDonation(
        donationId: String,
        requestedQuantity: Int,
        notes: String?
    ): AppResult<DonationRequest> {
        val pickupTime = store.getDonation(donationId)?.pickupDeadline ?: 0L
        return requestRepository.submitRequest(
            donationId = donationId,
            requestedQuantity = requestedQuantity,
            peopleToServe = 1,
            expectedPickupAt = pickupTime,
            notes = notes
        )
    }

    override suspend fun updateRequestStatus(
        requestId: String,
        status: DonationRequestStatus
    ): AppResult<DonationRequest> = when (status) {
        DonationRequestStatus.COLLECTED -> requestRepository.markCollected(requestId)
        else -> AppResult.Failure(
            AppError.Conflict("This status change must use the request workflow actions.")
        )
    }

    override suspend fun toggleVerificationStatus(ngoId: String): AppResult<NgoVerificationStatus> {
        val user = userRepository.currentUser.value ?: return AppResult.Failure(AppError.Unauthorized())
        val target = when (user.ngoProfile?.verificationStatus ?: NgoVerificationStatus.PENDING) {
            NgoVerificationStatus.PENDING -> NgoVerificationStatus.VERIFIED
            NgoVerificationStatus.VERIFIED -> NgoVerificationStatus.REJECTED
            NgoVerificationStatus.REJECTED -> NgoVerificationStatus.PENDING
        }
        return setVerificationStatus(ngoId, target)
    }

    override suspend fun setVerificationStatus(
        ngoId: String,
        status: NgoVerificationStatus
    ): AppResult<NgoVerificationStatus> = when (
        val result = userRepository.setNgoVerificationStatus(ngoId, status)
    ) {
        is AppResult.Success -> AppResult.Success(status)
        is AppResult.Failure -> AppResult.Failure(result.error)
    }

    private fun buildRecentActivities(
        status: NgoVerificationStatus,
        requests: List<DonationRequest>
    ): List<NgoActivity> {
        val activities = mutableListOf(
            NgoActivity(
                id = "act_verif",
                title = "Verification Status: ${status.name}",
                description = when (status) {
                    NgoVerificationStatus.VERIFIED -> "Your NGO is verified and authorized for food pickup."
                    NgoVerificationStatus.PENDING -> "Document verification is under administrator review."
                    NgoVerificationStatus.REJECTED -> "Verification was not approved. Update your profile details."
                },
                timestamp = System.currentTimeMillis() - 86_400_000L,
                type = NgoActivityType.VERIFICATION_UPDATE
            )
        )
        requests.forEach { request ->
            val activity = when (request.status) {
                DonationRequestStatus.PENDING -> NgoActivityType.REQUEST_SUBMITTED to
                    "Requested ${request.requestedQuantity} units for collection."
                DonationRequestStatus.APPROVED -> NgoActivityType.REQUEST_APPROVED to
                    "Donor approved ${request.approvedQuantity ?: request.requestedQuantity} units."
                DonationRequestStatus.PICKUP_SCHEDULED -> NgoActivityType.PICKUP_SCHEDULED to
                    "Pickup has been scheduled."
                DonationRequestStatus.COLLECTED,
                DonationRequestStatus.COMPLETED,
                DonationRequestStatus.DISTRIBUTED -> NgoActivityType.DONATION_COLLECTED to
                    "Collected ${request.approvedQuantity ?: request.requestedQuantity} units."
                else -> NgoActivityType.REQUEST_SUBMITTED to "Status: ${request.status.name}"
            }
            activities.add(
                NgoActivity(
                    id = "act_${request.id}",
                    title = activity.first.name.replace('_', ' ').lowercase()
                        .replaceFirstChar { it.uppercase() },
                    description = activity.second,
                    timestamp = request.updatedAt ?: request.requestedAt,
                    type = activity.first
                )
            )
        }
        return activities.sortedByDescending { it.timestamp }
    }
}
