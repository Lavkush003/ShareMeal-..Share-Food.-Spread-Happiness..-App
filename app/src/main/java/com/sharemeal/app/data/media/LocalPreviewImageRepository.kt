package com.sharemeal.app.data.media

import com.sharemeal.app.core.common.AppError
import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.core.media.ImageReference
import com.sharemeal.app.core.media.ImageRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalPreviewImageRepository @Inject constructor() : ImageRepository {

    override fun createLocalPreview(localUri: String): ImageReference = ImageReference(
        id = localUri,
        localUri = localUri
    )

    override fun displayUrl(reference: ImageReference): String =
        reference.remoteUrl ?: reference.localUri.orEmpty()

    override suspend fun uploadImage(
        localUri: String,
        mimeType: String
    ): AppResult<ImageReference> = AppResult.Failure(
        AppError.Unknown("Image upload is not available in local preview mode.")
    )
}
