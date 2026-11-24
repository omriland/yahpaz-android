package com.yahpz.domain

/** Material window width classes: under 600dp, 600 to 839dp, 840dp and up. */
enum class WidthClass { COMPACT, MEDIUM, EXPANDED }

enum class NavigationStyle { BOTTOM_BAR, RAIL }

data class AdaptiveLayout(
    val widthClass: WidthClass,
    val navigation: NavigationStyle,
    /** Open a selected item beside its list instead of in a sheet. */
    val listDetail: Boolean,
    /** Widest a form or card grows. Null means it fills the screen. */
    val contentMaxWidthDp: Int?,
)

private const val MEDIUM_MIN_WIDTH_DP = 600
private const val EXPANDED_MIN_WIDTH_DP = 840

const val CONTENT_MAX_WIDTH_DP = 600
const val SHEET_MAX_WIDTH_DP = 560
const val LIST_PANE_WIDTH_DP = 360

const val LIST_DETAIL_EMPTY_HINT = "בחרו פריט מהרשימה כדי לראות את הפרטים"

fun widthClassFor(widthDp: Int): WidthClass = when {
    widthDp >= EXPANDED_MIN_WIDTH_DP -> WidthClass.EXPANDED
    widthDp >= MEDIUM_MIN_WIDTH_DP -> WidthClass.MEDIUM
    else -> WidthClass.COMPACT
}

fun adaptiveLayoutFor(widthDp: Int): AdaptiveLayout {
    val widthClass = widthClassFor(widthDp)
    val isCompact = widthClass == WidthClass.COMPACT
    return AdaptiveLayout(
        widthClass = widthClass,
        navigation = if (isCompact) NavigationStyle.BOTTOM_BAR else NavigationStyle.RAIL,
        listDetail = widthClass == WidthClass.EXPANDED,
        contentMaxWidthDp = if (isCompact) null else CONTENT_MAX_WIDTH_DP,
    )
}
