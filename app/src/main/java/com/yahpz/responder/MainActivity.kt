package com.yahpz.responder

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

class MainActivity : ComponentActivity() {
    private val app: AppModel by viewModels()
    private val playUpdate by lazy { PlayFlexibleUpdate(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val modelIsFresh = !app.hasBootstrapped
        app.bootstrap(
            installedFromPlay = PlayInstallSource.installedFromPlay(this),
            skippedOptionalVersionCode = OptionalUpdatePrefs.skippedLatestVersionCode(this),
            savedNavigation = savedInstanceState?.getBundle(NAVIGATION_STATE_KEY)?.toStringMap(),
        )
        if (savedInstanceState == null || modelIsFresh) intent?.dataString?.let(app::applyIncomingUrl)
        setContent {
            val ui by app.state.collectAsState()
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
                ) {
                    RootScreen(
                        app = app,
                        ui = ui,
                        onPlayUpdate = {
                            playUpdate.startImmediateUpdate { PlayInstallSource.openPlayStore(this) }
                        },
                    )
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (app.state.value.isSignedIn) {
            outState.putBundle(NAVIGATION_STATE_KEY, app.navigationMap().toBundle())
        }
    }

    override fun onStart() {
        super.onStart()
        app.onForeground()
        if (app.state.value.forceUpdate != null) {
            playUpdate.startImmediateUpdate { PlayInstallSource.openPlayStore(this) }
        } else {
            playUpdate.startIfAvailable()
        }
    }

    override fun onResume() {
        super.onResume()
        playUpdate.resumeIfInProgress()
    }

    override fun onStop() {
        playUpdate.stop()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.dataString?.let(app::applyIncomingUrl)
    }
}

private const val NAVIGATION_STATE_KEY = "navigation_snapshot"

private fun Map<String, String>.toBundle(): Bundle =
    Bundle().also { bundle -> forEach { (key, value) -> bundle.putString(key, value) } }

private fun Bundle.toStringMap(): Map<String, String> =
    keySet().mapNotNull { key -> getString(key)?.let { key to it } }.toMap()
