package com.yahpz.responder

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.LayoutDirection
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.yahpz.domain.EventStatus
import com.yahpz.domain.ParticipationStatus
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
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Renders the leaders' events tab from fixtures, no backend. Writes PNGs to
 * app/build/outputs/roborazzi/. Pixel 8 size (1080x2400 @ 420dpi), Hebrew, RTL.
 *
 * Run: ./gradlew :app:testDebugUnitTest --tests '*UnitEventsScreenshotTest*'
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "he-w411dp-h914dp-420dpi")
class UnitEventsScreenshotTest {
    /** The test activity is registered only in debug (ui-test-manifest), so release skips. */
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
    fun list() {
        render(UnitEventsFixtures.leadState())
        compose.onRoot().captureRoboImage("$OUT/list.png")
    }

    @Test
    fun listScrolled() {
        render(UnitEventsFixtures.leadState())
        compose.onRoot().performTouchInput { swipeUp(startY = bottom * 0.9f, endY = top, durationMillis = 600) }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("$OUT/list-scrolled.png")
    }

    @Test
    fun filtersSheet() {
        render(UnitEventsFixtures.leadState())
        compose.onNode(hasContentDescription("סינון", substring = true) and hasClickAction()).performClick()
        compose.waitForIdle()
        captureScreenRoboImage("$OUT/filters-sheet.png")
    }

    @Test
    fun empty() {
        render(UnitEventsFixtures.leadState().copy(unitEvents = emptyList(), myActiveUnitEvents = emptyList()))
        compose.onRoot().captureRoboImage("$OUT/empty.png")
    }

    private fun render(ui: AppUiState) {
        compose.setContent { YahpazTestTheme { Box(Modifier.fillMaxSize()) { UnitEventsContent(ui, UnitEventsFixtures.noOpActions, today = UnitEventsFixtures.TODAY) } } }
        compose.waitForIdle()
    }

    companion object {
        const val OUT = "build/outputs/roborazzi"
    }
}

/** Same theme MainActivity applies: RTL, light Field palette, Plex type. */
@Composable
fun YahpazTestTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = FieldTheme.accent,
                onPrimary = FieldTheme.textOnAccent,
                background = FieldTheme.page,
                surface = FieldTheme.raised,
                onBackground = FieldTheme.textPrimary,
                onSurface = FieldTheme.textPrimary,
            ),
            typography = yahpazTypography(),
            content = content,
        )
    }
}

object UnitEventsFixtures {
    const val VIEWER = "lead-omri"

    /** Fixed so "היום" / "אתמול" render the same on any day. */
    val TODAY: LocalDate = LocalDate.of(2026, 11, 16)

    private fun hoursAgo(h: Long) = Instant.now().minus(h, ChronoUnit.HOURS).toString()

    private fun responder(id: String, status: ParticipationStatus, name: String) = ResponderSummary(
        id = "p-$id",
        responderId = id,
        status = status,
        profile = PersonName(fullName = name),
    )

    private val lead = PersonName(fullName = "עמרי לנדמן", callsign = "12")
    private val otherLead = PersonName(fullName = "דנה כהן", callsign = "31")

    /** On the viewer's own board: they lead it and it is still open, so it is locked there. */
    private val activeLocked = EventListItem(
        id = "e1",
        eventDate = "2026-11-16",
        policeEventId = "4417",
        patrolCallsign = "12",
        startedAt = "2026-11-16T07:40:00+02:00",
        createdAt = hoursAgo(2),
        location = "מחלף שורק",
        status = EventStatus.IN_PROGRESS,
        shiftLeadId = VIEWER,
        district = Named("מרכז"),
        eventType = Named("תאונת דרכים"),
        road = Named("כביש 1"),
        shiftLead = lead,
        hasImage = true,
        responders = listOf(
            responder("r1", ParticipationStatus.DONE, "יוסי לוי"),
            responder("r2", ParticipationStatus.PENDING, "מיכל אברהם"),
            responder("r3", ParticipationStatus.IN_PROGRESS, "אבי מזרחי"),
        ),
    )

    /** Pinned by the viewer, led by someone else, so it can be removed. */
    private val activePinned = EventListItem(
        id = "e2",
        eventDate = "2026-11-16",
        policeEventId = "4422",
        patrolCallsign = "31",
        startedAt = "2026-11-16T09:05:00+02:00",
        createdAt = hoursAgo(1),
        location = "מחלף בן שמן",
        status = EventStatus.PARTIAL,
        shiftLeadId = "lead-dana",
        district = Named("מרכז"),
        eventType = Named("רכב תקוע בנתיב"),
        road = Named("כביש 443"),
        shiftLead = otherLead,
        responders = listOf(responder("r4", ParticipationStatus.DONE, "רון שגיא")),
    )

    /** Missing police id, road and end time: the "needs attention" case. */
    private val incomplete = EventListItem(
        id = "e3",
        eventDate = "2026-11-16",
        startedAt = "2026-11-16T05:15:00+02:00",
        createdAt = hoursAgo(4),
        location = "יציאה לשער הגיא",
        status = EventStatus.IN_PROGRESS,
        shiftLeadId = VIEWER,
        district = Named("מרכז"),
        eventType = Named("פינוי מפגע"),
        shiftLead = lead,
        responders = listOf(responder("r5", ParticipationStatus.PENDING, "נועה ברק")),
    )

    private val longTitle = EventListItem(
        id = "e4",
        eventDate = "2026-11-15",
        policeEventId = "4380",
        patrolCallsign = "12",
        startedAt = "2026-11-15T22:30:00+02:00",
        endedAt = "2026-11-15T23:50:00+02:00",
        createdAt = hoursAgo(6),
        location = "כניסה לקיבוץ",
        status = EventStatus.DONE,
        shiftLeadId = "lead-dana",
        district = Named("מרכז"),
        eventType = Named("חילוץ רכב משטח פתוח לאחר סערה בכביש הגישה הצפוני ליישוב"),
        road = Named("כביש 38"),
        shiftLead = otherLead,
        responders = listOf(
            responder("r6", ParticipationStatus.DONE, "יוסי לוי"),
            responder("r7", ParticipationStatus.DONE, "רון שגיא"),
        ),
    )

    private val longAddress = EventListItem(
        id = "e5",
        eventDate = "2026-11-15",
        policeEventId = "4371",
        patrolCallsign = "12",
        startedAt = "2026-11-15T18:10:00+02:00",
        endedAt = "2026-11-15T19:00:00+02:00",
        createdAt = hoursAgo(8),
        location = "צומת הכניסה הדרומית ליישוב, ליד תחנת הדלק, כ-200 מטר מזרחית לגשר הרכבת",
        status = EventStatus.PARTIAL,
        shiftLeadId = VIEWER,
        district = Named("מרכז"),
        eventType = Named("גרירה"),
        road = Named("כביש 6"),
        shiftLead = lead,
        frozenOver60km = true,
        responders = listOf(
            responder("r8", ParticipationStatus.DONE, "מיכל אברהם"),
            responder("r9", ParticipationStatus.PENDING, "אבי מזרחי"),
        ),
    )

    private val cancelled = EventListItem(
        id = "e6",
        eventDate = "2026-11-15",
        policeEventId = "4366",
        patrolCallsign = "31",
        startedAt = "2026-11-15T15:20:00+02:00",
        endedAt = "2026-11-15T15:25:00+02:00",
        createdAt = hoursAgo(10),
        location = "מחלף לטרון",
        status = EventStatus.DONE,
        isCancelled = true,
        shiftLeadId = "lead-dana",
        district = Named("מרכז"),
        eventType = Named("תאונת דרכים"),
        road = Named("כביש 1"),
        shiftLead = otherLead,
    )

    /** Dated three days back to show the older-date format. */
    private val shiftBorn = EventListItem(
        id = "e7",
        eventDate = "2026-11-13",
        policeEventId = "4359",
        startedAt = "2026-11-13T12:00:00+02:00",
        endedAt = "2026-11-13T12:40:00+02:00",
        createdAt = hoursAgo(11),
        location = "מחלף חמד",
        status = EventStatus.DONE,
        origin = "shift",
        shiftLeadId = VIEWER,
        district = Named("מרכז"),
        eventType = Named("סיוע לנהג"),
        road = Named("כביש 1"),
        shiftLead = lead,
        treatmentDetail = "החלפת גלגל",
        responders = listOf(responder("r10", ParticipationStatus.DONE, "נועה ברק")),
    )

    fun leadState() = AppUiState(
        booting = false,
        userId = VIEWER,
        roles = listOf("shift_lead"),
        unitEvents = listOf(activePinned, incomplete, longTitle, longAddress, cancelled, shiftBorn),
        myActiveUnitEvents = listOf(activeLocked),
        myActiveEventPrefs = listOf(MyActiveEventPrefRow(userId = VIEWER, eventId = "e2", kind = "pin")),
        // Shift-born rows only show with the shift chip on; keep one visible.
        showShiftEvents = true,
    )

    val noOpActions = UnitEventsActions(
        reload = {},
        setShowShiftEvents = {},
        setShowOlderUnitEvents = {},
        setShowOthersCreatedEvents = {},
        addToMyActive = {},
        removeFromMyActive = {},
        openEditEvent = {},
        openFill = {},
        deleteEvent = { null },
        showToast = { _, _ -> },
        loadDetailResponders = { error("no backend in screenshot tests") },
    )
}
