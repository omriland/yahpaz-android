package com.yahpz.domain

import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

const val PLATE_LOOKUP_RESOURCE_ID = "053cea08-09bc-40ec-8f7a-156f0677aff3"

data class PlateLookupHit(
    val model: String?,
    val color: String?,
    val manufacturer: String? = null,
)

/** Strips non-digits then parses as Int (leading zeros drop via Int). */
fun plateLookupMispar(plate: String): Int {
    val digits = plate.filter { it.isDigit() }
    return digits.toIntOrNull() ?: 0
}

fun plateLookupUrl(plate: String): String {
    val filters = """{"mispar_rechev":${plateLookupMispar(plate)}}"""
    val encodedFilters = URLEncoder.encode(filters, StandardCharsets.UTF_8)
    return "https://data.gov.il/api/3/action/datastore_search?" +
        "resource_id=$PLATE_LOOKUP_RESOURCE_ID" +
        "&filters=$encodedFilters" +
        "&fields=tzeva_rechev,kinuy_mishari,tozeret_nm" +
        "&limit=1"
}

fun parsePlateLookupBody(body: String): PlateLookupHit? {
    val trimmedStart = body.trimStart()
    if (!trimmedStart.startsWith("{")) return null
    return try {
        val root = simpleJsonObject(body) ?: return null
        val result = root["result"] as? Map<*, *> ?: return null
        val records = result["records"] as? List<*> ?: return null
        val row = records.firstOrNull() as? Map<*, *> ?: return null
        val modelRaw = (row["kinuy_mishari"] as? String)?.trim().orEmpty()
        val colorRaw = (row["tzeva_rechev"] as? String)?.trim().orEmpty()
        val manufacturerRaw = (row["tozeret_nm"] as? String)?.trim().orEmpty()
        PlateLookupHit(
            model = modelRaw.ifEmpty { null },
            color = colorRaw.ifEmpty { null },
            manufacturer = manufacturerRaw.ifEmpty { null },
        )
    } catch (_: Exception) {
        null
    }
}

fun lookupPlate(plate: String): PlateLookupHit? {
    return try {
        val connection = (URI(plateLookupUrl(plate)).toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 8_000
        }
        connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { reader ->
            parsePlateLookupBody(reader.readText())
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * Minimal JSON object/array parser for the plate-lookup payload.
 * Avoids adding a JSON dependency to the pure `:domain` JVM module.
 */
@Suppress("UNCHECKED_CAST")
private fun simpleJsonObject(raw: String): Map<String, Any?>? {
    val parser = SimpleJsonParser(raw)
    val value = parser.parseValue()
    return value as? Map<String, Any?>
}

private class SimpleJsonParser(private val source: String) {
    private var index = 0

    fun parseValue(): Any? {
        skipWhitespace()
        if (index >= source.length) return null
        return when (source[index]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> parseString()
            't' -> parseLiteral("true", true)
            'f' -> parseLiteral("false", false)
            'n' -> parseLiteral("null", null)
            '-', in '0'..'9' -> parseNumber()
            else -> null
        }
    }

    private fun parseObject(): Map<String, Any?> {
        expect('{')
        val out = linkedMapOf<String, Any?>()
        skipWhitespace()
        if (peek() == '}') {
            index++
            return out
        }
        while (true) {
            skipWhitespace()
            val key = parseString()
            skipWhitespace()
            expect(':')
            val value = parseValue()
            out[key] = value
            skipWhitespace()
            when (peek()) {
                ',' -> index++
                '}' -> {
                    index++
                    return out
                }
                else -> return out
            }
        }
    }

    private fun parseArray(): List<Any?> {
        expect('[')
        val out = mutableListOf<Any?>()
        skipWhitespace()
        if (peek() == ']') {
            index++
            return out
        }
        while (true) {
            out += parseValue()
            skipWhitespace()
            when (peek()) {
                ',' -> index++
                ']' -> {
                    index++
                    return out
                }
                else -> return out
            }
        }
    }

    private fun parseString(): String {
        expect('"')
        val sb = StringBuilder()
        while (index < source.length) {
            val ch = source[index++]
            when (ch) {
                '"' -> return sb.toString()
                '\\' -> {
                    if (index >= source.length) break
                    when (val esc = source[index++]) {
                        '"', '\\', '/' -> sb.append(esc)
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000c')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            if (index + 4 > source.length) break
                            val hex = source.substring(index, index + 4)
                            sb.append(hex.toInt(16).toChar())
                            index += 4
                        }
                        else -> sb.append(esc)
                    }
                }
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun parseNumber(): Number {
        val start = index
        if (peek() == '-') index++
        while (peek() in '0'..'9') index++
        var isDouble = false
        if (peek() == '.') {
            isDouble = true
            index++
            while (peek() in '0'..'9') index++
        }
        if (peek() == 'e' || peek() == 'E') {
            isDouble = true
            index++
            if (peek() == '+' || peek() == '-') index++
            while (peek() in '0'..'9') index++
        }
        val raw = source.substring(start, index)
        return if (isDouble) raw.toDouble() else raw.toLong().let { if (it in Int.MIN_VALUE..Int.MAX_VALUE) it.toInt() else it }
    }

    private fun parseLiteral(literal: String, value: Any?): Any? {
        if (!source.startsWith(literal, index)) return null
        index += literal.length
        return value
    }

    private fun expect(ch: Char) {
        skipWhitespace()
        if (peek() == ch) index++
    }

    private fun peek(): Char? = source.getOrNull(index)

    private fun skipWhitespace() {
        while (peek()?.isWhitespace() == true) index++
    }
}
