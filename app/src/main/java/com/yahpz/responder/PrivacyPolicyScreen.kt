package com.yahpz.responder

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

fun isPrivacyPolicyUrl(url: String): Boolean {
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
    val host = uri.host?.removePrefix("www.") ?: return false
    if (host != "yahpz.com") return false
    return uri.path?.trimEnd('/') == "/privacy"
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PrivacyPolicyScreen(onClose: () -> Unit) {
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }
    BackHandler { onClose() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.page)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("מדיניות פרטיות", style = TypeScale.title, color = FieldTheme.textPrimary)
            TextButton(onClick = onClose, modifier = Modifier.heightIn(min = 44.dp)) {
                Text("סגירה", style = TypeScale.bodyStrong, color = FieldTheme.accent)
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                key(reloadKey) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        loading = true
                                        failed = false
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        loading = false
                                    }

                                    override fun onReceivedError(
                                        view: WebView,
                                        request: WebResourceRequest,
                                        error: WebResourceError,
                                    ) {
                                        if (request.isForMainFrame) {
                                            loading = false
                                            failed = true
                                        }
                                    }

                                    override fun shouldOverrideUrlLoading(
                                        view: WebView,
                                        request: WebResourceRequest,
                                    ): Boolean {
                                        val target = request.url ?: return false
                                        val scheme = target.scheme.orEmpty()
                                        if (scheme == "http" || scheme == "https") return false
                                        return runCatching {
                                            view.context.startActivity(Intent(Intent.ACTION_VIEW, target))
                                            true
                                        }.getOrDefault(true)
                                    }
                                }
                                loadUrl(AppConfig.privacyUrl)
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            if (loading && !failed) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = FieldTheme.accent,
                )
            }
            if (failed) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "טעינת מדיניות הפרטיות נכשלה.",
                        style = TypeScale.body,
                        color = FieldTheme.textPrimary,
                    )
                    PrimaryButton(
                        title = "ניסיון נוסף",
                        onClick = {
                            failed = false
                            loading = true
                            reloadKey += 1
                        },
                    )
                    GhostButton(title = "סגירה", onClick = onClose)
                }
            }
        }
    }
}
