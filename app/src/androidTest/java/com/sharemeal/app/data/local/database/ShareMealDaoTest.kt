package com.sharemeal.app.data.local.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sharemeal.app.domain.model.DonationRequest
import com.sharemeal.app.domain.model.DonationStatus
import com.sharemeal.app.domain.model.DonationQuantityUnit
import com.sharemeal.app.domain.model.FoodCategory
import com.sharemeal.app.domain.model.FoodClassification
import com.sharemeal.app.domain.model.FoodDonation
import com.sharemeal.app.domain.model.SyncState
import com.sharemeal.app.domain.model.UserRole
import com.sharemeal.app.domain.model.WorkflowEvent
import com.sharemeal.app.domain.model.WorkflowStage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShareMealDaoTest {

    private lateinit var database: ShareMealDatabase
    private lateinit var dao: ShareMealDao

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, ShareMealDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.shareMealDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun relationsLoadDonorNgoRequestsPickupAndHistory() = runBlocking {
        dao.upsertUsers(listOf(donor(), ngo()))
        dao.upsertDonation(donation())
        dao.upsertRequest(request())
        dao.upsertPickup(pickup())
        dao.upsertHistory(listOf(historyEvent()))

        val donorWithDonations = dao.getDonorWithDonations("donor")!!
        assertEquals(1, donorWithDonations.donations.size)

        val donationWithRequests = dao.getDonationWithRequests("donation")!!
        assertEquals(1, donationWithRequests.requests.size)

        val requestWithPickup = dao.getRequestWithPickup("request")!!
        assertNotNull(requestWithPickup.pickup)

        val ngoWithRequests = dao.getNgoWithRequests("ngo")!!
        assertEquals(1, ngoWithRequests.requests.size)

        val donationWithHistory = dao.getDonationWithHistory("donation")!!
        assertEquals(1, donationWithHistory.history.size)
    }

    @Test
    fun draftsAreExcludedFromActiveDonations() = runBlocking {
        dao.upsertUsers(listOf(donor()))
        dao.upsertDonation(donation())
        val draft = donation().copy(
            id = "draft",
            isDraft = true,
            sync = SyncMetadata(syncState = SyncState.PENDING.name, isDirty = true)
        )
        dao.upsertDonation(draft)

        assertEquals(draft, dao.getDraft("donor"))
        assertEquals(1, dao.observeDrafts("donor").first().size)
        assertEquals(1, dao.observeDonations().first().size)
    }

    @Test
    fun outboxUsesIdempotencyKeyAndSupportsRetrySelection() = runBlocking {
        val operation = PendingSyncEntity(
            operationId = "op-1",
            idempotencyKey = "stable-key",
            type = "CREATE_DONATION",
            entityType = "donation",
            entityId = "donation",
            path = "/donations",
            method = "POST",
            payloadJson = "{}",
            createdAt = 1_700_000_000_000L
        )

        assertEquals(1L, dao.insertPendingOperation(operation))
        assertEquals(-1L, dao.insertPendingOperation(operation.copy(operationId = "op-2")))
        assertEquals(1, dao.countPendingOperationsByKey("stable-key"))
        assertEquals(1, dao.observePendingCount().first())
        assertEquals(1, dao.getOperationsForRetry(Long.MAX_VALUE, 20).size)
    }

    private fun donor(): UserEntity = UserEntity(
        id = "donor",
        email = "donor@test.local",
        mobile = "0000000000",
        role = UserRole.DONOR.name,
        displayName = "Donor",
        createdAt = 1L
    )

    private fun ngo(): UserEntity = UserEntity(
        id = "ngo",
        email = "ngo@test.local",
        mobile = "1111111111",
        role = UserRole.NGO.name,
        displayName = "NGO",
        createdAt = 1L
    )

    private fun donation(): FoodDonationEntity = RoomEntityMapper.toEntity(
        FoodDonation(
            id = "donation",
            donorId = "donor",
            title = "Rice",
            description = "Cooked rice",
            category = FoodCategory.COOKED_MEAL,
            images = emptyList(),
            quantity = 10,
            quantityUnit = DonationQuantityUnit.PLATES,
            customQuantityUnit = null,
            classification = FoodClassification.VEGETARIAN,
            preparedAt = 1_700_000_000_000L,
            pickupWindowStart = 1_700_003_600_000L,
            pickupDeadline = 1_700_007_200_000L,
            storageConditions = "Warm",
            packaging = "Sealed",
            allergenInformation = "None",
            foodConditionDeclared = true,
            pickupAddress = "1 Test Street",
            city = "Bengaluru",
            state = "Karnataka",
            pinCode = "560001",
            contactPerson = "Donor",
            contactPhone = "0000000000",
            specialInstructions = null,
            status = DonationStatus.PUBLISHED,
            createdAt = 1_700_000_000_000L
        )
    )

    private fun request(): DonationRequestEntity = RoomEntityMapper.toEntity(
        DonationRequest(
            id = "request",
            donationId = "donation",
            ngoId = "ngo",
            ngoName = "NGO",
            requestedQuantity = 5,
            requestedAt = 1_700_000_100_000L,
            idempotencyKey = "request-key"
        )
    )

    private fun pickup(): PickupEntity = PickupEntity(
        requestId = "request",
        donationId = "donation",
        scheduledAt = 1_700_003_600_000L,
        contactPerson = "Donor",
        collectionInstructions = "Call on arrival",
        volunteerName = "Volunteer",
        volunteerPhone = "1111111111",
        expectedQuantity = 5,
        status = "SCHEDULED",
        ngoReadyAt = null,
        donorReadyAt = null,
        collectedAt = null,
        actualCollectedQuantity = null,
        quantityDiscrepancy = null,
        discrepancyReason = null,
        exceptionReason = null,
        exceptionNote = null,
        distributionConfirmedAt = null,
        actualReceivedQuantity = null,
        actualDistributedQuantity = null,
        beneficiariesServed = null,
        distributedAt = null,
        distributionLocation = null,
        remainingDistributionQuantity = null,
        distributionNotes = null,
        evidenceRefs = emptyList(),
        completedAt = null,
        createdAt = 1_700_000_100_000L,
        updatedAt = 1_700_000_100_000L
    )

    private fun historyEvent(): DonationHistoryEntity = RoomEntityMapper.toEntity(
        WorkflowEvent(
            id = "event-1",
            requestId = "request",
            donationId = "donation",
            stage = WorkflowStage.DONATION_PUBLISHED,
            actorId = "donor",
            actorRole = UserRole.DONOR,
            title = "Donation published",
            createdAt = 1_700_000_000_000L
        )
    )
}
