package com.sharemeal.app.data.local.database

import com.sharemeal.app.core.media.ImageReference
import com.sharemeal.app.domain.model.Address
import com.sharemeal.app.domain.model.AppNotification
import com.sharemeal.app.domain.model.AppNotificationType
import com.sharemeal.app.domain.model.DonorCategory
import com.sharemeal.app.domain.model.DonorProfile
import com.sharemeal.app.domain.model.DonationDraft
import com.sharemeal.app.domain.model.DonationQuantityUnit
import com.sharemeal.app.domain.model.DonationRequest
import com.sharemeal.app.domain.model.DonationRequestStatus
import com.sharemeal.app.domain.model.DonationStatus
import com.sharemeal.app.domain.model.FoodCategory
import com.sharemeal.app.domain.model.FoodClassification
import com.sharemeal.app.domain.model.FoodDonation
import com.sharemeal.app.domain.model.NgoProfile
import com.sharemeal.app.domain.model.NgoVerificationStatus
import com.sharemeal.app.domain.model.PendingOperation
import com.sharemeal.app.domain.model.PickupExceptionReason
import com.sharemeal.app.domain.model.PickupRecord
import com.sharemeal.app.domain.model.PickupSchedule
import com.sharemeal.app.domain.model.PickupWorkflowStatus
import com.sharemeal.app.domain.model.SyncOperationStatus
import com.sharemeal.app.domain.model.SyncOperationType
import com.sharemeal.app.domain.model.SyncState
import com.sharemeal.app.domain.model.User
import com.sharemeal.app.domain.model.UserRole
import com.sharemeal.app.domain.model.WorkflowEvent
import com.sharemeal.app.domain.model.WorkflowStage
import java.util.UUID

object RoomEntityMapper {

    fun toDomain(entity: UserWithProfiles): User = User(
        id = entity.user.id,
        email = entity.user.email,
        mobile = entity.user.mobile,
        role = enumValue(entity.user.role, UserRole.DONOR),
        displayName = entity.user.displayName,
        donorProfile = entity.donorProfile?.let(::toDomain),
        ngoProfile = entity.ngoProfile?.let(::toDomain),
        createdAt = entity.user.createdAt
    )

    fun toEntity(user: User): UserEntity = UserEntity(
        id = user.id,
        email = user.email,
        mobile = user.mobile,
        role = user.role.name,
        displayName = user.displayName,
        createdAt = user.createdAt,
        sync = synced(user.id)
    )

    fun toEntity(userId: String, profile: DonorProfile): DonorProfileEntity = DonorProfileEntity(
        userId = userId,
        fullName = profile.fullName,
        organizationName = profile.organizationName,
        category = profile.category.name,
        addressLine1 = profile.address.line1,
        city = profile.address.city,
        state = profile.address.state,
        pinCode = profile.address.pinCode,
        sync = synced(userId)
    )

    fun toDomain(entity: DonorProfileEntity): DonorProfile = DonorProfile(
        fullName = entity.fullName,
        organizationName = entity.organizationName,
        category = enumValue(entity.category, DonorCategory.OTHER),
        address = Address(
            line1 = entity.addressLine1,
            city = entity.city,
            state = entity.state,
            pinCode = entity.pinCode
        )
    )

    fun toEntity(userId: String, profile: NgoProfile): NgoProfileEntity = NgoProfileEntity(
        userId = userId,
        ngoName = profile.ngoName,
        registrationNumber = profile.registrationNumber,
        contactPersonName = profile.contactPersonName,
        addressLine1 = profile.address.line1,
        city = profile.address.city,
        state = profile.address.state,
        pinCode = profile.address.pinCode,
        operatingArea = profile.operatingArea,
        estimatedDistributionCapacity = profile.estimatedDistributionCapacity,
        verificationDocumentInfo = profile.verificationDocumentInfo,
        verificationStatus = profile.verificationStatus.name,
        sync = synced(userId)
    )

    fun toDomain(entity: NgoProfileEntity): NgoProfile = NgoProfile(
        ngoName = entity.ngoName,
        registrationNumber = entity.registrationNumber,
        contactPersonName = entity.contactPersonName,
        address = Address(
            line1 = entity.addressLine1,
            city = entity.city,
            state = entity.state,
            pinCode = entity.pinCode
        ),
        operatingArea = entity.operatingArea,
        estimatedDistributionCapacity = entity.estimatedDistributionCapacity,
        verificationDocumentInfo = entity.verificationDocumentInfo,
        verificationStatus = enumValue(entity.verificationStatus, NgoVerificationStatus.PENDING)
    )

    fun toDomain(entity: FoodDonationEntity): FoodDonation = FoodDonation(
        id = entity.id,
        donorId = entity.donorId,
        title = entity.title,
        description = entity.description,
        category = enumValue(entity.category, FoodCategory.OTHER),
        images = entity.images,
        quantity = entity.quantity,
        quantityUnit = enumValue(entity.quantityUnit, DonationQuantityUnit.OTHER),
        customQuantityUnit = entity.customQuantityUnit,
        classification = enumValue(entity.classification, FoodClassification.OTHER),
        preparedAt = entity.preparedAt,
        pickupWindowStart = entity.pickupWindowStart,
        pickupDeadline = entity.pickupDeadline,
        storageConditions = entity.storageConditions,
        packaging = entity.packaging,
        allergenInformation = entity.allergenInformation,
        foodConditionDeclared = entity.foodConditionDeclared,
        pickupAddress = entity.pickupAddress,
        city = entity.city,
        state = entity.state,
        pinCode = entity.pinCode,
        contactPerson = entity.contactPerson,
        contactPhone = entity.contactPhone,
        specialInstructions = entity.specialInstructions,
        status = enumValue(entity.status, DonationStatus.PUBLISHED),
        remainingQuantity = entity.remainingQuantity,
        createdAt = entity.createdAt
    )

    fun toEntity(donation: FoodDonation, isDraft: Boolean = false): FoodDonationEntity = FoodDonationEntity(
        id = donation.id,
        donorId = donation.donorId,
        title = donation.title,
        description = donation.description,
        category = donation.category.name,
        images = donation.images,
        quantity = donation.quantity,
        quantityUnit = donation.quantityUnit.name,
        customQuantityUnit = donation.customQuantityUnit,
        classification = donation.classification.name,
        preparedAt = donation.preparedAt,
        pickupWindowStart = donation.pickupWindowStart,
        pickupDeadline = donation.pickupDeadline,
        storageConditions = donation.storageConditions,
        packaging = donation.packaging,
        allergenInformation = donation.allergenInformation,
        foodConditionDeclared = donation.foodConditionDeclared,
        pickupAddress = donation.pickupAddress,
        city = donation.city,
        state = donation.state,
        pinCode = donation.pinCode,
        contactPerson = donation.contactPerson,
        contactPhone = donation.contactPhone,
        specialInstructions = donation.specialInstructions,
        status = donation.status.name,
        remainingQuantity = donation.remainingQuantity,
        createdAt = donation.createdAt,
        isDraft = isDraft,
        sync = if (isDraft) pending(donation.id) else synced(donation.id)
    )

    fun toDomain(draft: DonationDraft, donorId: String, id: String): FoodDonation = FoodDonation(
        id = id,
        donorId = donorId,
        title = draft.title,
        description = draft.description,
        category = draft.category,
        images = draft.images,
        quantity = draft.quantity,
        quantityUnit = draft.quantityUnit,
        customQuantityUnit = draft.customQuantityUnit,
        classification = draft.classification,
        preparedAt = draft.preparedAt,
        pickupWindowStart = draft.pickupWindowStart,
        pickupDeadline = draft.pickupDeadline,
        storageConditions = draft.storageConditions,
        packaging = draft.packaging,
        allergenInformation = draft.allergenInformation,
        foodConditionDeclared = draft.foodConditionDeclared,
        pickupAddress = draft.pickupAddress,
        city = draft.city,
        state = draft.state,
        pinCode = draft.pinCode,
        contactPerson = draft.contactPerson,
        contactPhone = draft.contactPhone,
        specialInstructions = draft.specialInstructions,
        createdAt = System.currentTimeMillis()
    )

    fun toDraft(entity: FoodDonationEntity): DonationDraft = DonationDraft(
        title = entity.title,
        description = entity.description,
        category = enumValue(entity.category, FoodCategory.OTHER),
        images = entity.images,
        quantity = entity.quantity,
        quantityUnit = enumValue(entity.quantityUnit, DonationQuantityUnit.OTHER),
        customQuantityUnit = entity.customQuantityUnit,
        classification = enumValue(entity.classification, FoodClassification.OTHER),
        preparedAt = entity.preparedAt,
        pickupWindowStart = entity.pickupWindowStart,
        pickupDeadline = entity.pickupDeadline,
        storageConditions = entity.storageConditions,
        packaging = entity.packaging,
        allergenInformation = entity.allergenInformation,
        foodConditionDeclared = entity.foodConditionDeclared,
        pickupAddress = entity.pickupAddress,
        city = entity.city,
        state = entity.state,
        pinCode = entity.pinCode,
        contactPerson = entity.contactPerson,
        contactPhone = entity.contactPhone,
        specialInstructions = entity.specialInstructions
    )

    fun toDomain(entity: DonationRequestEntity): DonationRequest = DonationRequest(
        id = entity.id,
        donationId = entity.donationId,
        ngoId = entity.ngoId,
        ngoName = entity.ngoName,
        requestedQuantity = entity.requestedQuantity,
        status = enumValue(entity.status, DonationRequestStatus.PENDING),
        requestedAt = entity.requestedAt,
        pickupScheduledTime = entity.pickupScheduledTime,
        completedAt = entity.completedAt,
        notes = entity.notes,
        approvedQuantity = entity.approvedQuantity,
        peopleToServe = entity.peopleToServe,
        expectedPickupAt = entity.expectedPickupAt,
        donorId = entity.donorId,
        donationTitle = entity.donationTitle,
        rejectionReason = entity.rejectionReason,
        idempotencyKey = entity.idempotencyKey,
        updatedAt = entity.updatedAt
    )

    fun toEntity(request: DonationRequest): DonationRequestEntity = DonationRequestEntity(
        id = request.id,
        donationId = request.donationId,
        ngoId = request.ngoId,
        ngoName = request.ngoName,
        requestedQuantity = request.requestedQuantity,
        status = request.status.name,
        requestedAt = request.requestedAt,
        pickupScheduledTime = request.pickupScheduledTime,
        completedAt = request.completedAt,
        notes = request.notes,
        approvedQuantity = request.approvedQuantity,
        peopleToServe = request.peopleToServe,
        expectedPickupAt = request.expectedPickupAt,
        donorId = request.donorId,
        donationTitle = request.donationTitle,
        rejectionReason = request.rejectionReason,
        idempotencyKey = request.idempotencyKey,
        updatedAt = request.updatedAt,
        sync = synced(request.id)
    )

    fun toEntity(record: PickupRecord): PickupEntity = PickupEntity(
        requestId = record.requestId,
        donationId = record.donationId,
        scheduledAt = record.schedule.scheduledAt,
        contactPerson = record.schedule.contactPerson,
        collectionInstructions = record.schedule.collectionInstructions,
        volunteerName = record.schedule.volunteerName,
        volunteerPhone = record.schedule.volunteerPhone,
        expectedQuantity = record.schedule.expectedQuantity,
        status = record.status.name,
        ngoReadyAt = record.ngoReadyAt,
        donorReadyAt = record.donorReadyAt,
        collectedAt = record.collectedAt,
        actualCollectedQuantity = record.actualCollectedQuantity,
        quantityDiscrepancy = record.quantityDiscrepancy,
        discrepancyReason = record.discrepancyReason,
        exceptionReason = record.exceptionReason?.name,
        exceptionNote = record.exceptionNote,
        distributionConfirmedAt = record.distributionConfirmedAt,
        actualReceivedQuantity = null,
        actualDistributedQuantity = null,
        beneficiariesServed = null,
        distributedAt = null,
        distributionLocation = null,
        remainingDistributionQuantity = null,
        distributionNotes = null,
        evidenceRefs = emptyList(),
        completedAt = record.completedAt,
        createdAt = record.createdAt,
        updatedAt = record.updatedAt,
        sync = synced(record.requestId)
    )

    fun toDomain(entity: PickupEntity): PickupRecord = PickupRecord(
        requestId = entity.requestId,
        donationId = entity.donationId,
        schedule = PickupSchedule(
            scheduledAt = entity.scheduledAt,
            contactPerson = entity.contactPerson,
            collectionInstructions = entity.collectionInstructions,
            volunteerName = entity.volunteerName,
            volunteerPhone = entity.volunteerPhone,
            expectedQuantity = entity.expectedQuantity
        ),
        status = enumValue(entity.status, PickupWorkflowStatus.SCHEDULED),
        ngoReadyAt = entity.ngoReadyAt,
        donorReadyAt = entity.donorReadyAt,
        collectedAt = entity.collectedAt,
        actualCollectedQuantity = entity.actualCollectedQuantity,
        quantityDiscrepancy = entity.quantityDiscrepancy,
        discrepancyReason = entity.discrepancyReason,
        exceptionReason = entity.exceptionReason?.let {
            enumValue(it, PickupExceptionReason.CANCELLED_PICKUP)
        },
        exceptionNote = entity.exceptionNote,
        distributionConfirmedAt = entity.distributionConfirmedAt,
        completedAt = entity.completedAt,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt
    )

    fun mergeDistribution(entity: PickupEntity, distribution: com.sharemeal.app.domain.model.DistributionRecord):
        PickupEntity = entity.copy(
        actualReceivedQuantity = distribution.actualReceivedQuantity,
        actualDistributedQuantity = distribution.actualDistributedQuantity,
        beneficiariesServed = distribution.beneficiariesServed,
        distributedAt = distribution.distributedAt,
        distributionLocation = distribution.location,
        remainingDistributionQuantity = distribution.remainingQuantity,
        distributionNotes = distribution.notes,
        evidenceRefs = distribution.evidenceRefs,
        distributionConfirmedAt = distribution.distributedAt,
        updatedAt = System.currentTimeMillis()
    )

    fun toDistributionRecord(entity: PickupEntity): com.sharemeal.app.domain.model.DistributionRecord? {
        val distributedAt = entity.distributedAt ?: return null
        return com.sharemeal.app.domain.model.DistributionRecord(
            id = "dist_${entity.requestId}",
            requestId = entity.requestId,
            donationId = entity.donationId,
            actualReceivedQuantity = entity.actualReceivedQuantity ?: entity.actualCollectedQuantity ?: 0,
            actualDistributedQuantity = entity.actualDistributedQuantity ?: 0,
            beneficiariesServed = entity.beneficiariesServed ?: 0,
            distributedAt = distributedAt,
            location = entity.distributionLocation.orEmpty(),
            remainingQuantity = entity.remainingDistributionQuantity ?: 0,
            notes = entity.distributionNotes,
            evidenceRefs = entity.evidenceRefs,
            createdAt = entity.createdAt
        )
    }

    fun toDomain(entity: NotificationEntity): AppNotification = AppNotification(
        id = entity.id,
        recipientId = entity.recipientId,
        type = enumValue(entity.type, AppNotificationType.NEW_REQUEST),
        title = entity.title,
        body = entity.body,
        requestId = entity.requestId,
        donationId = entity.donationId,
        createdAt = entity.createdAt,
        isRead = entity.isRead
    )

    fun toEntity(notification: AppNotification): NotificationEntity = NotificationEntity(
        id = notification.id,
        recipientId = notification.recipientId,
        type = notification.type.name,
        title = notification.title,
        body = notification.body,
        requestId = notification.requestId,
        donationId = notification.donationId,
        createdAt = notification.createdAt,
        isRead = notification.isRead,
        sync = synced(notification.id)
    )

    fun toDomain(entity: DonationHistoryEntity): WorkflowEvent = WorkflowEvent(
        id = entity.id,
        requestId = entity.requestId,
        donationId = entity.donationId,
        stage = enumValue(entity.stage, WorkflowStage.DONATION_PUBLISHED),
        actorId = entity.actorId,
        actorRole = enumValue(entity.actorRole, UserRole.DONOR),
        title = entity.title,
        note = entity.note,
        quantity = entity.quantity,
        createdAt = entity.createdAt
    )

    fun toEntity(event: WorkflowEvent): DonationHistoryEntity = DonationHistoryEntity(
        id = event.id,
        donationId = event.donationId,
        requestId = event.requestId,
        stage = event.stage.name,
        actorId = event.actorId,
        actorRole = event.actorRole.name,
        title = event.title,
        note = event.note,
        quantity = event.quantity,
        createdAt = event.createdAt,
        sync = synced(event.id)
    )

    fun toDomain(entity: PendingSyncEntity): PendingOperation = PendingOperation(
        operationId = entity.operationId,
        idempotencyKey = entity.idempotencyKey,
        type = enumValue(entity.type, SyncOperationType.CREATE_DONATION),
        entityType = entity.entityType,
        entityId = entity.entityId,
        path = entity.path,
        method = entity.method,
        payloadJson = entity.payloadJson,
        createdAt = entity.createdAt,
        attemptCount = entity.attemptCount,
        nextRetryAt = entity.nextRetryAt,
        lastError = entity.lastError,
        serverSnapshotJson = entity.serverSnapshotJson,
        status = enumValue(entity.status, SyncOperationStatus.PENDING)
    )

    fun toEntity(operation: PendingOperation): PendingSyncEntity = PendingSyncEntity(
        operationId = operation.operationId,
        idempotencyKey = operation.idempotencyKey,
        type = operation.type.name,
        entityType = operation.entityType,
        entityId = operation.entityId,
        path = operation.path,
        method = operation.method,
        payloadJson = operation.payloadJson,
        createdAt = operation.createdAt,
        attemptCount = operation.attemptCount,
        nextRetryAt = operation.nextRetryAt,
        lastError = operation.lastError,
        serverSnapshotJson = operation.serverSnapshotJson,
        status = operation.status.name
    )

    private fun synced(serverId: String): SyncMetadata = SyncMetadata(
        serverId = serverId,
        serverUpdatedAt = System.currentTimeMillis(),
        syncState = SyncState.SYNCED.name,
        isDirty = false
    )

    private fun pending(entityId: String): SyncMetadata = SyncMetadata(
        serverId = null,
        serverUpdatedAt = null,
        syncState = SyncState.PENDING.name,
        isDirty = true
    )

    private inline fun <reified T : Enum<T>> enumValue(value: String, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: fallback

    fun newDraftId(): String = "draft_${UUID.randomUUID()}"
}
