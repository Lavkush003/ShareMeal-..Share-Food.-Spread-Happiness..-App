package com.sharemeal.app.data.offline

import com.sharemeal.app.core.common.AppError
import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.core.session.SessionManager
import com.sharemeal.app.data.local.database.RoomEntityMapper
import com.sharemeal.app.data.local.database.ShareMealDao
import com.sharemeal.app.data.local.database.SyncMetadata
import com.sharemeal.app.data.local.database.UserEntity
import com.sharemeal.app.domain.model.DonationDraft
import com.sharemeal.app.domain.model.SyncState
import com.sharemeal.app.domain.model.UserRole
import com.sharemeal.app.domain.repository.DonationDraftRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class RoomDonationDraftRepository @Inject constructor(
    private val dao: ShareMealDao,
    private val sessionManager: SessionManager
) : DonationDraftRepository {

    override suspend fun saveDraft(draft: DonationDraft): AppResult<String> = try {
        val session = sessionManager.session.value
            ?: return AppResult.Failure(AppError.Unauthorized())
        val id = RoomEntityMapper.newDraftId()
        val donorId = session.userId

        ensureUser(donorId, session.role)
        val donation = RoomEntityMapper.toDomain(draft, donorId, id)
        val entity = RoomEntityMapper.toEntity(donation, isDraft = true).copy(
            updatedAt = System.currentTimeMillis(),
            sync = SyncMetadata(syncState = SyncState.PENDING.name, isDirty = true)
        )
        dao.upsertDonation(entity)
        AppResult.Success(id)
    } catch (error: Exception) {
        AppResult.Failure(AppError.Unknown("Could not save draft.", cause = error))
    }

    override suspend fun loadLatestDraft(): AppResult<DonationDraft?> = try {
        val donorId = sessionManager.session.value?.userId
            ?: return AppResult.Failure(AppError.Unauthorized())
        val draft = dao.observeDrafts(donorId).first()
            .maxByOrNull { it.updatedAt }
            ?.let(RoomEntityMapper::toDraft)
        AppResult.Success(draft)
    } catch (error: Exception) {
        AppResult.Failure(AppError.Unknown("Could not load saved draft.", cause = error))
    }

    private suspend fun ensureUser(userId: String, role: UserRole) {
        if (dao.getUser(userId) != null) return
        dao.upsertUser(
            UserEntity(
                id = userId,
                email = "$userId@cached.local",
                mobile = "",
                role = role.name,
                displayName = userId,
                createdAt = System.currentTimeMillis(),
                sync = SyncMetadata(syncState = SyncState.SYNCED.name)
            )
        )
    }
}
