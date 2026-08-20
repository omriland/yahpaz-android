package com.yahpz.domain

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

const val PRIVACY_PATH = "/privacy"
const val PRIVACY_TOKEN_TTL_SEC = 15 * 60
const val PRIVACY_TOKEN_PURPOSE = "privacy-v1"
private const val CLOCK_SKEW_SEC = 60

fun createPrivacyPageToken(
    secret: String,
    nowSec: Long,
    ttlSec: Int = PRIVACY_TOKEN_TTL_SEC,
): String {
    val exp = nowSec + ttlSec
    val sig = hmacSha256Hex(secret, "$PRIVACY_TOKEN_PURPOSE.$exp")
    return "$exp.$sig"
}

fun verifyPrivacyPageToken(
    secret: String,
    token: String,
    nowSec: Long,
    ttlSec: Int = PRIVACY_TOKEN_TTL_SEC,
): Boolean {
    val parsed = parsePrivacyToken(token) ?: return false
    if (nowSec > parsed.exp + CLOCK_SKEW_SEC) return false
    if (parsed.exp > nowSec + ttlSec + CLOCK_SKEW_SEC) return false
    val expected = hmacSha256Hex(secret, "$PRIVACY_TOKEN_PURPOSE.${parsed.exp}")
    return timingSafeEqualHex(parsed.sig, expected)
}

fun buildPrivacyPolicyUrl(origin: String, token: String): String {
    val base = origin.trimEnd('/')
    return "$base$PRIVACY_PATH?t=$token"
}

private data class PrivacyTokenParts(val exp: Long, val sig: String)

private fun parsePrivacyToken(token: String): PrivacyTokenParts? {
    val trimmed = token.trim()
    val dot = trimmed.indexOf('.')
    if (dot <= 0 || dot == trimmed.lastIndex) return null
    val expRaw = trimmed.substring(0, dot)
    val sig = trimmed.substring(dot + 1).lowercase()
    if (!expRaw.matches(Regex("^\\d{10,12}$"))) return null
    if (!sig.matches(Regex("^[0-9a-f]{64}$"))) return null
    val exp = expRaw.toLongOrNull() ?: return null
    return PrivacyTokenParts(exp, sig)
}

private fun hmacSha256Hex(secret: String, message: String): String {
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
    return mac.doFinal(message.toByteArray(Charsets.UTF_8)).joinToString("") { byte ->
        "%02x".format(byte.toInt() and 0xFF)
    }
}

private fun timingSafeEqualHex(left: String, right: String): Boolean {
    if (left.length != right.length) return false
    var diff = 0
    for (i in left.indices) {
        diff = diff or (left[i].code xor right[i].code)
    }
    return diff == 0
}
