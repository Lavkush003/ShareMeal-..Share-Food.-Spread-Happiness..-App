package com.sharemeal.app.domain.model

object PickupWorkflowRules {

    val primaryStages: List<WorkflowStage> = listOf(
        WorkflowStage.DONATION_PUBLISHED,
        WorkflowStage.NGO_REQUESTED,
        WorkflowStage.DONOR_APPROVED,
        WorkflowStage.PICKUP_SCHEDULED,
        WorkflowStage.NGO_READY,
        WorkflowStage.DONOR_READY,
        WorkflowStage.FOOD_COLLECTED,
        WorkflowStage.DISTRIBUTION_CONFIRMED,
        WorkflowStage.COMPLETED
    )

    fun canTransition(from: PickupWorkflowStatus, to: PickupWorkflowStatus): Boolean = when (from) {
        PickupWorkflowStatus.SCHEDULED ->
            to == PickupWorkflowStatus.NGO_READY ||
                to == PickupWorkflowStatus.CANCELLED ||
                to == PickupWorkflowStatus.MISSED ||
                to == PickupWorkflowStatus.DONOR_UNAVAILABLE ||
                to == PickupWorkflowStatus.NGO_UNABLE_TO_COLLECT ||
                to == PickupWorkflowStatus.SAFETY_REJECTED
        PickupWorkflowStatus.NGO_READY ->
            to == PickupWorkflowStatus.DONOR_READY ||
                to == PickupWorkflowStatus.MISSED ||
                to == PickupWorkflowStatus.DONOR_UNAVAILABLE ||
                to == PickupWorkflowStatus.NGO_UNABLE_TO_COLLECT ||
                to == PickupWorkflowStatus.SAFETY_REJECTED
        PickupWorkflowStatus.DONOR_READY ->
            to == PickupWorkflowStatus.COLLECTED ||
                to == PickupWorkflowStatus.MISSED ||
                to == PickupWorkflowStatus.DONOR_UNAVAILABLE ||
                to == PickupWorkflowStatus.NGO_UNABLE_TO_COLLECT ||
                to == PickupWorkflowStatus.SAFETY_REJECTED
        PickupWorkflowStatus.COLLECTED ->
            to == PickupWorkflowStatus.DISTRIBUTED ||
                to == PickupWorkflowStatus.SAFETY_REJECTED
        PickupWorkflowStatus.DISTRIBUTED -> to == PickupWorkflowStatus.COMPLETED
        else -> false
    }

    fun buildTimeline(
        events: List<WorkflowEvent>,
        currentStage: WorkflowStage?
    ): List<PickupTimelineEntry> {
        val timestamps = events.associateBy({ it.stage }, { it.createdAt })
        val exception = events.lastOrNull { it.stage.isException }
        val current = currentStage ?: exception?.stage
        return primaryStages.map { stage ->
            PickupTimelineEntry(
                stage = stage,
                title = stage.name,
                completedAt = timestamps[stage],
                isCurrent = stage == current
            )
        }
    }
}
