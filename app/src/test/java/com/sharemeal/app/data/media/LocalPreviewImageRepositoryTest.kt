package com.sharemeal.app.data.media

import com.sharemeal.app.core.common.AppResult
import com.sharemeal.app.core.media.ImageReference
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalPreviewImageRepositoryTest {

    private val repository = LocalPreviewImageRepository()

    @Test
    fun `local preview keeps the selected uri without a remote url`() {
        val image = repository.createLocalPreview("content://media/food/1")

        assertEquals("content://media/food/1", image.id)
        assertEquals("content://media/food/1", image.localUri)
        assertNull(image.remoteUrl)
        assertTrue(image.isLocalOnly)
        assertEquals(
            "content://media/food/1",
            repository.displayUrl(image)
        )
    }

    @Test
    fun `remote url is preferred when available`() {
        val image = ImageReference(
            id = "img-1",
            localUri = "content://media/food/1",
            remoteUrl = "https://cdn.example.com/food/1.jpg"
        )

        assertEquals(
            "https://cdn.example.com/food/1.jpg",
            repository.displayUrl(image)
        )
    }

    @Test
    fun `upload is unavailable in local preview mode`() = runTest {
        val result = repository.uploadImage(
            localUri = "content://media/food/1",
            mimeType = "image/jpeg"
        )

        assertTrue(result is AppResult.Failure)
    }
}
