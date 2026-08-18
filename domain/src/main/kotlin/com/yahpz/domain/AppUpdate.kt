package com.yahpz.domain

/** True when the installed build is below the server minimum (hard force update). */
fun needsForceUpdate(currentVersionCode: Int, minVersionCode: Int): Boolean =
    currentVersionCode < minVersionCode
