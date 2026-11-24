package com.yahpz.responder

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.yahpz.domain.CurrentShift
import com.yahpz.domain.EVENT_DELETE_ACTION
import com.yahpz.domain.LIVE_SHIFT_NEW_EVENT_ACTION
import com.yahpz.domain.LIVE_SHIFT_ODOMETER_ACTION
import com.yahpz.domain.ON_SHIFT_BAR_ACTION
import com.yahpz.domain.LookupOption
import com.yahpz.domain.NEW_EVENT_HINT
import com.yahpz.domain.NEW_EVENT_MORE_TYPES_TITLE
import com.yahpz.domain.NEW_EVENT_PRIMARY_TYPES
import com.yahpz.domain.NEW_EVENT_SHEET_CLOSE
import com.yahpz.domain.NEW_EVENT_SHEET_TITLE
import com.yahpz.domain.SHIFT_EVENT_DELETE_BODY
import com.yahpz.domain.SHIFT_EVENT_DELETE_CANCEL
import com.yahpz.domain.SHIFT_EVENT_DELETE_TITLE
import com.yahpz.domain.ShiftPhase
import com.yahpz.domain.ShiftStatus
import com.yahpz.domain.barText
import com.yahpz.domain.formatJerusalemTimeOfDay
import com.yahpz.domain.liveShiftEventTypeLabel
import com.yahpz.domain.liveShiftEventsTitle
import com.yahpz.domain.liveShiftStamp
import com.yahpz.domain.shiftTimeLeftLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

/**
 * Renders the live shift screen, the new-event sheet and the on-shift bar from fixtures, no backend.
 * Pixel 8 size, Hebrew, RTL. PNGs land in app/build/outputs/roborazzi/.
 *
 * Run: ./gradlew :app:testDebugUnitTest --tests '*LiveShiftScreenshotTest*'
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "he-w411dp-h914dp-420dpi")
class LiveShiftScreenshotTest {
    @get:Rule(order = 0)
    val debugOnly = TestRule { base, _ ->
        object : Statement() {
            override fun evaluate() {
                assumeTrue("screenshot tests run on the debug variant", BuildConfig.DEBUG)
                base.evaluate()
            }
        }
    }

    @get:Rule(order = 1)
    val compose = createComposeRule()

    init {
        System.setProperty("roborazzi.test.record", "true")
    }

    @Test
    fun screenActive() {
        renderScreen(LiveShiftFixtures.state(ShiftPhase.ACTIVE))
        assertHeaderShown(ShiftPhase.ACTIVE)
        compose.onNodeWithText(LIVE_SHIFT_NEW_EVENT_ACTION).assertIsDisplayed()
        compose.onRoot().captureRoboImage("$OUT/live-shift-active.png")
    }

    @Test
    fun screenFinishing() {
        renderScreen(LiveShiftFixtures.state(ShiftPhase.FINISHING))
        assertHeaderShown(ShiftPhase.FINISHING)
        compose.onNodeWithText(LIVE_SHIFT_NEW_EVENT_ACTION).assertIsDisplayed()
        compose.onRoot().captureRoboImage("$OUT/live-shift-finishing.png")
    }

    @Test
    fun screenReadOnlyHidesNewEventButton() {
        renderScreen(LiveShiftFixtures.state(ShiftPhase.CLOSED))
        assertHeaderShown(ShiftPhase.CLOSED)
        compose.onNodeWithText(liveShiftEventsTitle(2)).assertIsDisplayed()
        compose.onNodeWithText(LIVE_SHIFT_NEW_EVENT_ACTION).assertDoesNotExist()
        compose.onNodeWithText(EVENT_DELETE_ACTION).assertDoesNotExist()
        compose.onRoot().captureRoboImage("$OUT/live-shift-read-only.png")
    }

    @Test
    fun screenListsEventsWithCount() {
        renderScreen(LiveShiftFixtures.state(ShiftPhase.ACTIVE))
        compose.onNodeWithText(liveShiftEventsTitle(2)).assertIsDisplayed()
        compose.onNodeWithText(liveShiftEventTypeLabel("תאונה")).assertIsDisplayed()
        compose.onNodeWithText(liveShiftEventTypeLabel("גרירה")).assertIsDisplayed()
        compose.onNodeWithText("מחלף שורק").assertIsDisplayed()
    }

    @Test
    fun screenOpensTappedEvent() {
        val opened = mutableListOf<String>()
        renderScreen(LiveShiftFixtures.state(ShiftPhase.ACTIVE), LiveShiftFixtures.actions(onOpenEvent = { opened += it }))
        // The fixture typeName is 'תאונה'. The screen shows the suffix like Models.kt typeLabel does for origin == "shift".
        compose.onNodeWithText(liveShiftEventTypeLabel("תאונה")).performClick()
        assertEquals(listOf("ev-empty"), opened)
    }

    @Test
    fun screenOffersDeleteOnEmptyEventOnly() {
        val deleted = mutableListOf<String>()
        renderScreen(LiveShiftFixtures.state(ShiftPhase.ACTIVE), LiveShiftFixtures.actions(onDelete = { deleted += it }))
        assertEquals(1, compose.onAllNodesWithText(EVENT_DELETE_ACTION).fetchSemanticsNodes().size)
        compose.onNodeWithText(EVENT_DELETE_ACTION).performClick()
        confirmDelete()
        assertEquals(listOf("ev-empty"), deleted)
    }

    @Test
    fun screenOffersManagerDeleteOnEmptyEventOnlyOnClosedShift() {
        val deleted = mutableListOf<String>()
        renderScreen(
            LiveShiftFixtures.state(ShiftPhase.CLOSED, canManage = true),
            LiveShiftFixtures.actions(onDelete = { deleted += it }),
        )
        assertEquals(1, compose.onAllNodesWithText(EVENT_DELETE_ACTION).fetchSemanticsNodes().size)
        compose.onNodeWithText(EVENT_DELETE_ACTION).performClick()
        confirmDelete()
        assertEquals(listOf("ev-empty"), deleted)
    }

    @Test
    fun screenOffersNoDeleteOnClosedShift() {
        renderScreen(LiveShiftFixtures.state(ShiftPhase.CLOSED))
        compose.onNodeWithText(EVENT_DELETE_ACTION).assertDoesNotExist()
    }

    @Test
    fun screenAsksBeforeDeleting() {
        val deleted = mutableListOf<String>()
        renderScreen(LiveShiftFixtures.state(ShiftPhase.ACTIVE), LiveShiftFixtures.actions(onDelete = { deleted += it }))
        compose.onNodeWithText(EVENT_DELETE_ACTION).performClick()
        compose.onNodeWithText(SHIFT_EVENT_DELETE_TITLE).assertIsDisplayed()
        compose.onNodeWithText(SHIFT_EVENT_DELETE_BODY).assertIsDisplayed()
        assertEquals(emptyList<String>(), deleted)
        compose.onRoot().captureRoboImage("$OUT/live-shift-delete-confirm.png")
        confirmDelete()
        assertEquals(listOf("ev-empty"), deleted)
    }

    @Test
    fun screenCancelledDeleteDeletesNothing() {
        val deleted = mutableListOf<String>()
        renderScreen(LiveShiftFixtures.state(ShiftPhase.ACTIVE), LiveShiftFixtures.actions(onDelete = { deleted += it }))
        compose.onNodeWithText(EVENT_DELETE_ACTION).performClick()
        compose.onNodeWithText(SHIFT_EVENT_DELETE_CANCEL).performClick()
        compose.waitForIdle()
        compose.onNodeWithText(SHIFT_EVENT_DELETE_TITLE).assertDoesNotExist()
        assertEquals(emptyList<String>(), deleted)
    }

    @Test
    fun screenOpensOdometerSheetButton() {
        var odometerOpened = false
        renderScreen(LiveShiftFixtures.state(ShiftPhase.ACTIVE), LiveShiftFixtures.actions(onOpenOdometer = { odometerOpened = true }))
        compose.onNodeWithText(LIVE_SHIFT_ODOMETER_ACTION).performClick()
        assertEquals(true, odometerOpened)
    }

    @Test
    fun sheet() {
        renderSheet()
        compose.onNodeWithText(NEW_EVENT_SHEET_TITLE).assertIsDisplayed()
        compose.onNodeWithText(NEW_EVENT_SHEET_CLOSE).assertIsDisplayed()
        compose.onNodeWithText(NEW_EVENT_MORE_TYPES_TITLE).assertIsDisplayed()
        compose.onNodeWithText(NEW_EVENT_HINT).assertIsDisplayed()
        compose.onNodeWithText("תאונה").assertIsDisplayed()
        compose.onNodeWithText("גרירה").assertIsDisplayed()
        val moreTitleTop = compose.onNodeWithText(NEW_EVENT_MORE_TYPES_TITLE).getBoundsInRoot().top
        for (primaryType in NEW_EVENT_PRIMARY_TYPES) {
            val primaryBottom = compose.onNodeWithText(primaryType).getBoundsInRoot().bottom
            assertTrue("'$primaryType' sits above the more-types title", primaryBottom <= moreTitleTop)
        }
        val moreTitleBottom = compose.onNodeWithText(NEW_EVENT_MORE_TYPES_TITLE).getBoundsInRoot().bottom
        val moreTypes = LiveShiftFixtures.eventTypes.map { it.name }.filterNot { it in NEW_EVENT_PRIMARY_TYPES }
        assertTrue("the fixture has more-types to check", moreTypes.isNotEmpty())
        for (moreType in moreTypes) {
            compose.onNodeWithText(moreType).assertIsDisplayed()
            val moreTypeTop = compose.onNodeWithText(moreType).getBoundsInRoot().top
            assertTrue("'$moreType' sits below the more-types title", moreTypeTop >= moreTitleBottom)
        }
        captureScreenRoboImage("$OUT/new-event-sheet.png")
    }

    @Test
    fun sheetPickCallsBackWithTappedType() {
        val picked = mutableListOf<String>()
        renderSheet(onPick = { picked += it.id })
        compose.onNodeWithText("תקוע").performClick()
        assertEquals(listOf("t-stuck"), picked)
    }

    @Test
    fun sheetShowsServerError() {
        renderSheet(error = "המשמרת אינה פתוחה לתיעוד")
        compose.onNodeWithText("המשמרת אינה פתוחה לתיעוד").assertIsDisplayed()
        captureScreenRoboImage("$OUT/new-event-sheet-error.png")
    }

    @Test
    fun barActive() {
        renderBar(ShiftPhase.ACTIVE)
        compose.onNodeWithText(LiveShiftFixtures.barTextFor(ShiftPhase.ACTIVE)).assertIsDisplayed()
        compose.onNodeWithText(ON_SHIFT_BAR_ACTION).assertIsDisplayed()
        compose.onRoot().captureRoboImage("$OUT/on-shift-bar-active.png")
        assertEquals(FieldTheme.accentSubtle.toArgb(), barBackgroundPixel("$OUT/on-shift-bar-active.png"))
    }

    @Test
    fun barFinishing() {
        renderBar(ShiftPhase.FINISHING)
        compose.onNodeWithText(LiveShiftFixtures.barTextFor(ShiftPhase.FINISHING)).assertIsDisplayed()
        compose.onNodeWithText(ON_SHIFT_BAR_ACTION).assertIsDisplayed()
        compose.onRoot().captureRoboImage("$OUT/on-shift-bar-finishing.png")
        assertEquals(FieldTheme.partialTint.toArgb(), barBackgroundPixel("$OUT/on-shift-bar-finishing.png"))
    }

    @Test
    fun barActionOpensShift() {
        var opened = false
        renderBar(ShiftPhase.ACTIVE, onOpen = { opened = true })
        compose.onNodeWithText(ON_SHIFT_BAR_ACTION).performClick()
        assertEquals(true, opened)
    }

    /** Title, hours, stamp and time left, each built from :domain like the screen builds them. */
    private fun assertHeaderShown(phase: ShiftPhase) {
        val shift = LiveShiftFixtures.shiftFor(phase)
        compose.onNodeWithText(shift.title).assertIsDisplayed()
        val hours = "${formatJerusalemTimeOfDay(Instant.parse(LiveShiftFixtures.STARTS_AT))}–${formatJerusalemTimeOfDay(Instant.parse(LiveShiftFixtures.ENDS_AT))}"
        compose.onNodeWithText(hours).assertIsDisplayed()
        // The header stamp can share its words with an event's stamp (both 'ממתין לתיעוד'), so match the first.
        compose.onAllNodesWithText(liveShiftStamp(phase, shift.status).label)[0].assertIsDisplayed()
        val timeLeft = shiftTimeLeftLabel(phase, Instant.parse(LiveShiftFixtures.ENDS_AT), LiveShiftFixtures.nowFor(phase))
        if (timeLeft == null) {
            assertNoStaleTimeLeft(phase)
            return
        }
        compose.onNodeWithText(timeLeft).assertIsDisplayed()
    }

    /** A closed shift shows no time left: no label a phase-blind screen could build at that instant. */
    private fun assertNoStaleTimeLeft(phase: ShiftPhase) {
        if (phase != ShiftPhase.CLOSED) return
        val endsAt = Instant.parse(LiveShiftFixtures.ENDS_AT)
        val closedNow = LiveShiftFixtures.nowFor(ShiftPhase.CLOSED)
        val wrongLabels = listOfNotNull(
            shiftTimeLeftLabel(ShiftPhase.ACTIVE, endsAt, closedNow),
            shiftTimeLeftLabel(ShiftPhase.FINISHING, endsAt, closedNow),
        )
        for (label in wrongLabels) compose.onNodeWithText(label).assertDoesNotExist()
        if (wrongLabels.isEmpty()) {
            compose.onNodeWithText("עוד ", substring = true).assertDoesNotExist()
        }
    }

    /** Reads the bar's background from the screenshot just written (captureToImage times out under Robolectric). */
    private fun barBackgroundPixel(path: String): Int =
        android.graphics.BitmapFactory.decodeFile(path).getPixel(BAR_PIXEL_INSET, BAR_PIXEL_INSET)

    private fun confirmDelete() {
        compose.onNode(hasText(EVENT_DELETE_ACTION) and hasAnyAncestor(isDialog())).performClick()
        compose.waitForIdle()
    }

    private fun renderScreen(state: LiveShiftState, actions: LiveShiftActions = LiveShiftFixtures.actions()) {
        compose.setContent { YahpazTestTheme { Box(Modifier.fillMaxSize()) { LiveShiftContent(state, actions) } } }
        compose.waitForIdle()
    }

    private fun renderSheet(error: String? = null, onPick: (LookupOption) -> Unit = {}) {
        compose.setContent {
            YahpazTestTheme {
                NewShiftEventSheet(types = LiveShiftFixtures.eventTypes, errorMessage = error, onPick = onPick, onDismiss = {})
            }
        }
        compose.waitForIdle()
    }

    private fun renderBar(phase: ShiftPhase, onOpen: () -> Unit = {}) {
        compose.setContent {
            YahpazTestTheme {
                Column { OnShiftBar(text = LiveShiftFixtures.barTextFor(phase), phase = phase, onOpen = onOpen) }
            }
        }
        compose.waitForIdle()
    }

    companion object {
        const val OUT = "build/outputs/roborazzi"
        const val BAR_PIXEL_INSET = 2
    }
}

object LiveShiftFixtures {
    const val STARTS_AT = "2026-11-16T05:00:00Z"
    const val ENDS_AT = "2026-11-16T13:00:00Z"
    val NOW: Instant = Instant.parse("2026-11-16T08:00:00Z")
    val NOW_AFTER_END: Instant = Instant.parse("2026-11-16T15:00:00Z")
    val NOW_AFTER_CLOSE: Instant = Instant.parse("2026-11-19T13:00:00Z")

    /** A finishing shift is past its end, a closed one is past end + 48 h, every other phase is mid-shift. */
    fun nowFor(phase: ShiftPhase): Instant = when (phase) {
        ShiftPhase.FINISHING -> NOW_AFTER_END
        ShiftPhase.CLOSED -> NOW_AFTER_CLOSE
        else -> NOW
    }

    fun shiftFor(phase: ShiftPhase): ShiftListItem =
        if (phase == ShiftPhase.CLOSED) shift.copy(status = ShiftStatus.CLOSED) else shift

    val eventTypes = listOf(
        LookupOption(id = "t-accident", name = "תאונה"),
        LookupOption(id = "t-stuck", name = "תקוע"),
        LookupOption(id = "t-abandoned", name = "נטוש"),
        LookupOption(id = "t-flipped", name = "הפוך"),
        LookupOption(id = "t-tow", name = "גרירה"),
        LookupOption(id = "t-hazard", name = "פינוי מפגע"),
        LookupOption(id = "t-assist", name = "סיוע לנהג"),
    )

    val shift = ShiftListItem(
        id = "shift-1",
        shiftDate = "2026-11-16",
        shiftKind = "morning",
        vehicleType = "patrol_north",
        status = ShiftStatus.IN_PROGRESS,
        startsAt = STARTS_AT,
        endsAt = ENDS_AT,
    )

    /** One empty event the viewer may delete, and one filled and done. */
    private val events = listOf(
        LiveShiftEvent(
            id = "ev-empty",
            typeName = "תאונה",
            location = null,
            openedAt = Instant.parse("2026-11-16T07:30:00Z"),
            empty = true,
            done = false,
        ),
        LiveShiftEvent(
            id = "ev-done",
            typeName = "גרירה",
            location = "מחלף שורק",
            openedAt = Instant.parse("2026-11-16T06:10:00Z"),
            empty = false,
            done = true,
        ),
    )

    fun state(phase: ShiftPhase, canManage: Boolean = false) = LiveShiftState(
        shift = shiftFor(phase),
        phase = phase,
        now = nowFor(phase),
        events = events,
        crewNames = listOf("יוסי לוי", "מיכל אברהם"),
        canManage = canManage,
    )

    fun barTextFor(phase: ShiftPhase): String = barText(
        shift = CurrentShift(
            id = shift.id,
            shiftKind = shift.shiftKind,
            shiftDate = shift.shiftDate,
            startsAt = Instant.parse(LiveShiftFixtures.STARTS_AT),
            endsAt = Instant.parse(LiveShiftFixtures.ENDS_AT),
            vehicleType = shift.vehicleType,
        ),
        phase = phase,
        now = nowFor(phase),
    )

    fun actions(
        onOpenEvent: (String) -> Unit = {},
        onDelete: (String) -> Unit = {},
        onOpenOdometer: () -> Unit = {},
    ) = LiveShiftActions(
        refresh = {},
        openNewEvent = {},
        openEvent = onOpenEvent,
        deleteEvent = { eventId -> onDelete(eventId); null },
        openOdometer = onOpenOdometer,
    )
}
