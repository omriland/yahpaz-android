package com.yahpz.responder

import android.os.Bundle
import android.os.Looper
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Fold and rotation recreate MainActivity: no booting spinner, same tab, same open fill.
 * Process death builds a fresh AppModel: tab and open fill come back from the saved map.
 *
 * Needs two test seams in product code, both reset in [clearSeams]:
 * [YahpazAPI.fakeSignedInSession] and [fakeSideloadUpdateCheck].
 * Seam contract: while [YahpazAPI.fakeSignedInSession] is set, every YahpazAPI fetch that
 * applySession triggers (events, shifts, vehicles, contacts, current shift) must return an
 * empty result without touching the network, so boot is offline and deterministic.
 * Process death goes through MainActivity: its saved Bundle is read back in onCreate while
 * bootstrap is still running.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "he-w411dp-h914dp-420dpi")
class ActivityRecreateTest {
    private val signedInSession = FakeSignedInSession(
        userId = "user-1",
        profile = ProfileRecord(id = "user-1"),
        roles = listOf("responder"),
    )

    @Before
    fun installSeams() {
        YahpazAPI.fakeSignedInSession = signedInSession
        fakeSideloadUpdateCheck = SideloadUpdateCheck()
    }

    @After
    fun clearSeams() {
        YahpazAPI.fakeSignedInSession = null
        fakeSideloadUpdateCheck = null
    }

    private fun waitUntilBootFinished(model: AppModel) {
        val deadline = System.currentTimeMillis() + BOOT_TIMEOUT_MS
        while (model.state.value.booting && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(BOOT_POLL_MS)
        }
        assertFalse("bootstrap did not finish", model.state.value.booting)
    }

    private fun modelOf(activity: MainActivity): AppModel =
        ViewModelProvider(activity)[AppModel::class.java]

    private fun openContactsWithAFill(model: AppModel) {
        model.setTab(AppTab.CONTACTS)
        model.openFill("event-1")
    }

    @Test
    fun recreateDoesNotShowTheBootingSpinnerAgain() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity(::waitForSignedInBoot)
            val bootingValuesAfterRecreate = mutableListOf<Boolean>()
            val collecting = CoroutineScope(Dispatchers.Unconfined).launch {
                modelOfScenario(scenario).state
                    .drop(1)
                    .collect { bootingValuesAfterRecreate.add(it.booting) }
            }
            scenario.recreate()
            scenario.onActivity { waitUntilBootFinished(modelOf(it)) }
            collecting.cancel()
            assertFalse("booting went back to true", bootingValuesAfterRecreate.contains(true))
        }
    }

    @Test
    fun recreateKeepsTheOpenTabAndTheOpenFill() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity(::waitForSignedInBoot)
            scenario.onActivity { openContactsWithAFill(modelOf(it)) }
            scenario.recreate()
            scenario.onActivity {
                val model = modelOf(it)
                waitUntilBootFinished(model)
                assertEquals(AppTab.CONTACTS, model.state.value.tab)
                assertEquals("event-1", model.state.value.fillEventId)
            }
        }
    }

    @Test
    fun aFreshActivityRestoresTheTabAndTheFillFromTheSavedBundle() {
        val savedBundle = saveStateOfActivityWith { openContactsWithAFill(it) }

        val restored = startFreshActivityFrom(savedBundle)
        val model = modelOf(restored)
        waitForSignedInBoot(restored)

        assertEquals(AppTab.CONTACTS, model.state.value.tab)
        assertEquals("event-1", model.state.value.fillEventId)
        assertFalse(model.state.value.fillShiftBorn)
    }

    @Test
    fun aFreshActivityRestoresAShiftBornFillFromTheSavedBundle() {
        val savedBundle = saveStateOfActivityWith { it.openShiftBornFill("event-2") }

        val restored = startFreshActivityFrom(savedBundle)
        val model = modelOf(restored)
        waitForSignedInBoot(restored)

        assertEquals("event-2", model.state.value.fillEventId)
        assertTrue(model.state.value.fillShiftBorn)
    }

    private fun saveStateOfActivityWith(openNavigation: (AppModel) -> Unit): Bundle {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        waitForSignedInBoot(controller.get())
        openNavigation(modelOf(controller.get()))
        val savedBundle = Bundle()
        controller.saveInstanceState(savedBundle)
        controller.pause().stop().destroy()
        return savedBundle
    }

    private fun startFreshActivityFrom(savedBundle: Bundle): MainActivity =
        Robolectric.buildActivity(MainActivity::class.java).create(savedBundle).start().resume().get()

    private fun modelOfScenario(scenario: ActivityScenario<MainActivity>): AppModel {
        lateinit var model: AppModel
        scenario.onActivity { model = modelOf(it) }
        return model
    }

    private fun waitForSignedInBoot(activity: MainActivity) {
        val model = modelOf(activity)
        waitUntilBootFinished(model)
        assertTrue(model.state.value.isSignedIn)
    }

    private companion object {
        const val BOOT_TIMEOUT_MS = 5_000L
        const val BOOT_POLL_MS = 20L
    }
}
