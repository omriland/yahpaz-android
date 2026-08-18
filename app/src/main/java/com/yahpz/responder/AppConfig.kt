package com.yahpz.responder

object AppConfig {
    const val supabaseUrl = "https://rtvizpsfvtjowbimugns.supabase.co"
    const val supabaseAnonKey =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJ0dml6cHNmdnRqb3diaW11Z25zIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODYyNjMyMTksImV4cCI6MjEwMTgzOTIxOX0.e25DgGY5UraIRIqKq15e7aJji-7cwhcl7mEiixMmV64"
    const val appOrigin = "https://yahpz.com"
    const val passwordResetRedirect = "https://yahpz.com/?set_password=1"
    const val appVersionUrl = "https://yahpz.com/android/version.json"
    /** Last-resort fallback if version.json is missing apkUrl. Prefer the versioned URL from the manifest. */
    const val defaultApkUrl = "https://yahpz.com/android/yahpaz-0.1.3.apk"
}
