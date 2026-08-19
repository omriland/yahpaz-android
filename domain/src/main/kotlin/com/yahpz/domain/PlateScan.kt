package com.yahpz.domain

/** OCR confuses these glyphs with digits on Israeli plates. */
private val OCR_DIGIT_MAP = mapOf(
    'O' to '0',
    'o' to '0',
    'D' to '0',
    'Q' to '0',
    'I' to '1',
    'l' to '1',
    '|' to '1',
    '!' to '1',
    'Z' to '2',
    'z' to '2',
    'S' to '5',
    's' to '5',
    'B' to '8',
    'G' to '6',
)

/**
 * Normalize one OCR line into digit-ish characters, keeping separators so
 * grouped plate patterns (12-345-67) stay recoverable.
 */
fun normalizePlateOcrLine(raw: String): String {
    val sb = StringBuilder(raw.length)
    for (ch in raw) {
        when {
            ch.isDigit() -> sb.append(ch)
            ch in OCR_DIGIT_MAP -> sb.append(OCR_DIGIT_MAP.getValue(ch))
            ch == '-' || ch == ' ' || ch == '·' || ch == '.' || ch == ':' || ch == '/' -> sb.append('-')
            else -> Unit
        }
    }
    return sb.toString()
}

/**
 * Extract unique 7- or 8-digit Israeli plate candidates from OCR text.
 * Prefers longer (8-digit) matches when overlapping, returns most-likely-first.
 */
fun extractIsraeliPlateCandidates(ocrText: String): List<String> {
    if (ocrText.isBlank()) return emptyList()
    val scored = linkedMapOf<String, Int>()
    for (line in ocrText.lineSequence()) {
        val normalized = normalizePlateOcrLine(line)
        if (normalized.isEmpty()) continue
        for (match in PLATE_CANDIDATE_REGEX.findAll(normalized)) {
            val digits = match.value.filter { it.isDigit() }
            if (digits.length != 7 && digits.length != 8) continue
            // Prefer candidates that already looked like grouped plates.
            val score = when {
                match.value.contains('-') -> 3
                digits.length == 8 -> 2
                else -> 1
            }
            scored[digits] = maxOf(scored[digits] ?: 0, score)
        }
        // Whole digit runs between separators (OCR often drops dashes on a single run).
        for (run in normalized.split('-')) {
            when (run.length) {
                7 -> scored[run] = maxOf(scored[run] ?: 0, 1)
                8 -> scored[run] = maxOf(scored[run] ?: 0, 2)
            }
        }
        val pure = normalized.filter { it.isDigit() }
        when (pure.length) {
            7 -> scored[pure] = maxOf(scored[pure] ?: 0, 1)
            8 -> scored[pure] = maxOf(scored[pure] ?: 0, 2)
        }
    }
    return scored.entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenByDescending { it.key.length })
        .map { it.key }
        .distinct()
}

private val PLATE_CANDIDATE_REGEX = Regex("""\d(?:[\d-]{5,10})\d""")

data class PlateScanConfirmState(
    val digits: String? = null,
    val streak: Int = 0,
)

/**
 * Require [requiredStreak] consecutive frames with the same top candidate
 * before treating a plate as confirmed (reduces OCR flicker).
 */
fun advancePlateScanConfirm(
    state: PlateScanConfirmState,
    topCandidate: String?,
    requiredStreak: Int = 3,
): Pair<PlateScanConfirmState, String?> {
    val next = plateDigits(topCandidate.orEmpty())
    if (next.length != 7 && next.length != 8) {
        return PlateScanConfirmState() to null
    }
    val streak = if (state.digits == next) state.streak + 1 else 1
    val updated = PlateScanConfirmState(digits = next, streak = streak)
    return if (streak >= requiredStreak) updated to next else updated to null
}
