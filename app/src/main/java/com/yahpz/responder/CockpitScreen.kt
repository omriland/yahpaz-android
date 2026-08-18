package com.yahpz.responder

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yahpz.domain.COCKPIT_CAPTION
import com.yahpz.domain.COCKPIT_EMPTY
import com.yahpz.domain.COCKPIT_LOAD_FAILED
import com.yahpz.domain.COCKPIT_LOAD_FAILED_CAPTION
import com.yahpz.domain.COCKPIT_MAPS_FAILED
import com.yahpz.domain.COCKPIT_NO_LOCATION
import com.yahpz.domain.COCKPIT_NO_RESULTS
import com.yahpz.domain.COCKPIT_OPEN_MAPS
import com.yahpz.domain.COCKPIT_SEARCH_PLACEHOLDER
import com.yahpz.domain.COCKPIT_TITLE
import com.yahpz.domain.StampTone
import com.yahpz.domain.cockpitLeadDisplay
import com.yahpz.domain.cockpitMapsOpenUris
import com.yahpz.domain.cockpitOwnParticipation
import com.yahpz.domain.cockpitReelDetail
import com.yahpz.domain.cockpitReelLead
import com.yahpz.domain.cockpitReelPlace
import com.yahpz.domain.cockpitReelTitle
import com.yahpz.domain.cockpitResponderSummary
import com.yahpz.domain.cockpitWindowCountLabel
import com.yahpz.domain.eventStamp
import com.yahpz.domain.filterCockpitEventsByQuery
import com.yahpz.domain.formatCockpitAge
import com.yahpz.domain.formatCockpitClock
import com.yahpz.domain.mineFillCtaLabel
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CockpitScreen(app: AppModel, ui: AppUiState, onBack: () -> Unit) {
    val context = LocalContext.current
    var events by remember { mutableStateOf<List<CockpitEventListItem>?>(null) }
    var failed by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var refreshing by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf<CockpitEventListItem?>(null) }
    var mapsError by remember { mutableStateOf<String?>(null) }
    var now by remember { mutableStateOf(Instant.now()) }

    BackHandler(onBack = onBack)

    LaunchedEffect(reloadKey) {
        failed = false
        runCatching { YahpazAPI.fetchCockpitEvents() }
            .onSuccess {
                events = it
                now = Instant.now()
                failed = false
            }
            .onFailure {
                failed = true
                if (events == null) events = emptyList()
            }
        refreshing = false
    }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(15_000)
            now = Instant.now()
        }
    }

    val inputs = events.orEmpty().map { it.asInput }
    val filteredInputs = filterCockpitEventsByQuery(inputs, query)
    val byId = events.orEmpty().associateBy { it.id }
    val filtered = filteredInputs.mapNotNull { byId[it.id] }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            refreshing = true
            reloadKey += 1
        },
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.page),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ToolsBackRow(COCKPIT_TITLE, onBack)
            Text(COCKPIT_CAPTION, style = TypeScale.caption, color = FieldTheme.textMuted)
            SearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = COCKPIT_SEARCH_PLACEHOLDER,
            )
            when {
                events == null && !failed -> LoadingBlock("טוען את הקוקפיט…")
                failed && events.isNullOrEmpty() -> EmptyState(
                    title = COCKPIT_LOAD_FAILED,
                    caption = COCKPIT_LOAD_FAILED_CAPTION,
                    actionTitle = "רענון",
                    onAction = { reloadKey += 1 },
                )
                filtered.isEmpty() -> EmptyState(
                    title = when {
                        failed -> COCKPIT_LOAD_FAILED
                        query.trim().isNotEmpty() -> COCKPIT_NO_RESULTS
                        else -> COCKPIT_EMPTY
                    },
                    caption = if (failed) COCKPIT_LOAD_FAILED_CAPTION else null,
                    actionTitle = when {
                        failed -> "רענון"
                        query.trim().isNotEmpty() -> "ניקוי חיפוש"
                        else -> null
                    },
                    onAction = when {
                        failed -> ({ reloadKey += 1 })
                        query.trim().isNotEmpty() -> ({ query = "" })
                        else -> null
                    },
                )
                else -> Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        cockpitWindowCountLabel(filtered.size),
                        style = TypeScale.caption,
                        color = FieldTheme.textMuted,
                    )
                    filtered.forEach { event ->
                        CockpitEventRow(event, now) { detail = event }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    detail?.let { opened ->
        val current = events.orEmpty().firstOrNull { it.id == opened.id } ?: opened
        val input = current.asInput
        val stamp = eventStamp(current.status)
        val lead = cockpitReelLead(current.shiftLead?.fullName, current.shiftLead?.callsign)
        val mine = cockpitOwnParticipation(input.responders, ui.userId)
        val mapUris = cockpitMapsOpenUris(
            current.locationLat,
            current.locationLng,
            current.road?.name,
            current.location,
        )
        ModalBottomSheet(onDismissRequest = {
            detail = null
            mapsError = null
        }) {
            Column(
                Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        cockpitReelTitle(current.policeEventId),
                        style = TypeScale.section,
                        color = FieldTheme.textPrimary,
                    )
                    StampChip(stamp)
                }
                LedgerRow("שעה", formatCockpitClock(current.createdAt))
                LedgerRow("גיל", formatCockpitAge(current.createdAt, now))
                LedgerRow(
                    "פרטים",
                    cockpitReelDetail(
                        current.eventType?.name,
                        current.road?.name,
                        current.location,
                    ).orEmpty(),
                )
                LedgerRow("מיקום", cockpitReelPlace(current.road?.name, current.location).orEmpty())
                LedgerRow("אחמ״ש", cockpitLeadDisplay(lead))
                LedgerRow("כוננים", cockpitResponderSummary(input.responders))
                if (current.locationLat != null && current.locationLng != null) {
                    LedgerRow(
                        "קואורדינטות",
                        "${current.locationLat}, ${current.locationLng}",
                    )
                }
                mapsError?.let {
                    Text(it, style = TypeScale.caption, color = FieldTheme.alert)
                }
                GhostButton(
                    title = COCKPIT_OPEN_MAPS,
                    onClick = {
                        mapsError = null
                        if (mapUris.isEmpty()) {
                            mapsError = COCKPIT_NO_LOCATION
                            app.showToast(COCKPIT_NO_LOCATION, StampTone.PENDING)
                        } else {
                            val error = openExternalMaps(context, mapUris)
                            if (error != null) {
                                mapsError = error
                                app.showToast(error, StampTone.PENDING)
                            }
                        }
                    },
                )
                mine?.let { status ->
                    mineFillCtaLabel(status)?.let { label ->
                        PrimaryButton(title = label, onClick = {
                            val id = current.id
                            detail = null
                            app.openFill(id)
                        })
                    }
                }
                if (ui.canManageUnit) {
                    PrimaryButton(
                        title = "עריכה",
                        onClick = {
                            val id = current.id
                            detail = null
                            app.openEditEvent(id)
                        },
                    )
                }
                TextButton(
                    onClick = {
                        detail = null
                        mapsError = null
                    },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("סגירה", color = FieldTheme.accent)
                }
            }
        }
    }
}

@Composable
private fun CockpitEventRow(
    event: CockpitEventListItem,
    now: Instant,
    onOpen: () -> Unit,
) {
    val stamp = eventStamp(event.status)
    val lead = cockpitReelLead(event.shiftLead?.fullName, event.shiftLead?.callsign)
    FieldCard(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clickable(onClick = onOpen),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    cockpitReelTitle(event.policeEventId),
                    style = TypeScale.section,
                    color = FieldTheme.textPrimary,
                )
                cockpitReelDetail(
                    event.eventType?.name,
                    event.road?.name,
                    event.location,
                )?.let { detail ->
                    Text(detail, style = TypeScale.caption, color = FieldTheme.textMuted)
                }
                lead?.let {
                    Text(
                        "אחמ״ש: ${cockpitLeadDisplay(it)}",
                        style = TypeScale.caption,
                        color = FieldTheme.textMuted,
                    )
                }
                Text(
                    cockpitResponderSummary(event.responders.map { it.asInput }),
                    style = TypeScale.caption,
                    color = FieldTheme.textSecondary,
                )
                Text(
                    formatCockpitAge(event.createdAt, now),
                    style = TypeScale.caption,
                    color = FieldTheme.textMuted,
                )
            }
            StampChip(stamp)
        }
    }
}

private fun openExternalMaps(context: Context, uris: List<String>): String? {
    for (uri in uris) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
            return null
        } catch (_: ActivityNotFoundException) {
            // try next scheme
        }
    }
    return COCKPIT_MAPS_FAILED
}
