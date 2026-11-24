package com.yahpz.responder

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A short screen, as with the keyboard open: the submit button must be reachable by scrolling. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "he-w411dp-h300dp-420dpi")
class LoginScrollTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun submitButtonIsReachableOnAShortScreen() {
        compose.setContent {
            YahpazTestTheme {
                LoginScreen(
                    busy = false,
                    error = null,
                    email = "a@b.co",
                    password = "secret",
                    mode = LoginMode.SIGNIN,
                    onEmail = {},
                    onPassword = {},
                    onSubmit = {},
                    onToggleMode = {},
                    onOpenPrivacy = {},
                )
            }
        }
        val submit = compose.onNode(hasText("כניסה") and hasClickAction())
        submit.performScrollTo()
        submit.assertIsDisplayed()
    }

    @Test
    fun theFormTitleIsStillShown() {
        compose.setContent {
            YahpazTestTheme {
                LoginScreen(false, null, "", "", LoginMode.SIGNIN, {}, {}, {}, {}, {})
            }
        }
        compose.onNodeWithText("כניסה למערכת").assertIsDisplayed()
    }
}
