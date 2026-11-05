package com.sharemeal.app.domain.model

data class SavedSession(
    val userId: String,
    val role: UserRole
)

enum class PasswordResetResult {
    BACKEND_REQUIRED
}
