package com.yahpz.responder

object AppConfig {
    const val supabaseUrl = "https://rtvizpsfvtjowbimugns.supabase.co"
    const val supabaseAnonKey =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJ0dml6cHNmdnRqb3diaW11Z25zIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODYyNjMyMTksImV4cCI6MjEwMTgzOTIxOX0.e25DgGY5UraIRIqKq15e7aJji-7cwhcl7mEiixMmV64"
    const val appOrigin = "https://yahpz.com"
    const val privacyUrl = "https://yahpz.com/privacy"
    /** HMAC secret shared with the web `/privacy?t=` gate. Not a user credential. */
    const val privacyPageSecret =
        "7dac9feb0b215b384d9e024eb6be9e7704ada56ef66077b1b03705d3997e5901"
    const val passwordResetRedirect = "https://yahpz.com/?set_password=1"
    const val appVersionUrl = "https://yahpz.com/android/version.json"
    /** Fallback when yahpz.com is stale or unreachable (GitHub-hosted feed). */
    const val appVersionUrlFallback =
        "https://raw.githubusercontent.com/omriland/yahpaz-android/main/site/android/version.json"
    /** Last-resort fallback if version.json is missing apkUrl. Prefer the versioned URL from the manifest. */
    const val defaultApkUrl =
        "https://yahpz.com/android/yahpaz-0.3.5.apk"
}
