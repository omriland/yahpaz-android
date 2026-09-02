package com.yahpz.domain

/**
 * Login fields are Latin (email / password). An RTL Compose TextField can inject
 * bidi marks into the stored value so GoTrue sees a different string than web.
 */
private val LOGIN_INVISIBLE = Regex("[\\u200B-\\u200F\\u202A-\\u202E\\u2066-\\u2069\\uFEFF]")

fun normalizeLoginEmail(raw: String): String =
    LOGIN_INVISIBLE.replace(raw, "").trim().lowercase()

fun normalizeLoginSecret(raw: String): String = LOGIN_INVISIBLE.replace(raw, "")
