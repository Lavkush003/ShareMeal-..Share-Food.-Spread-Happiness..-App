package com.sharemeal.app.domain.model

import com.sharemeal.app.data.local.MockDonationSeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DonationQuantityCalculatorTest {

    private val donation = MockDonationSeed.create(now = 1_000_000L).first().copy(
        id = "donation-qty",
        quantity = 100,
        remainingQuantity = 100
    )

    private fun request(
        id: String,
        requested: Int,
        approved: Int? = null,
        status: DonationRequestStatus = DonationRequestStatus.PENDING
    ) = DonationRequest(
        id = id,
        donationId = donation.id,
        ngoId = "ngo-$id",
        ngoName = "NGO $id",
        requestedQuantity = requested,
        approvedQuantity = approved,
        status = status,
        requestedAt = 1_000_000L
    )

    @Test
    fun `summary excludes rejected cancelled and expired requests from requested total`() {
        val requests = listOf(
            request("1", 10),
            request("2", 20, status = DonationRequestStatus.REJECTED),
            request("3", 30, status = DonationRequestStatus.CANCELLED),
            request("4", 40, status = DonationRequestStatus.EXPIRED)
        )

        val summary = DonationQuantityCalculator.calculate(donation, requests)

        assertEquals(10, summary.requestedQuantity)
        assertEquals(100, summary.remainingQuantity)
    }

    @Test
    fun `summary separates approved allocated collected and uncollected quantities`() {
        val requests = listOf(
            request("1", 20, approved = 15, status = DonationRequestStatus.APPROVED),
            request("2", 30, approved = 25, status = DonationRequestStatus.PICKUP_SCHEDULED),
            request("3", 10, approved = 10, status = DonationRequestStatus.COLLECTED)
        )

        val summary = DonationQuantityCalculator.calculate(donation, requests)

        assertEquals(60, summary.requestedQuantity)
        assertEquals(40, summary.approvedQuantity)
        assertEquals(50, summary.allocatedQuantity)
        assertEquals(10, summary.collectedQuantity)
        assertEquals(40, summary.uncollectedQuantity)
        assertEquals(50, summary.remainingQuantity)
    }

    @Test
    fun `summary computes remaining quantity from active allocations only`() {
        val requests = listOf(
            request("1", 20, approved = 20, status = DonationRequestStatus.COMPLETED),
            request("2", 30, approved = 15, status = DonationRequestStatus.APPROVED)
        )

        val summary = DonationQuantityCalculator.calculate(donation, requests)

        assertEquals(35, summary.allocatedQuantity)
        assertEquals(65, summary.remainingQuantity)
    }

    @Test
    fun `summary rejects allocation greater than donation quantity`() {
        val requests = listOf(
            request("1", 60, approved = 60, status = DonationRequestStatus.APPROVED),
            request("2", 60, approved = 60, status = DonationRequestStatus.APPROVED)
        )

        assertThrows(IllegalArgumentException::class.java) {
            DonationQuantityCalculator.calculate(donation, requests)
        }
    }

    @Test
    fun `allocated by ignores requests for other donations`() {
        val requests = listOf(
            request("1", 10, approved = 10, status = DonationRequestStatus.APPROVED),
            request("2", 20, approved = 20, status = DonationRequestStatus.APPROVED)
                .copy(donationId = "other-donation")
        )

        assertEquals(10, DonationQuantityCalculator.allocatedBy(requests, donation.id))
    }
}
