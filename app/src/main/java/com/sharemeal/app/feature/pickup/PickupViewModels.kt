package com.sharemeal.app.feature.pickup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.core.navigation.Routes.ARG_PICKUP_ACTION
import com.sharemeal.app.core.navigation.Routes.ARG_PICKUP_MODE
import com.sharemeal.app.core.navigation.Routes.ARG_REQUEST_ID
import com.sharemeal.app.domain.model.DistributionInput
import com.sharemeal.app.domain.model.DistributionRecord
import com.sharemeal.app.domain.model.DonationRequest
import com.sharemeal.app.domain.model.FoodDonation
import com.sharemeal.app.domain.model.PickupExceptionReason
import com.sharemeal.app.domain.model.PickupRecord
import com.sharemeal.app.domain.model.PickupTimelineEntry
import com.sharemeal.app.domain.model.PickupWorkflowRules
import com.sharemeal.app.domain.model.UserRole
import com.sharemeal.app.domain.model.WorkflowEvent
import com.sharemeal.app.domain.repository.DonationRepository
import com.sharemeal.app.domain.repository.DonationRequestRepository
import com.sharemeal.app.domain.repository.PickupRepository
import com.sharemeal.app.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PickupListMode { UPCOMING, HISTORY, COMPLETED }

enum class PickupActionType {
    SCHEDULE,
    NGO_READY,
    DONOR_READY,
    COLLECTION,
    DISTRIBUTION,
    COMPLETE,
    EXCEPTION;

    companion object {
        fun from(value: String?): PickupActionType? = entries.firstOrNull { it.name == value }
    }
}

data class PickupListItem(
    val request: DonationRequest,
    val pickup: PickupRecord,
    val distribution: DistributionRecord?
)

data class PickupListUiState(
    val isLoading: Boolean = true,
    val items: List<PickupListItem> = emptyList()
)

@HiltViewModel
class PickupListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val requestRepository: DonationRequestRepository,
    private val pickupRepository: PickupRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val mode = PickupListMode.entries.firstOrNull {
        it.name == savedStateHandle.get<String>(ARG_PICKUP_MODE)
    } ?: PickupListMode.UPCOMING

    private val _uiState = MutableStateFlow(PickupListUiState())
    val uiState: StateFlow<PickupListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val user = currentUser()
            if (user == null) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }
            combine(
                requestRepository.requests,
                pickupRepository.pickups,
                pickupRepository.distributions
            ) { requests, pickups, distributions ->
                val distributionByRequest = distributions.associateBy { it.requestId }
                requests.mapNotNull { request ->
                    val pickup = pickups.firstOrNull { it.requestId == request.id } ?: return@mapNotNull null
                    val isMine = when (user.role) {
                        UserRole.NGO -> request.ngoId == user.id
                        UserRole.DONOR -> request.donorId == user.id
                    }
                    if (!isMine) return@mapNotNull null
                    PickupListItem(request, pickup, distributionByRequest[request.id])
                }
            }.collect { items ->
                val filtered = when (mode) {
                    PickupListMode.UPCOMING -> items
                        .filter { it.pickup.status.isActive }
                        .sortedBy { it.pickup.schedule.scheduledAt }
                    PickupListMode.HISTORY -> items
                        .filter { it.pickup.status.isTerminal }
                        .sortedByDescending { it.pickup.updatedAt }
                    PickupListMode.COMPLETED -> items
                        .filter { it.pickup.status == com.sharemeal.app.domain.model.PickupWorkflowStatus.COMPLETED }
                        .sortedByDescending { it.pickup.completedAt ?: it.pickup.updatedAt }
                }
                _uiState.update { it.copy(isLoading = false, items = filtered) }
            }
        }
    }

    private suspend fun currentUser() = userRepository.currentUser.value
        ?: (userRepository.getCurrentUser() as? AppResult.Success)?.data
}

data class PickupDetailUiState(
    val isLoading: Boolean = true,
    val request: DonationRequest? = null,
    val donation: FoodDonation? = null,
    val pickup: PickupRecord? = null,
    val distribution: DistributionRecord? = null,
    val timeline: List<PickupTimelineEntry> = emptyList(),
    val events: List<WorkflowEvent> = emptyList(),
    val isDonor: Boolean = false,
    val errorText: String? = null
)

@HiltViewModel
class PickupDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val requestRepository: DonationRequestRepository,
    private val donationRepository: DonationRepository,
    private val pickupRepository: PickupRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val requestId: String? = savedStateHandle[ARG_REQUEST_ID]

    private val _uiState = MutableStateFlow(PickupDetailUiState())
    val uiState: StateFlow<PickupDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val user = currentUser()
            combine(
                requestRepository.requests,
                pickupRepository.pickups,
                pickupRepository.distributions,
                pickupRepository.events
            ) { requests, pickups, distributions, events ->
                val request = requests.firstOrNull { it.id == requestId }
                val pickup = pickups.firstOrNull { it.requestId == requestId }
                val requestEvents = events.filter { it.requestId == requestId }.sortedBy { it.createdAt }
                val donation = request?.let {
                    (donationRepository.getDonationById(it.donationId) as? AppResult.Success)?.data
                }
                PickupDetailBundle(
                    request = request,
                    donation = donation,
                    pickup = pickup,
                    distribution = distributions.firstOrNull { it.requestId == requestId },
                    events = requestEvents,
                    isDonor = user?.role == UserRole.DONOR
                )
            }.collect { bundle ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        request = bundle.request,
                        donation = bundle.donation,
                        pickup = bundle.pickup,
                        distribution = bundle.distribution,
                        events = bundle.events,
                        timeline = PickupWorkflowRules.buildTimeline(
                            events = bundle.events,
                            currentStage = bundle.events.lastOrNull()?.stage
                        ),
                        isDonor = bundle.isDonor,
                        errorText = if (bundle.request == null || bundle.pickup == null) {
                            "Pickup record not found."
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }

    private suspend fun currentUser() = userRepository.currentUser.value
        ?: (userRepository.getCurrentUser() as? AppResult.Success)?.data

    private data class PickupDetailBundle(
        val request: DonationRequest?,
        val donation: FoodDonation?,
        val pickup: PickupRecord?,
        val distribution: DistributionRecord?,
        val events: List<WorkflowEvent>,
        val isDonor: Boolean
    )
}

data class PickupActionUiState(
    val isLoading: Boolean = true,
    val action: PickupActionType? = null,
    val request: DonationRequest? = null,
    val pickup: PickupRecord? = null,
    val isDone: Boolean = false,
    val errorText: String? = null,
    val successText: String? = null
)

@HiltViewModel
class PickupActionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val requestRepository: DonationRequestRepository,
    private val pickupRepository: PickupRepository
) : ViewModel() {

    private val requestId: String? = savedStateHandle[ARG_REQUEST_ID]
    private val action = PickupActionType.from(savedStateHandle[ARG_PICKUP_ACTION])

    private val _uiState = MutableStateFlow(PickupActionUiState(action = action))
    val uiState: StateFlow<PickupActionUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        val id = requestId
        if (id.isNullOrBlank() || action == null) {
            _uiState.update {
                it.copy(isLoading = false, errorText = "Pickup confirmation reference is missing.")
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorText = null) }
            val request = when (val result = requestRepository.getRequest(id)) {
                is AppResult.Success -> result.data
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, errorText = result.error.message) }
                    return@launch
                }
            }
            val pickup = when (val result = pickupRepository.getPickup(id)) {
                is AppResult.Success -> result.data
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, errorText = result.error.message) }
                    return@launch
                }
            }
            _uiState.update {
                it.copy(isLoading = false, request = request, pickup = pickup)
            }
        }
    }

    fun schedule(
        scheduledAt: Long,
        contactPerson: String,
        collectionInstructions: String,
        volunteerName: String,
        volunteerPhone: String,
        expectedQuantity: Int
    ) = runAction(
        success = "Pickup coordinated.",
        action = {
            pickupRepository.schedulePickup(
                requestId.orEmpty(),
                com.sharemeal.app.domain.model.PickupSchedule(
                    scheduledAt = scheduledAt,
                    contactPerson = contactPerson,
                    collectionInstructions = collectionInstructions,
                    volunteerName = volunteerName,
                    volunteerPhone = volunteerPhone,
                    expectedQuantity = expectedQuantity
                )
            )
        }
    )

    fun confirmNgoReady(volunteerName: String, volunteerPhone: String, note: String?) = runAction(
        success = "NGO readiness confirmed.",
        action = {
            pickupRepository.confirmNgoReady(
                requestId.orEmpty(),
                volunteerName,
                volunteerPhone,
                note
            )
        }
    )

    fun confirmDonorReady(contactPerson: String, collectionInstructions: String, note: String?) = runAction(
        success = "Donor readiness confirmed.",
        action = {
            pickupRepository.confirmDonorReady(
                requestId.orEmpty(),
                contactPerson,
                collectionInstructions,
                note
            )
        }
    )

    fun confirmCollection(
        actualCollectedQuantity: Int,
        discrepancyReason: String?,
        note: String?
    ) = runAction(
        success = "Physical collection confirmed.",
        action = {
            pickupRepository.confirmCollection(
                requestId.orEmpty(),
                actualCollectedQuantity,
                discrepancyReason,
                note
            )
        }
    )

    fun confirmDistribution(input: DistributionInput) = runAction(
        success = "Distribution confirmed.",
        action = { pickupRepository.confirmDistribution(requestId.orEmpty(), input) }
    )

    fun complete() = runAction(
        success = "Donation workflow completed.",
        action = { pickupRepository.complete(requestId.orEmpty()) }
    )

    fun recordException(reason: PickupExceptionReason, note: String?) = runAction(
        success = "Pickup exception recorded.",
        action = { pickupRepository.recordException(requestId.orEmpty(), reason, note) }
    )

    fun clearMessages() {
        _uiState.update { it.copy(errorText = null, successText = null) }
    }

    private fun runAction(
        success: String,
        action: suspend () -> AppResult<PickupRecord>
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorText = null, successText = null) }
            when (val result = action()) {
                is AppResult.Success -> _uiState.update {
                    it.copy(isLoading = false, pickup = result.data, isDone = true, successText = success)
                }
                is AppResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, errorText = result.error.message)
                }
            }
        }
    }
}
