package com.sharemeal.app.core.media

import com.sharemeal.app.core.common.AppResult

data class ImageReference(
    val id: String,
    val url: String
)

interface ImageRepository {
    suspend fun uploadImage(localUri: String, mimeType: String): AppResult<ImageReference>
    fun displayUrl(reference: ImageReference): String
}
