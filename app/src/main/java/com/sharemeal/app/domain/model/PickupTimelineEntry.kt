package com.sharemeal.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PickupTimelineEntry(
    val stage: WorkflowStage,
    val title: String,
    val completedAt: Long? = null,
    val isCurrent: Boolean = false
)
