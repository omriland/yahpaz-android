package com.yahpz.responder

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class NeedsUnknownSourcesException : Exception()

/** Downloads an APK in-process and commits a PackageInstaller session. */
object SideloadApkInstaller {
    suspend fun downloadAndInstall(
        context: Context,
        apkUrl: String,
        onProgress: (Float) -> Unit,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val app = context.applicationContext
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !app.packageManager.canRequestPackageInstalls()
            ) {
                withContext(Dispatchers.Main) {
                    app.startActivity(
                        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                            .setData(Uri.parse("package:${app.packageName}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
                throw NeedsUnknownSourcesException()
            }
            val dest = File(app.cacheDir, "sideload-update.apk")
            downloadTo(apkUrl, dest, onProgress)
            commitInstallSession(app, dest)
        }
    }

    private fun downloadTo(apkUrl: String, dest: File, onProgress: (Float) -> Unit) {
        val connection = (URL(apkUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 120_000
            requestMethod = "GET"
            instanceFollowRedirects = true
        }
        try {
            if (connection.responseCode !in 200..299) {
                error("http ${connection.responseCode}")
            }
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: -1L
            dest.outputStream().use { out ->
                connection.inputStream.use { input ->
                    val buf = ByteArray(DEFAULT_BUFFER_SIZE)
                    var copied = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        copied += n
                        if (total > 0) onProgress((copied.toFloat() / total.toFloat()).coerceIn(0f, 1f))
                    }
                }
            }
            onProgress(1f)
        } finally {
            connection.disconnect()
        }
    }

    private fun commitInstallSession(context: Context, apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }
        val sessionId = installer.createSession(params)
        val session = installer.openSession(sessionId)
        try {
            apk.inputStream().use { input ->
                session.openWrite("base.apk", 0, apk.length()).use { out ->
                    input.copyTo(out)
                    session.fsync(out)
                }
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val pending = PendingIntent.getBroadcast(
                context,
                sessionId,
                Intent(context, ApkInstallReceiver::class.java),
                flags,
            )
            session.commit(pending.intentSender)
        } catch (error: Exception) {
            session.abandon()
            throw error
        } finally {
            session.close()
        }
    }
}
