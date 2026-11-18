package com.sharemeal.app.data.sync

import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface SyncApiService {
    @POST
    suspend fun sendOperation(
        @Url path: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body payload: JsonObject
    ): Response<JsonObject>
}
