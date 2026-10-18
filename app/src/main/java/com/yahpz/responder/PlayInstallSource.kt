package com.yahpz.responder

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

object PlayInstallSource {
    const val PLAY_STORE_PACKAGE = "com.android.vending"

    fun installedFromPlay(context: Context): Boolean {
        val installer = installerPackage(context) ?: return false
        return installer == PLAY_STORE_PACKAGE
    }

    fun openPlayStore(context: Context) {
        val pkg = context.packageName
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
                    .setPackage(PLAY_STORE_PACKAGE)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onFailure {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }

    private fun installerPackage(context: Context): String? =
        runCatching {
            val pm = context.packageManager
            val pkg = context.packageName
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(pkg).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(pkg)
            }
        }.getOrNull()
}
