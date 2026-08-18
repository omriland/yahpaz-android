package com.yahpz.domain

/** Hebrew (and Latin) manufacturer → car-logos-dataset slug. Longest keys first. */

private val COUNTRY_SUFFIXES = listOf(
    "גרמניה",
    "גרמנ",
    "ד.קור",
    "דקור",
    "קוריאה",
    "יפן",
    "סין",
    "צרפת",
    "איטליה",
    "אנגליה",
    "בריטניה",
    "ארהב",
    "ארה\"ב",
    "ארצות הברית",
    "צכיה",
    "צ׳כיה",
    "ספרד",
    "שבדיה",
    "שוודיה",
    "הודו",
    "תאילנד",
    "מקסיקו",
    "טורקיה",
    "רומניה",
    "סלובקיה",
)

/** Longest-first Hebrew / mixed keys. */
private val HEBREW_BRANDS: List<Pair<String, String>> = listOf(
    "אלפא רומיאו" to "alfa-romeo",
    "לנד רובר" to "land-rover",
    "מרצדס בנץ" to "mercedes-benz",
    "מרצדס" to "mercedes-benz",
    "פולקסווגן" to "volkswagen",
    "סאנגיונג" to "ssangyong",
    "מיצובישי" to "mitsubishi",
    "שברולט" to "chevrolet",
    "סיטרואן" to "citroen",
    "פיג׳ו" to "peugeot",
    "פיג'ו" to "peugeot",
    "רנו" to "renault",
    "טויוטה" to "toyota",
    "יונדאי" to "hyundai",
    "ג׳נסיס" to "genesis",
    "ג'נסיס" to "genesis",
    "לקסוס" to "lexus",
    "אינפיניטי" to "infiniti",
    "סובארו" to "subaru",
    "הונדה" to "honda",
    "ניסאן" to "nissan",
    "מאזדה" to "mazda",
    "סוזוקי" to "suzuki",
    "איסוזו" to "isuzu",
    "סקודה" to "skoda",
    "סיאט" to "seat",
    "קופרה" to "cupra",
    "אאודי" to "audi",
    "פורשה" to "porsche",
    "וולוו" to "volvo",
    "יגואר" to "jaguar",
    "פיאט" to "fiat",
    "אופל" to "opel",
    "פורד" to "ford",
    "דאצ׳יה" to "dacia",
    "דאצ'יה" to "dacia",
    "טסלה" to "tesla",
    "קרייזלר" to "chrysler",
    "דודג׳" to "dodge",
    "דודג'" to "dodge",
    "ג׳יפ" to "jeep",
    "ג'יפ" to "jeep",
    "ג׳ילי" to "geely",
    "ג'ילי" to "geely",
    "צ׳רי" to "chery",
    "צ'רי" to "chery",
    "צרי" to "chery",
    "בי.ווי.די" to "byd",
    "ביווידי" to "byd",
    "סמארט" to "smart",
    "מיני" to "mini",
    "אקורה" to "acura",
    "לינקולן" to "lincoln",
    "קדילאק" to "cadillac",
    "קיה" to "kia",
    "ב מ וו" to "bmw",
    "ב.מ.וו" to "bmw",
    "במוו" to "bmw",
)

private val LATIN_BRANDS: List<Pair<String, String>> = listOf(
    "mercedes-benz" to "mercedes-benz",
    "mercedes" to "mercedes-benz",
    "volkswagen" to "volkswagen",
    "ssangyong" to "ssangyong",
    "mitsubishi" to "mitsubishi",
    "chevrolet" to "chevrolet",
    "land rover" to "land-rover",
    "land-rover" to "land-rover",
    "alfa romeo" to "alfa-romeo",
    "alfa-romeo" to "alfa-romeo",
    "citroen" to "citroen",
    "citroën" to "citroen",
    "peugeot" to "peugeot",
    "renault" to "renault",
    "toyota" to "toyota",
    "hyundai" to "hyundai",
    "genesis" to "genesis",
    "lexus" to "lexus",
    "infiniti" to "infiniti",
    "subaru" to "subaru",
    "honda" to "honda",
    "nissan" to "nissan",
    "mazda" to "mazda",
    "suzuki" to "suzuki",
    "isuzu" to "isuzu",
    "skoda" to "skoda",
    "škoda" to "skoda",
    "seat" to "seat",
    "cupra" to "cupra",
    "audi" to "audi",
    "porsche" to "porsche",
    "volvo" to "volvo",
    "jaguar" to "jaguar",
    "fiat" to "fiat",
    "opel" to "opel",
    "ford" to "ford",
    "dacia" to "dacia",
    "tesla" to "tesla",
    "chrysler" to "chrysler",
    "dodge" to "dodge",
    "jeep" to "jeep",
    "geely" to "geely",
    "chery" to "chery",
    "byd" to "byd",
    "smart" to "smart",
    "mini" to "mini",
    "acura" to "acura",
    "lincoln" to "lincoln",
    "cadillac" to "cadillac",
    "kia" to "kia",
    "bmw" to "bmw",
    "mg" to "mg",
)

fun normalizeManufacturer(raw: String): String {
    var value = raw.trim().replace(Regex("\\s+"), " ")
    if (value.isEmpty()) return ""
    var changed = true
    while (changed) {
        changed = false
        for (suffix in COUNTRY_SUFFIXES) {
            val withDot = "$suffix."
            when {
                value == suffix || value == withDot -> {
                    value = ""
                    changed = true
                }
                value.endsWith(" $withDot") -> {
                    value = value.removeSuffix(" $withDot").trim()
                    changed = true
                }
                value.endsWith(" $suffix") -> {
                    value = value.removeSuffix(" $suffix").trim()
                    changed = true
                }
            }
        }
    }
    return value.trimEnd('.', ',').trim()
}

fun resolveCarLogoSlug(manufacturer: String?): String? {
    val normalized = normalizeManufacturer(manufacturer.orEmpty())
    if (normalized.isEmpty()) return null

    for ((key, slug) in HEBREW_BRANDS) {
        if (normalized.contains(key)) return slug
    }

    val lower = normalized.lowercase()
    for ((key, slug) in LATIN_BRANDS) {
        if (lower.contains(key)) return slug
    }

    return null
}
