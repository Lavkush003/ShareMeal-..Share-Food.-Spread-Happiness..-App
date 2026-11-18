package com.sharemeal.app.data.local.database

import androidx.room.TypeConverter
import com.sharemeal.app.core.media.ImageReference
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RoomConverters {

    @TypeConverter
    fun stringListToJson(value: List<String>): String = json.encodeToString(value)

    @TypeConverter
    fun jsonToStringList(value: String?): List<String> = decode(value, emptyList())

    @TypeConverter
    fun imageListToJson(value: List<ImageReference>): String = json.encodeToString(value)

    @TypeConverter
    fun jsonToImageList(value: String?): List<ImageReference> = decode(value, emptyList())

    private inline fun <reified T> decode(value: String?, fallback: List<T>): List<T> {
        if (value.isNullOrBlank()) return fallback
        return try {
            json.decodeFromString<List<T>>(value)
        } catch (_: SerializationException) {
            fallback
        } catch (_: IllegalArgumentException) {
            fallback
        }
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
