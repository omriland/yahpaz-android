package com.yahpz.responder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yahpz.domain.AssignableProfile
import com.yahpz.domain.EVENT_ASSIGN_CLOSE
import com.yahpz.domain.LookupOption
import com.yahpz.domain.MAIN_LEAD_LOCKED_HINT
import com.yahpz.domain.SECONDARY_LEAD_ADD
import com.yahpz.domain.SECONDARY_LEAD_LABEL
import com.yahpz.domain.SECONDARY_LEAD_LOCKED_HINT
import com.yahpz.domain.SECONDARY_LEAD_PICKER_EMPTY
import com.yahpz.domain.SECONDARY_LEAD_PICKER_NONE
import com.yahpz.domain.SECONDARY_LEAD_REMOVE
import com.yahpz.domain.SecondaryLead
import com.yahpz.domain.canChangeEventMainLead
import com.yahpz.domain.canManageSecondaryLeads
import com.yahpz.domain.canRemoveSecondaryLead
import com.yahpz.domain.eventLeadFieldLabel
import com.yahpz.domain.filterShiftLeadPicker
import com.yahpz.domain.formatLeadPerson
import com.yahpz.domain.reassignMainLeads

@Composable
fun EventLeadLedgerRows(
    main: PersonName?,
    secondaries: List<EventSecondaryLeadRow>,
) {
    val mapped = secondaries.map { it.asDomain() }
    LedgerRow(
        eventLeadFieldLabel(mapped.isNotEmpty()),
        formatLeadPerson(main?.fullName, main?.callsign).ifEmpty { "—" },
    )
    mapped.forEach { row ->
        LedgerRow(SECONDARY_LEAD_LABEL, row.display.ifEmpty { "—" })
    }
}

@Composable
fun EventShiftLeadsFields(
    roles: List<String>,
    viewerId: String?,
    eventExists: Boolean,
    shiftLeadId: String,
    shiftLeadName: String,
    shiftLeadCallsign: String,
    secondaryLeads: List<SecondaryLead>,
    shiftLeadUsers: List<AssignableProfile>,
    onChange: (mainId: String, mainName: String, mainCallsign: String, secondaries: List<SecondaryLead>) -> Unit,
) {
    val canChangeMain = canChangeEventMainLead(
        roles = roles,
        eventExists = eventExists,
        viewerIsCurrentMain = viewerId != null && viewerId == shiftLeadId,
        hasSecondaries = secondaryLeads.isNotEmpty(),
    )
    val canManage = canManageSecondaryLeads(roles)
    val excludeIds = buildSet {
        add(shiftLeadId)
        addAll(secondaryLeads.map { it.userId })
    }
    val mainOptions = shiftLeadUsers.map { LookupOption(id = it.id, name = it.display) }

    fun emit(
        mainId: String,
        mainName: String,
        mainCallsign: String,
        secondaries: List<SecondaryLead>,
    ) {
        onChange(mainId, mainName, mainCallsign, secondaries)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (canChangeMain) {
            LookupPickerField(
                label = eventLeadFieldLabel(secondaryLeads.isNotEmpty()),
                options = mainOptions,
                selectedId = shiftLeadId,
                onSelect = { nextId ->
                    if (nextId == shiftLeadId) return@LookupPickerField
                    val nextPerson = shiftLeadUsers.firstOrNull { it.id == nextId }
                    val reassigned = reassignMainLeads(
                        previousMainId = shiftLeadId,
                        nextMainId = nextId,
                        previousMainName = shiftLeadName,
                        previousMainCallsign = shiftLeadCallsign,
                        secondaries = secondaryLeads,
                    )
                    emit(
                        reassigned.mainId,
                        nextPerson?.fullName.orEmpty(),
                        nextPerson?.callsign.orEmpty(),
                        reassigned.secondaries,
                    )
                },
                placeholder = "בחירת אחמ״ש",
                searchPlaceholder = "חיפוש אחמ״ש",
            )
        } else {
            LedgerRow(
                eventLeadFieldLabel(secondaryLeads.isNotEmpty()),
                formatLeadPerson(shiftLeadName, shiftLeadCallsign).ifEmpty { "—" },
            )
            if (canManage) {
                Text(MAIN_LEAD_LOCKED_HINT, style = TypeScale.caption, color = FieldTheme.textMuted)
            }
        }

        secondaryLeads.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(SECONDARY_LEAD_LABEL, style = TypeScale.caption, color = FieldTheme.textMuted)
                    Text(
                        row.display.ifEmpty { "—" },
                        style = TypeScale.body,
                        color = FieldTheme.textPrimary,
                    )
                    if (row.locked) {
                        Text(
                            SECONDARY_LEAD_LOCKED_HINT,
                            style = TypeScale.caption,
                            color = FieldTheme.textMuted,
                        )
                    }
                }
                if (canRemoveSecondaryLead(roles, row.locked)) {
                    Text(
                        "הסרה",
                        style = TypeScale.caption,
                        color = FieldTheme.alert,
                        modifier = Modifier
                            .clickable {
                                emit(
                                    shiftLeadId,
                                    shiftLeadName,
                                    shiftLeadCallsign,
                                    secondaryLeads.filter { it.userId != row.userId },
                                )
                            }
                            .padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
                            .semantics { contentDescription = SECONDARY_LEAD_REMOVE },
                    )
                }
            }
        }

        if (canManage) {
            SecondaryLeadPicker(
                people = shiftLeadUsers,
                excludeIds = excludeIds,
                onAdd = { person ->
                    emit(
                        shiftLeadId,
                        shiftLeadName,
                        shiftLeadCallsign,
                        secondaryLeads + SecondaryLead(
                            userId = person.id,
                            locked = false,
                            fullName = person.fullName,
                            callsign = person.callsign,
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun SecondaryLeadPicker(
    people: List<AssignableProfile>,
    excludeIds: Set<String>,
    onAdd: (AssignableProfile) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    GhostButton(
        title = if (open) EVENT_ASSIGN_CLOSE else SECONDARY_LEAD_ADD,
        onClick = {
            if (open) {
                open = false
            } else {
                query = ""
                open = true
            }
        },
    )
    if (!open) return
    val visible = filterShiftLeadPicker(people, excludeIds, query)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FormField(
            label = "חיפוש אחמ״ש",
            value = query,
            onValueChange = { query = it },
        )
        if (people.none { it.id !in excludeIds }) {
            Text(SECONDARY_LEAD_PICKER_EMPTY, style = TypeScale.caption, color = FieldTheme.textMuted)
        } else if (visible.isEmpty()) {
            Text(SECONDARY_LEAD_PICKER_NONE, style = TypeScale.caption, color = FieldTheme.textMuted)
        } else {
            visible.forEach { profile ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .clickable {
                            onAdd(profile)
                            open = false
                            query = ""
                        }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(profile.display, style = TypeScale.body, color = FieldTheme.textPrimary)
                    Text("הוספה", style = TypeScale.caption, color = FieldTheme.accent)
                }
            }
        }
    }
}
