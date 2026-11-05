package com.sharemeal.app.domain.model

data class AuthSession(
    val userId: String,
    val role: UserRole
)
