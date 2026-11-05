package com.sharemeal.app.data.auth

import com.sharemeal.app.domain.model.Address
import com.sharemeal.app.domain.model.DonorCategory
import com.sharemeal.app.domain.model.DonorProfile
import com.sharemeal.app.domain.model.NgoProfile
import com.sharemeal.app.domain.model.NgoVerificationStatus
import com.sharemeal.app.domain.model.User
import com.sharemeal.app.domain.model.UserRole
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.withLock

@Singleton
class MockAccountSeeder @Inject constructor(
    private val accountStore: MockAccountStore,
    private val accountLock: MockAccountLock,
    private val passwordHasher: MockPasswordHasher
) {
    @Volatile
    private var seeded = false

    suspend fun ensureSeeded() {
        if (seeded) return

        accountLock.mutex.withLock {
            if (!seeded) {
                if (accountStore.load().isEmpty()) {
                    accountStore.save(createDemoAccounts())
                }
                seeded = true
            }
        }
    }

    private fun createDemoAccounts(): List<MockAccountRecord> {
        val donorCredential = newCredential()
        val ngoCredential = newCredential()
        return listOf(
            MockAccountRecord(
                user = demoDonor(),
                passwordSalt = donorCredential.salt,
                passwordHash = donorCredential.hash
            ),
            MockAccountRecord(
                user = demoNgo(),
                passwordSalt = ngoCredential.salt,
                passwordHash = ngoCredential.hash
            )
        )
    }

    private fun newCredential(): Credential {
        val salt = passwordHasher.newSalt()
        return Credential(
            salt = salt,
            hash = passwordHasher.hash(DemoPassword, salt)
        )
    }

    private fun demoDonor(): User = User(
        id = "mock-donor-demo",
        email = "donor@sharemeal.dev",
        mobile = "9876543210",
        role = UserRole.DONOR,
        displayName = "Aarav Sharma",
        donorProfile = DonorProfile(
            fullName = "Aarav Sharma",
            organizationName = "Green Leaf Kitchen",
            category = DonorCategory.RESTAURANT,
            address = Address(
                line1 = "12, 4th Block, Jayanagar",
                city = "Bengaluru",
                state = "Karnataka",
                pinCode = "560011"
            )
        ),
        createdAt = DemoCreatedAt
    )

    private fun demoNgo(): User = User(
        id = "mock-ngo-demo",
        email = "ngo@sharemeal.dev",
        mobile = "9876543211",
        role = UserRole.NGO,
        displayName = "Hope Foundation",
        ngoProfile = NgoProfile(
            ngoName = "Hope Foundation",
            registrationNumber = "MOCK-NGO-2026",
            contactPersonName = "Meera Rao",
            address = Address(
                line1 = "44, Community Centre Road",
                city = "Bengaluru",
                state = "Karnataka",
                pinCode = "560034"
            ),
            operatingArea = "Central Bengaluru",
            estimatedDistributionCapacity = "200 meals/day",
            verificationDocumentInfo = "Mock FSSAI registration and trust certificate",
            verificationStatus = NgoVerificationStatus.PENDING
        ),
        createdAt = DemoCreatedAt
    )

    private data class Credential(
        val salt: String,
        val hash: String
    )

    companion object {
        const val DemoPassword = "ShareMeal123!"
        private const val DemoCreatedAt = 1_700_000_000_000L
    }
}
