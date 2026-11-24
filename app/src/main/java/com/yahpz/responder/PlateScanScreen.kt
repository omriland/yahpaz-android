package com.yahpz.responder

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.yahpz.domain.CameraPermissionAction
import com.yahpz.domain.OPEN_SETTINGS_LABEL
import com.yahpz.domain.PlateScanConfirmState
import com.yahpz.domain.cameraPermissionAction
import com.yahpz.domain.advancePlateScanConfirm
import com.yahpz.domain.extractIsraeliPlateCandidates
import com.yahpz.domain.formatPlate
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ExperimentalPlateScanDialog(
    onDismiss: () -> Unit,
    onPlateScanned: (String) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        ExperimentalPlateScanScreen(
            onClose = onDismiss,
            onPlateScanned = onPlateScanned,
        )
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun ExperimentalPlateScanScreen(
    onClose: () -> Unit,
    onPlateScanned: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var previewHint by remember { mutableStateOf<String?>(null) }
    var statusText by remember { mutableStateOf(SCAN_READY_TEXT) }
    val confirmState = remember { AtomicReference(PlateScanConfirmState()) }
    val delivered = remember { AtomicBoolean(false) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    var cameraAlreadyAsked by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
        cameraAlreadyAsked = true
        if (!granted) statusText = "נדרשת הרשאת מצלמה לסריקה"
    }

    fun cameraPermissionGranted(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    fun currentCameraAction(): CameraPermissionAction {
        val canStillAsk = context.findActivity()?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
        } ?: true
        return cameraPermissionAction(hasCameraPermission, canStillAsk, cameraAlreadyAsked)
    }

    fun onCameraButton() {
        when (currentCameraAction()) {
            CameraPermissionAction.OPEN_SETTINGS -> context.startActivity(appSettingsIntent(context.packageName))
            CameraPermissionAction.REQUEST -> permissionLauncher.launch(Manifest.permission.CAMERA)
            CameraPermissionAction.NONE -> Unit
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    BackHandler(onBack = onClose)

    // The user may grant the camera in app settings and come back.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && !hasCameraPermission && cameraPermissionGranted()) {
                hasCameraPermission = true
                statusText = SCAN_READY_TEXT
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
            recognizer.close()
            runCatching {
                ProcessCameraProvider.getInstance(context).get().unbindAll()
            }
        }
    }

    LaunchedEffect(hasCameraPermission) {
        if (!hasCameraPermission) return@LaunchedEffect
        val cameraProvider = withContext(Dispatchers.IO) {
            ProcessCameraProvider.getInstance(context).get()
        }
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(1280, 720),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                        ),
                    )
                    .build(),
            )
            .build()
        val busy = AtomicBoolean(false)
        analysis.setAnalyzer(analysisExecutor) { imageProxy ->
            if (delivered.get() || !busy.compareAndSet(false, true)) {
                imageProxy.close()
                return@setAnalyzer
            }
            val mediaImage = imageProxy.image
            if (mediaImage == null) {
                busy.set(false)
                imageProxy.close()
                return@setAnalyzer
            }
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees,
            )
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    if (delivered.get()) return@addOnSuccessListener
                    val top = extractIsraeliPlateCandidates(visionText.text).firstOrNull()
                    val (nextState, confirmed) = advancePlateScanConfirm(confirmState.get(), top)
                    confirmState.set(nextState)
                    mainExecutor.execute {
                        previewHint = top?.let { formatPlate(it) }
                        statusText = when {
                            confirmed != null -> "מזהה ${formatPlate(confirmed)}…"
                            top != null -> "מזהה ${formatPlate(top)}…"
                            else -> "כוונו את המצלמה ללוחית"
                        }
                    }
                    if (confirmed != null && delivered.compareAndSet(false, true)) {
                        mainExecutor.execute { onPlateScanned(confirmed) }
                    }
                }
                .addOnCompleteListener {
                    busy.set(false)
                    imageProxy.close()
                }
        }
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis,
            )
        } catch (_: Exception) {
            statusText = "לא ניתן לפתוח את המצלמה"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize(),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "סריקה ניסיונית",
                    style = TypeScale.label,
                    color = Color.White,
                    modifier = Modifier
                        .background(Color(0x99000000), RoundedCornerShape(4.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.background(Color(0x99000000), RoundedCornerShape(4.dp)),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "סגירה",
                        tint = Color.White,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .border(2.dp, FieldTheme.accent, RoundedCornerShape(8.dp)),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xCC000000), RoundedCornerShape(8.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(statusText, style = TypeScale.bodyStrong, color = Color.White)
                previewHint?.let {
                    Text(
                        text = it,
                        style = TypeScale.numeric,
                        color = FieldTheme.accent,
                    )
                }
                if (!hasCameraPermission) {
                    val cameraButtonLabel = if (currentCameraAction() == CameraPermissionAction.OPEN_SETTINGS) {
                        OPEN_SETTINGS_LABEL
                    } else {
                        "אפשרו מצלמה"
                    }
                    TextButton(onClick = ::onCameraButton) {
                        Text(cameraButtonLabel, color = FieldTheme.accent, style = TypeScale.bodyStrong)
                    }
                } else {
                    TextButton(onClick = onClose) {
                        Text("הקלדה ידנית", color = Color.White, style = TypeScale.body)
                    }
                }
            }
        }
    }
}

private const val SCAN_READY_TEXT = "כוונו את המצלמה ללוחית"

fun appSettingsIntent(packageName: String): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
