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
        app.bootstrap(
            installedFromPlay = PlayInstallSource.installedFromPlay(this),
            skippedOptionalVersionCode = OptionalUpdatePrefs.skippedLatestVersionCode(this),
        )
        intent?.dataString?.let(app::applyIncomingUrl)
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
                    RootScreen(app, ui)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        app.onForeground()
        playUpdate.startIfAvailable()
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
