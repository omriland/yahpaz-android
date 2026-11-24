package com.yahpz.responder

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.yahpz.domain.AdaptiveLayout
import com.yahpz.domain.MOBILE_MORE_LABEL
import com.yahpz.domain.MobileNavEntry
import com.yahpz.domain.NavigationStyle

internal const val BOTTOM_BAR_TAG = "bottom-bar"
internal const val NAVIGATION_RAIL_TAG = "navigation-rail"

/**
 * Main navigation: bottom bar on phones, a rail on medium and expanded widths.
 * Floating buttons stay inside the content area, so they never sit under the rail.
 */
@Composable
internal fun AdaptiveNavigationScaffold(
    layout: AdaptiveLayout,
    entries: List<MobileNavEntry>,
    selectedView: String?,
    showMore: Boolean,
    onSelectView: (String) -> Unit,
    onOpenMore: () -> Unit,
    floatingActionButton: @Composable () -> Unit,
    moreSelected: Boolean = false,
    content: @Composable () -> Unit,
) {
    when (layout.navigation) {
        NavigationStyle.BOTTOM_BAR -> Scaffold(
            containerColor = FieldTheme.page,
            floatingActionButton = floatingActionButton,
            bottomBar = {
                MainBottomBar(entries, selectedView, showMore, moreSelected, onSelectView, onOpenMore)
            },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) { content() }
        }
        NavigationStyle.RAIL -> Row(Modifier.fillMaxSize()) {
            MainRail(entries, selectedView, showMore, moreSelected, onSelectView, onOpenMore)
            Scaffold(
                containerColor = FieldTheme.page,
                floatingActionButton = floatingActionButton,
                modifier = Modifier.weight(1f),
            ) { padding ->
                Box(Modifier.padding(padding).fillMaxSize()) { content() }
            }
        }
    }
}

@Composable
private fun MainBottomBar(
    entries: List<MobileNavEntry>,
    selectedView: String?,
    showMore: Boolean,
    moreSelected: Boolean,
    onSelectView: (String) -> Unit,
    onOpenMore: () -> Unit,
) {
    NavigationBar(
        containerColor = FieldTheme.raised,
        tonalElevation = 0.dp,
        modifier = Modifier.testTag(BOTTOM_BAR_TAG),
    ) {
        entries.forEach { entry ->
            NavigationBarItem(
                selected = entry.view == selectedView,
                onClick = { onSelectView(entry.view) },
                icon = { Icon(iconForView(entry.view), contentDescription = entry.label) },
                label = { Text(entry.label, style = TypeScale.caption) },
                colors = navItemColors,
                modifier = Modifier.heightIn(min = 44.dp),
            )
        }
        if (showMore) {
            NavigationBarItem(
                selected = moreSelected,
                onClick = onOpenMore,
                icon = { Icon(Icons.Outlined.MoreHoriz, contentDescription = MOBILE_MORE_LABEL) },
                label = { Text(MOBILE_MORE_LABEL, style = TypeScale.caption) },
                colors = navItemColors,
                modifier = Modifier.heightIn(min = 44.dp),
            )
        }
    }
}

@Composable
private fun MainRail(
    entries: List<MobileNavEntry>,
    selectedView: String?,
    showMore: Boolean,
    moreSelected: Boolean,
    onSelectView: (String) -> Unit,
    onOpenMore: () -> Unit,
) {
    NavigationRail(
        containerColor = FieldTheme.raised,
        modifier = Modifier.testTag(NAVIGATION_RAIL_TAG),
    ) {
        entries.forEach { entry ->
            NavigationRailItem(
                selected = entry.view == selectedView,
                onClick = { onSelectView(entry.view) },
                icon = { Icon(iconForView(entry.view), contentDescription = entry.label) },
                label = { Text(entry.label, style = TypeScale.caption) },
                colors = railItemColors,
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
        if (showMore) {
            NavigationRailItem(
                selected = moreSelected,
                onClick = onOpenMore,
                icon = { Icon(Icons.Outlined.MoreHoriz, contentDescription = MOBILE_MORE_LABEL) },
                label = { Text(MOBILE_MORE_LABEL, style = TypeScale.caption) },
                colors = railItemColors,
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
    }
}

private val railItemColors
    @Composable get() = NavigationRailItemDefaults.colors(
        selectedIconColor = FieldTheme.accent,
        selectedTextColor = FieldTheme.accent,
        indicatorColor = FieldTheme.accentSubtle,
        unselectedIconColor = FieldTheme.textMuted,
        unselectedTextColor = FieldTheme.textMuted,
    )
