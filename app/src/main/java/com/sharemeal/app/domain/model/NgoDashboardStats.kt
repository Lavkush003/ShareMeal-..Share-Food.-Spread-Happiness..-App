package com.sharemeal.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class NgoDashboardStats(
    val ngoName: String,
    val verificationStatus: NgoVerificationStatus,
    val availableNearbyDonationsCount: Int,
    val pendingRequestsCount: Int,
    val approvedCollectionsCount: Int,
    val upcomingPickupsCount: Int,
    val completedDistributionsCount: Int,
    val totalMealsCollected: Int,
    val recentActivities: List<NgoActivity>
)
