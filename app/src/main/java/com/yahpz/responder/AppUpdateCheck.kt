package com.yahpz.responder

import com.yahpz.domain.needsForceUpdate
import com.yahpz.domain.needsOptionalUpdate
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

data class OptionalUpdateAvailable(
    val messageHe: String,
    val apkUrl: String,
    val latestVersionCode: Int,
    val latestVersionName: String,
)

data class SideloadUpdateCheck(
    val force: ForceUpdateRequired? = null,
    val optional: OptionalUpdateAvailable? = null,
)

private val versionJson = Json { ignoreUnknownKeys = true }

const val DEFAULT_FORCE_UPDATE_MESSAGE =
    "יש גרסה חדשה של האפליקציה. יש להוריד ולהתקין כדי להמשיך."

const val DEFAULT_OPTIONAL_UPDATE_MESSAGE =
    "יש עדכון לאבן דרך. אפשר להתקין עכשיו בלי לפתוח את החנות."

suspend fun checkSideloadUpdates(currentVersionCode: Int): SideloadUpdateCheck =
    withContext(Dispatchers.IO) {
        val manifest = fetchPreferredAppVersionManifest() ?: return@withContext SideloadUpdateCheck()
        val apkUrl = manifest.apkUrl.ifBlank { AppConfig.defaultApkUrl }
        if (needsForceUpdate(currentVersionCode, manifest.minVersionCode)) {
            return@withContext SideloadUpdateCheck(
                force = ForceUpdateRequired(
                    messageHe = manifest.messageHe.ifBlank { DEFAULT_FORCE_UPDATE_MESSAGE },
                    apkUrl = apkUrl,
                ),
            )
        }
        if (needsOptionalUpdate(currentVersionCode, manifest.minVersionCode, manifest.latestVersionCode)) {
            val named = manifest.latestVersionName.trim()
            val message = if (named.isEmpty()) {
                DEFAULT_OPTIONAL_UPDATE_MESSAGE
            } else {
                "יש עדכון לאבן דרך ($named). אפשר להתקין עכשיו בלי לפתוח את החנות."
            }
            return@withContext SideloadUpdateCheck(
                optional = OptionalUpdateAvailable(
                    messageHe = message,
                    apkUrl = apkUrl,
                    latestVersionCode = manifest.latestVersionCode,
                    latestVersionName = named,
                ),
            )
        }
        SideloadUpdateCheck()
    }

suspend fun checkForceUpdate(currentVersionCode: Int): ForceUpdateRequired? =
    checkSideloadUpdates(currentVersionCode).force

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
