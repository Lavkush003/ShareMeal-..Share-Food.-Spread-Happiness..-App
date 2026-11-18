package com.sharemeal.app.domain.repository

import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.domain.model.DonationDraft

interface DonationDraftRepository {
    suspend fun saveDraft(draft: DonationDraft): AppResult<String>
    suspend fun loadLatestDraft(): AppResult<DonationDraft?>
}
