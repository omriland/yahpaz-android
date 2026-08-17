package com.yahpz.responder

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yahpz.domain.StampDescriptor
import com.yahpz.domain.StampTone

@Composable
fun LiveTrackScreen(token: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val tracker = remember { LocationTracker(context.applicationContext) }
    val state by tracker.state.collectAsState()
    val permission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        tracker.onPermissionResult(grants.values.any { it })
    }

    LaunchedEffect(token) {
        if (tracker.hasLocationPermission()) {
            tracker.start(token)
        } else {
            permission.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
            tracker.start(token)
        }
    }
    DisposableEffect(token) {
        onDispose { tracker.stop() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.page)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("שיתוף מיקום", style = TypeScale.title, color = FieldTheme.textPrimary)
        Text(state.statusText, style = TypeScale.body, color = FieldTheme.textSecondary, textAlign = TextAlign.Center)
        if (state.sharing) {
            StampChip(StampDescriptor("בדרך", StampTone.PENDING))
        }
        if (state.ended) {
            Text("אפשר לסגור את המסך.", style = TypeScale.caption, color = FieldTheme.textMuted)
        }
        if (state.failed != null && !state.ended) {
            Text(state.failed!!, style = TypeScale.body, color = FieldTheme.alert, textAlign = TextAlign.Center)
        }
        GhostButton(title = "סגירה", onClick = {
            tracker.stop()
            onClose()
        })
    }
}
