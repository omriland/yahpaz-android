package com.yahpz.responder

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import com.github.takahirom.roborazzi.captureRoboImage
import com.yahpz.domain.MOBILE_MORE_LABEL
import com.yahpz.domain.MobileNavEntry
import com.yahpz.domain.adaptiveLayoutFor
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

/**
 * Phone versus Fold inner screen. Phone must keep the bottom bar and single pane;
 * the wide layout must swap in the rail, list-detail and a capped, centred form.
 * Qualifiers are set per test class run through the helpers below.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "he-w411dp-h914dp-420dpi")
class PhoneLayoutScreenshotTest {
    @get:Rule(order = 0)
    val debugOnly = debugOnlyRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @Test
    fun mainTabsKeepTheBottomBarOnAPhone() {
        compose.setContent { YahpazTestTheme { NavigationFixture() } }
        compose.waitForIdle()
        compose.onNodeWithTag(BOTTOM_BAR_TAG).assertIsDisplayed()
        compose.onNodeWithTag(NAVIGATION_RAIL_TAG).assertDoesNotExist()
        compose.assertMainTabLabelsDisplayed()
        compose.onRoot().captureRoboImage("$LAYOUT_OUT/phone-main-tabs.png")
    }

    @Test
    fun eventsListOpensDetailInASheetOnAPhone() {
        compose.setContent { YahpazTestTheme { EventsFixture() } }
        compose.waitForIdle()
        compose.onNodeWithText("מחלף לטרון", substring = true).performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(DETAIL_PANE_TAG).assertDoesNotExist()
        compose.onNodeWithTag(DETAIL_SHEET_TAG).assertIsDisplayed()
        compose.onNodeWithText(DETAIL_SHEET_TITLE).assertIsDisplayed()
        compose.onRoot().captureRoboImage("$LAYOUT_OUT/phone-events-detail.png")
    }

    @Test
    fun formFillsTheWidthOnAPhone() {
        compose.setContent { YahpazTestTheme { FormFixture() } }
        compose.waitForIdle()
        val width = compose.onNodeWithTag(CAPPED_CONTENT_TAG).getUnclippedBoundsInRoot().width
        assertEquals(411f, width.value, 1f)
        compose.onRoot().captureRoboImage("$LAYOUT_OUT/phone-form.png")
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "he-w841dp-h900dp-420dpi")
class FoldLayoutScreenshotTest {
    @get:Rule(order = 0)
    val debugOnly = debugOnlyRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @Test
    fun mainTabsUseTheRailInsteadOfTheBottomBar() {
        compose.setContent { YahpazTestTheme { NavigationFixture() } }
        compose.waitForIdle()
        compose.onNodeWithTag(NAVIGATION_RAIL_TAG).assertIsDisplayed()
        compose.onNodeWithTag(BOTTOM_BAR_TAG).assertDoesNotExist()
        compose.onNodeWithText(MOBILE_MORE_LABEL).assertIsDisplayed()
        compose.assertMainTabLabelsDisplayed()
        compose.onRoot().captureRoboImage("$LAYOUT_OUT/fold-main-tabs.png")
    }

    @Test
    fun eventsListShowsTheOpenedEventBesideItInsteadOfASheet() {
        compose.setContent { YahpazTestTheme { EventsFixture() } }
        compose.waitForIdle()
        compose.onNodeWithTag(DETAIL_PANE_TAG).assertDoesNotExist()
        compose.onNodeWithText("מחלף לטרון", substring = true).performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(DETAIL_PANE_TAG).assertIsDisplayed()
        compose.onNodeWithTag(LIST_PANE_TAG).assertIsDisplayed()
        compose.onNodeWithTag(DETAIL_SHEET_TAG).assertDoesNotExist()
        val list = compose.onNodeWithTag(LIST_PANE_TAG).getUnclippedBoundsInRoot()
        val detail = compose.onNodeWithTag(DETAIL_PANE_TAG).getUnclippedBoundsInRoot()
        assertEquals(360f, list.width.value, 8f)
        assertPanesSideBySide(list.left.value, list.right.value, detail.left.value, detail.right.value)
        compose.onRoot().captureRoboImage("$LAYOUT_OUT/fold-events-detail.png")
    }

    @Test
    fun formIsCappedNearSixHundredAndCentred() {
        compose.setContent { YahpazTestTheme { FormFixture() } }
        compose.waitForIdle()
        val bounds = compose.onNodeWithTag(CAPPED_CONTENT_TAG).getUnclippedBoundsInRoot()
        assertTrue("capped near 600dp, was ${bounds.width}", bounds.width.value in 560f..640f)
        val startGap = bounds.left.value
        val endGap = 841f - bounds.right.value
        assertEquals(startGap, endGap, 1f)
        compose.onRoot().captureRoboImage("$LAYOUT_OUT/fold-form.png")
    }
}

private const val LAYOUT_OUT = "build/outputs/roborazzi"
private const val CAPPED_CONTENT_TAG = "capped-content"
private const val DETAIL_SHEET_TAG = "unit-event-detail-sheet"
private const val DETAIL_SHEET_TITLE = "פרטי האירוע"
private val MAIN_TAB_LABELS = listOf("האירועים שלי", "המשמרות שלי", "אירועי היחידה", "אנשי קשר")

private fun assertPanesSideBySide(listLeft: Float, listRight: Float, detailLeft: Float, detailRight: Float) {
    val detailBeforeList = detailRight <= listLeft + 1f
    val detailAfterList = detailLeft >= listRight - 1f
    assertTrue(
        "panes overlap: list $listLeft..$listRight, detail $detailLeft..$detailRight",
        detailBeforeList || detailAfterList,
    )
}

private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.assertMainTabLabelsDisplayed() {
    MAIN_TAB_LABELS.forEach { label -> onNodeWithText(label).assertIsDisplayed() }
}

private fun debugOnlyRule() = TestRule { base, _ ->
    object : Statement() {
        override fun evaluate() {
            assumeTrue("screenshot tests run on the debug variant", BuildConfig.DEBUG)
            System.setProperty("roborazzi.test.record", "true")
            base.evaluate()
        }
    }
}

@Composable
private fun currentLayout() = adaptiveLayoutFor(LocalConfiguration.current.screenWidthDp)

@Composable
private fun NavigationFixture() {
    val entries = listOf(
        MobileNavEntry("mine", "האירועים שלי"),
        MobileNavEntry("my_shifts", "המשמרות שלי"),
        MobileNavEntry("users", "אירועי היחידה"),
        MobileNavEntry("contacts", "אנשי קשר"),
    )
    AdaptiveNavigationScaffold(
        layout = currentLayout(),
        entries = entries,
        selectedView = "mine",
        showMore = true,
        onSelectView = {},
        onOpenMore = {},
        floatingActionButton = {},
    ) {
        Box(Modifier.fillMaxSize().background(FieldTheme.page)) { Text("תוכן") }
    }
}

@Composable
private fun EventsFixture() {
    UnitEventsContent(
        ui = UnitEventsFixtures.leadState(),
        actions = UnitEventsFixtures.noOpActions,
        today = UnitEventsFixtures.TODAY,
        layout = currentLayout(),
    )
}

@Composable
private fun FormFixture() {
    AdaptiveContentFrame(layout = currentLayout()) {
        Box(Modifier.fillMaxWidth().height(200.dp).background(FieldTheme.raised).testTag(CAPPED_CONTENT_TAG))
    }
}
