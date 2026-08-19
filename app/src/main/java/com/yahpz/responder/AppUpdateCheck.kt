package com.yahpz.responder

import com.yahpz.domain.needsForceUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class AppVersionManifest(
    val minVersionCode: Int,
    val latestVersionCode: Int = minVersionCode,
    val latestVersionName: String = "",
    val apkUrl: String = "",
    val messageHe: String = DEFAULT_FORCE_UPDATE_MESSAGE,
)

data class ForceUpdateRequired(
    val messageHe: String,
    val apkUrl: String,
)

private val versionJson = Json { ignoreUnknownKeys = true }

const val DEFAULT_FORCE_UPDATE_MESSAGE =
    "יש גרסה חדשה של האפליקציה. יש להוריד ולהתקין כדי להמשיך."

suspend fun checkForceUpdate(currentVersionCode: Int): ForceUpdateRequired? =
    withContext(Dispatchers.IO) {
        val manifest = fetchPreferredAppVersionManifest() ?: return@withContext null
        if (!needsForceUpdate(currentVersionCode, manifest.minVersionCode)) return@withContext null
        ForceUpdateRequired(
            messageHe = manifest.messageHe.ifBlank { DEFAULT_FORCE_UPDATE_MESSAGE },
            apkUrl = manifest.apkUrl.ifBlank { AppConfig.defaultApkUrl },
        )
    }

/** Prefer the feed with the highest minVersionCode among reachable URLs. */
internal fun fetchPreferredAppVersionManifest(
    urls: List<String> = listOf(AppConfig.appVersionUrl, AppConfig.appVersionUrlFallback),
): AppVersionManifest? =
    urls.mapNotNull { fetchAppVersionManifest(it) }
        .maxByOrNull { it.minVersionCode }

private fun fetchAppVersionManifest(url: String): AppVersionManifest? =
    runCatching {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 5_000
            requestMethod = "GET"
            instanceFollowRedirects = true
        }
        try {
            if (connection.responseCode !in 200..299) return null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            versionJson.decodeFromString(AppVersionManifest.serializer(), body)
        } finally {
            connection.disconnect()
        }
    }.getOrNull()
