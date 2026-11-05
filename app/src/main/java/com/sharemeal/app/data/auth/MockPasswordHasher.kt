package com.sharemeal.app.data.auth

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockPasswordHasher @Inject constructor() {

    private val secureRandom = SecureRandom()

    fun newSalt(): String {
        val salt = ByteArray(SaltBytes)
        secureRandom.nextBytes(salt)
        return salt.toHex()
    }

    fun hash(password: String, salt: String): String {
        val spec = PBEKeySpec(
            password.toCharArray(),
            salt.hexToBytes(),
            Iterations,
            KeyLengthBits
        )
        return try {
            SecretKeyFactory.getInstance(Algorithm).generateSecret(spec).encoded.toHex()
        } finally {
            spec.clearPassword()
        }
    }

    fun verify(password: String, salt: String, expectedHash: String): Boolean {
        return try {
            MessageDigest.isEqual(
                hash(password, salt).hexToBytes(),
                expectedHash.hexToBytes()
            )
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") { byte ->
        "%02x".format(byte.toInt() and 0xFF)
    }

    private fun String.hexToBytes(): ByteArray {
        require(length % 2 == 0) { "Invalid hexadecimal value." }
        return ByteArray(length / 2) { index ->
            substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    }

    private companion object {
        const val Algorithm = "PBKDF2WithHmacSHA1"
        const val Iterations = 12_000
        const val KeyLengthBits = 256
        const val SaltBytes = 16
    }
}
