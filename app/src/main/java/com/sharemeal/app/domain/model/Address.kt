package com.sharemeal.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Address(
    val line1: String,
    val city: String,
    val state: String,
    val pinCode: String
)
