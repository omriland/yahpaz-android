package com.yahpz.responder

import android.content.Context
import android.os.Build

object PlayInstallSource {
    const val PLAY_STORE_PACKAGE = "com.android.vending"

    fun installedFromPlay(context: Context): Boolean {
        val installer = installerPackage(context) ?: return false
        return installer == PLAY_STORE_PACKAGE
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
