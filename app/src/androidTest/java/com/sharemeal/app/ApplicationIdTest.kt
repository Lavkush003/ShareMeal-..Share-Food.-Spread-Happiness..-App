package com.sharemeal.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ApplicationIdTest {

    @Test
    fun applicationIdIsShareMeal() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        assertTrue(context.packageName.startsWith("com.sharemeal.app"))
    }
}
