package com.yahpz.responder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private val saveShape = RoundedCornerShape(8.dp)
private val noticeShape = RoundedCornerShape(6.dp)

/**
 * Sticky top of the new-event form: חזרה, the title, and the form's main action, so the
 * אחמ״ש can create the event from anywhere in the long form. A validation message from
 * this button shows right under it (the bottom one is out of sight).
 */
@Composable
fun EventCreateHeader(
    title: String,
    saveTitle: String,
    saving: Boolean,
    enabled: Boolean,
    error: String?,
    scrolled: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = if (scrolled) 3.dp else 0.dp)
            .background(FieldTheme.page)
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 36.dp)
                .clip(RoundedCornerShape(4.dp))
                .clickable(role = Role.Button, onClick = onBack)
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = FieldTheme.accent,
                modifier = Modifier.size(20.dp),
            )
            Text("חזרה", style = TypeScale.bodyStrong, color = FieldTheme.accent)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style = TypeScale.title,
                color = FieldTheme.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            val active = enabled && !saving
            Row(
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .clip(saveShape)
                    .background(if (enabled) FieldTheme.accent else FieldTheme.textMuted)
                    .clickable(enabled = active, role = Role.Button, onClick = onSave)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = FieldTheme.textOnAccent,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = null,
                        tint = FieldTheme.textOnAccent,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(saveTitle, style = TypeScale.bodyStrong, color = FieldTheme.textOnAccent)
            }
        }
        if (error != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .clip(noticeShape)
                    .background(FieldTheme.alertTint)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = FieldTheme.alert,
                    modifier = Modifier.size(18.dp),
                )
                Text(error, style = TypeScale.caption, color = FieldTheme.alert)
            }
        }
    }
}
