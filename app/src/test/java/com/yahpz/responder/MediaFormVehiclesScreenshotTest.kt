package com.yahpz.responder

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.performScrollTo
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.yahpz.domain.EVENT_MEDIA_PENDING_VEHICLE
import com.yahpz.domain.EventMediaPlateOption
import com.yahpz.domain.MediaPlateOwner
import com.yahpz.domain.TreatedPlate
import com.yahpz.domain.mediaVehicleOptions
import com.yahpz.domain.resolveMediaPlateChoice
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The photo step offers a vehicle typed into the form that the report has not saved
 * yet, and says the photo links to it on save. Before the fix only saved rows showed.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "he-w411dp-h914dp-420dpi")
class MediaFormVehiclesScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val owner = MediaPlateOwner.Responder("er-me")
    private val saved = listOf(
        EventMediaPlateOption("p1", "1234567", "TOYOTA COROLLA", "לבן", null, eventResponderId = "er-me"),
    )
    private val form = listOf(TreatedPlate("1234567", "TOYOTA COROLLA", "לבן"), TreatedPlate("7654321"))

    @Test
    fun photoStepOffersAnUnsavedFormVehicleAndSaysItLinksOnSave() {
        assumeTrue("screenshot tests run on the debug variant", BuildConfig.DEBUG)
        System.setProperty("roborazzi.test.record", "true")
        compose.setContent {
            YahpazTestTheme {
                var draft by remember {
                    mutableStateOf(MediaDraft(key = "d1", uri = Uri.parse("file:///nonexistent.jpg")))
                }
                Column(Modifier.verticalScroll(rememberScrollState())) { MediaDraftCard(
                    draft = draft,
                    plates = mediaVehicleOptions(form, saved),
                    platesLoaded = true,
                    canSave = false,
                    pendingVehicle = resolveMediaPlateChoice(draft.treatedPlateIds, saved, owner).missing.isNotEmpty(),
                    onCaption = {},
                    onPlates = { draft = draft.copy(treatedPlateIds = it) },
                    onTakenWhen = {},
                    onSave = {},
                    onRemove = {},
                ) }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("12-345-67", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("76-543-21", substring = true).performScrollTo().assertIsDisplayed().performClick()
        compose.waitForIdle()
        compose.onNodeWithText(EVENT_MEDIA_PENDING_VEHICLE).performScrollTo().assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/media-step-unsaved-vehicle.png")
    }
}
