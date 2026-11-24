package com.yahpz.responder

import android.provider.Settings
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppSettingsIntentTest {
    @Test
    fun opensTheAppDetailsPageOfThePackage() {
        val intent = appSettingsIntent("com.yahpz.responder")
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
        assertEquals("package:com.yahpz.responder", intent.data.toString())
    }
}
