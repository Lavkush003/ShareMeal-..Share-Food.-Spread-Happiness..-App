package com.sharemeal.app.data.sync

import com.sharemeal.app.domain.model.PendingOperation
import com.sharemeal.app.domain.model.SyncConflict
import com.sharemeal.app.domain.model.SyncConflictCode
import com.sharemeal.app.domain.model.SyncRemoteResult
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import retrofit2.HttpException

@Singleton
class RetrofitSyncRemoteDataSource @Inject constructor(
    private val api: SyncApiService,
    private val json: Json
) : SyncRemoteDataSource {

    override suspend fun send(operation: PendingOperation): SyncRemoteResult = try {
        val payload = runCatching { json.parseToJsonElement(operation.payloadJson) }
            .getOrNull() as? JsonObject ?: buildJsonObject { }
        val path = operation.path.trim().trimStart('/')
        val response = api.sendOperation(path, operation.idempotencyKey, payload)
        val body = response.body()
        val snapshot = body?.toString()

        when {
            response.isSuccessful -> SyncRemoteResult.Success(
                serverId = body?.stringField("id", "serverId", "server_id"),
                serverUpdatedAt = body?.longField("serverUpdatedAt", "server_updated_at", "updatedAt"),
                responseJson = snapshot ?: "{}"
            )

            response.code() == 409 -> SyncRemoteResult.Conflict(
                SyncConflict(
                    code = body?.stringField("code", "conflict")
                        ?.let(::conflictCode) ?: SyncConflictCode.UNKNOWN,
                    message = body?.stringField("message", "error")
                        ?: "The server rejected this change because local data is stale.",
                    serverSnapshotJson = snapshot
                )
            )

            else -> SyncRemoteResult.Failure(
                message = "Server returned HTTP ${response.code()}.",
                retryable = response.code() >= 500 || response.code() == 429
            )
        }
    } catch (error: IOException) {
        SyncRemoteResult.Failure(
            message = error.message ?: "Network unavailable.",
            retryable = true
        )
    } catch (error: HttpException) {
        SyncRemoteResult.Failure(
            message = error.message ?: "Server request failed.",
            retryable = error.code() >= 500 || error.code() == 429
        )
    } catch (error: Exception) {
        SyncRemoteResult.Failure(
            message = error.message ?: "Synchronization failed.",
            retryable = false
        )
    }

    private fun conflictCode(value: String): SyncConflictCode =
        runCatching { SyncConflictCode.valueOf(value.uppercase()) }
            .getOrDefault(SyncConflictCode.UNKNOWN)

    private fun JsonObject.stringField(vararg names: String): String? = names
        .firstNotNullOfOrNull { name -> (this[name] as? JsonPrimitive)?.content }

    private fun JsonObject.longField(vararg names: String): Long? = names
        .firstNotNullOfOrNull { name -> (this[name] as? JsonPrimitive)?.content?.toLongOrNull() }
}
