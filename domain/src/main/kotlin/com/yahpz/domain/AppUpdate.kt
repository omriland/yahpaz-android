package com.yahpz.domain

/** True when the installed build is below the server minimum (hard force update). */
fun needsForceUpdate(currentVersionCode: Int, minVersionCode: Int): Boolean =
    currentVersionCode < minVersionCode

/** True when the build may keep running but a newer APK is available. */
fun needsOptionalUpdate(
    currentVersionCode: Int,
    minVersionCode: Int,
    latestVersionCode: Int,
): Boolean =
    currentVersionCode >= minVersionCode && currentVersionCode < latestVersionCode
