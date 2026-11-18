package com.sharemeal.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class WorkflowEvent(
    val id: String,
    val requestId: String,
    val donationId: String,
    val stage: WorkflowStage,
    val actorId: String,
    val actorRole: UserRole,
    val title: String,
    val note: String? = null,
    val quantity: Int? = null,
    val createdAt: Long
)
