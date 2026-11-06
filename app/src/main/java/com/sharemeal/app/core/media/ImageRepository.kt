package com.sharemeal.app.core.media

import com.sharemeal.app.core.common.AppResult
import kotlinx.serialization.Serializable

@Serializable
data class ImageReference(
    val id: String,
    val localUri: String? = null,
    val remoteUrl: String? = null
) {
    val isLocalOnly: Boolean
        get() = remoteUrl.isNullOrBlank()
}

interface ImageRepository {
    fun createLocalPreview(localUri: String): ImageReference
    fun displayUrl(reference: ImageReference): String
    suspend fun uploadImage(localUri: String, mimeType: String): AppResult<ImageReference>
}
