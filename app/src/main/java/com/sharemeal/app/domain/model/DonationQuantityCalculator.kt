package com.sharemeal.app.domain.model

/**
 * Derived quantity view for a single donation listing.
 *
 * All values are computed from repository data. Nothing here is persisted, so the
 * figures can never drift from the requests that produced them.
 */
data class DonationQuantitySummary(
    val donationId: String,
    val totalQuantity: Int,
    val requestedQuantity: Int,
    val approvedQuantity: Int,
    val allocatedQuantity: Int,
    val remainingQuantity: Int,
    val collectedQuantity: Int,
    val uncollectedQuantity: Int
)

object DonationQuantityCalculator {

    fun calculate(donation: FoodDonation, requests: List<DonationRequest>): DonationQuantitySummary {
        require(donation.quantity >= 0) { "Total quantity cannot be negative." }

        val own = requests.filter { it.donationId == donation.id }
        own.forEach { request ->
            require(request.requestedQuantity >= 0) { "Requested quantity cannot be negative." }
            request.approvedQuantity?.let { approved ->
                require(approved >= 0) { "Approved quantity cannot be negative." }
                require(approved <= request.requestedQuantity) {
                    "Approved quantity cannot exceed requested quantity."
                }
                require(request.status.allocatesQuantity || request.status.isTerminal) {
                    "Approved quantity requires an allocation status."
                }
            }
        }

        val requested = own
            .filterNot { it.status == DonationRequestStatus.REJECTED ||
                it.status == DonationRequestStatus.CANCELLED ||
                it.status == DonationRequestStatus.EXPIRED }
            .sumOf { it.requestedQuantity }

        val allocated = own
            .filter { it.status.allocatesQuantity }
            .sumOf { it.approvedQuantity ?: 0 }

        val approved = own
            .filter { it.status == DonationRequestStatus.APPROVED ||
                it.status == DonationRequestStatus.PICKUP_SCHEDULED }
            .sumOf { it.approvedQuantity ?: 0 }

        val collected = own
            .filter { it.status == DonationRequestStatus.COLLECTED ||
                it.status == DonationRequestStatus.DISTRIBUTED ||
                it.status == DonationRequestStatus.COMPLETED }
            .sumOf { it.approvedQuantity ?: 0 }

        require(allocated <= donation.quantity) { "Allocated quantity exceeds total quantity." }
        require(allocated >= collected) { "Collected quantity exceeds allocated quantity." }
        require(approved <= allocated) { "Approved quantity exceeds allocated quantity." }

        return DonationQuantitySummary(
            donationId = donation.id,
            totalQuantity = donation.quantity,
            requestedQuantity = requested,
            approvedQuantity = approved,
            allocatedQuantity = allocated,
            remainingQuantity = donation.quantity - allocated,
            collectedQuantity = collected,
            uncollectedQuantity = allocated - collected
        )
    }

    fun allocatedBy(requests: List<DonationRequest>, donationId: String): Int =
        requests
            .filter { it.donationId == donationId && it.status.allocatesQuantity }
            .sumOf { it.approvedQuantity ?: 0 }
}
