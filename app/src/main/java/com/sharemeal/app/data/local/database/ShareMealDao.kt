package com.sharemeal.app.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ShareMealDao {

    @Upsert
    suspend fun upsertUsers(users: List<UserEntity>)

    @Upsert
    suspend fun upsertUser(user: UserEntity)

    @Upsert
    suspend fun upsertDonorProfile(profile: DonorProfileEntity)

    @Upsert
    suspend fun upsertNgoProfile(profile: NgoProfileEntity)

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUser(userId: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY display_name ASC")
    fun observeUsers(): Flow<List<UserEntity>>

    @Transaction
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserWithProfiles(userId: String): UserWithProfiles?

    @Transaction
    @Query("SELECT * FROM users WHERE id = :userId")
    fun observeUserWithProfiles(userId: String): Flow<UserWithProfiles?>

    @Transaction
    @Query("SELECT * FROM users WHERE id = :donorId")
    suspend fun getDonorWithDonations(donorId: String): DonorWithDonations?

    @Upsert
    suspend fun upsertDonations(donations: List<FoodDonationEntity>)

    @Upsert
    suspend fun upsertDonation(donation: FoodDonationEntity)

    @Query("SELECT * FROM food_donations WHERE id = :donationId")
    suspend fun getDonation(donationId: String): FoodDonationEntity?

    @Query("SELECT * FROM food_donations WHERE is_draft = 0 ORDER BY created_at DESC")
    fun observeDonations(): Flow<List<FoodDonationEntity>>

    @Query(
        "SELECT * FROM food_donations WHERE is_draft = 0 AND pickup_deadline > :now " +
            "AND status NOT IN ('CANCELLED', 'EXPIRED') ORDER BY pickup_deadline ASC"
    )
    fun observeActiveDonations(now: Long): Flow<List<FoodDonationEntity>>

    @Query("SELECT * FROM food_donations WHERE is_draft = 1 AND donor_id = :donorId LIMIT 1")
    suspend fun getDraft(donorId: String): FoodDonationEntity?

    @Query("SELECT * FROM food_donations WHERE is_draft = 1 AND donor_id = :donorId ORDER BY updated_at DESC")
    fun observeDrafts(donorId: String): Flow<List<FoodDonationEntity>>

    @Upsert
    suspend fun upsertRequests(requests: List<DonationRequestEntity>)

    @Upsert
    suspend fun upsertRequest(request: DonationRequestEntity)

    @Query("SELECT * FROM donation_requests WHERE id = :requestId")
    suspend fun getRequest(requestId: String): DonationRequestEntity?

    @Query("SELECT * FROM donation_requests ORDER BY requested_at DESC")
    fun observeRequests(): Flow<List<DonationRequestEntity>>

    @Query("SELECT * FROM donation_requests WHERE donation_id = :donationId ORDER BY requested_at DESC")
    fun observeDonationRequests(donationId: String): Flow<List<DonationRequestEntity>>

    @Query("SELECT * FROM donation_requests WHERE ngo_id = :ngoId ORDER BY requested_at DESC")
    fun observeNgoRequests(ngoId: String): Flow<List<DonationRequestEntity>>

    @Transaction
    @Query("SELECT * FROM donation_requests WHERE id = :requestId")
    suspend fun getRequestWithPickup(requestId: String): RequestWithPickup?

    @Transaction
    @Query("SELECT * FROM food_donations WHERE id = :donationId")
    suspend fun getDonationWithRequests(donationId: String): DonationWithRequests?

    @Transaction
    @Query("SELECT * FROM users WHERE id = :ngoId")
    suspend fun getNgoWithRequests(ngoId: String): NgoWithRequests?

    @Upsert
    suspend fun upsertPickup(pickup: PickupEntity)

    @Query("SELECT * FROM pickups WHERE request_id = :requestId")
    suspend fun getPickup(requestId: String): PickupEntity?

    @Query("SELECT * FROM pickups WHERE request_id = :requestId")
    fun observePickup(requestId: String): Flow<PickupEntity?>

    @Upsert
    suspend fun upsertNotifications(notifications: List<NotificationEntity>)

    @Query("SELECT * FROM notifications WHERE recipient_id = :userId ORDER BY created_at DESC")
    fun observeNotifications(userId: String): Flow<List<NotificationEntity>>

    @Query("UPDATE notifications SET is_read = 1 WHERE id = :notificationId")
    suspend fun markNotificationRead(notificationId: String)

    @Query("UPDATE notifications SET is_read = 1 WHERE recipient_id = :userId")
    suspend fun markAllNotificationsRead(userId: String)

    @Upsert
    suspend fun upsertHistory(events: List<DonationHistoryEntity>)

    @Query("DELETE FROM donation_history WHERE donation_id = :donationId")
    suspend fun deleteHistoryForDonation(donationId: String)

    @Transaction
    suspend fun replaceHistoryForDonation(donationId: String, events: List<DonationHistoryEntity>) {
        deleteHistoryForDonation(donationId)
        upsertHistory(events)
    }

    @Query("SELECT * FROM donation_history WHERE donation_id = :donationId ORDER BY created_at DESC")
    fun observeHistory(donationId: String): Flow<List<DonationHistoryEntity>>

    @Query("SELECT * FROM donation_history ORDER BY created_at DESC")
    fun observeAllHistory(): Flow<List<DonationHistoryEntity>>

    @Transaction
    @Query("SELECT * FROM food_donations WHERE id = :donationId")
    suspend fun getDonationWithHistory(donationId: String): DonationWithHistory?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPendingOperation(operation: PendingSyncEntity): Long

    @Query("SELECT * FROM pending_sync WHERE idempotency_key = :idempotencyKey LIMIT 1")
    suspend fun getPendingOperationByKey(idempotencyKey: String): PendingSyncEntity?

    @Query("SELECT COUNT(*) FROM pending_sync WHERE idempotency_key = :idempotencyKey")
    suspend fun countPendingOperationsByKey(idempotencyKey: String): Int

    @Query("SELECT * FROM pending_sync ORDER BY created_at ASC")
    fun observePendingOperations(): Flow<List<PendingSyncEntity>>

    @Query(
        "SELECT * FROM pending_sync WHERE status IN ('PENDING', 'FAILED') " +
            "AND (next_retry_at IS NULL OR next_retry_at <= :now) ORDER BY created_at ASC LIMIT :limit"
    )
    suspend fun getOperationsForRetry(now: Long, limit: Int): List<PendingSyncEntity>

    @Query("SELECT COUNT(*) FROM pending_sync WHERE status IN ('PENDING', 'FAILED', 'IN_PROGRESS')")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_sync WHERE status = 'CONFLICT'")
    fun observeConflictCount(): Flow<Int>

    @Query(
        "UPDATE pending_sync SET status = :status, attempt_count = :attemptCount, " +
            "next_retry_at = :nextRetryAt, last_error = :lastError, " +
            "server_snapshot_json = :serverSnapshotJson WHERE operation_id = :operationId"
    )
    suspend fun updatePendingOperation(
        operationId: String,
        status: String,
        attemptCount: Int,
        nextRetryAt: Long?,
        lastError: String?,
        serverSnapshotJson: String? = null
    )

    @Query("DELETE FROM pending_sync WHERE operation_id = :operationId")
    suspend fun deletePendingOperation(operationId: String)

    @Query(
        "UPDATE food_donations SET sync_server_id = :serverId, sync_server_updated_at = :serverUpdatedAt, " +
            "sync_state = :syncState, sync_is_dirty = :isDirty, sync_last_error = :lastError WHERE id = :entityId"
    )
    suspend fun updateDonationSync(
        entityId: String,
        serverId: String?,
        serverUpdatedAt: Long?,
        syncState: String,
        isDirty: Boolean,
        lastError: String?
    )

    @Query(
        "UPDATE donation_requests SET sync_server_id = :serverId, sync_server_updated_at = :serverUpdatedAt, " +
            "sync_state = :syncState, sync_is_dirty = :isDirty, sync_last_error = :lastError WHERE id = :entityId"
    )
    suspend fun updateRequestSync(
        entityId: String,
        serverId: String?,
        serverUpdatedAt: Long?,
        syncState: String,
        isDirty: Boolean,
        lastError: String?
    )

    @Query(
        "UPDATE pickups SET sync_server_id = :serverId, sync_server_updated_at = :serverUpdatedAt, " +
            "sync_state = :syncState, sync_is_dirty = :isDirty, sync_last_error = :lastError WHERE request_id = :entityId"
    )
    suspend fun updatePickupSync(
        entityId: String,
        serverId: String?,
        serverUpdatedAt: Long?,
        syncState: String,
        isDirty: Boolean,
        lastError: String?
    )

    @Query(
        "UPDATE users SET sync_server_id = :serverId, sync_server_updated_at = :serverUpdatedAt, " +
            "sync_state = :syncState, sync_is_dirty = :isDirty, sync_last_error = :lastError WHERE id = :entityId"
    )
    suspend fun updateUserSync(
        entityId: String,
        serverId: String?,
        serverUpdatedAt: Long?,
        syncState: String,
        isDirty: Boolean,
        lastError: String?
    )
}
