package com.yahpz.responder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.yahpz.domain.EVENT_CANCELLED_LABEL
import com.yahpz.domain.EVENT_DRAFT_FORM_ERROR
import com.yahpz.domain.EVENT_END_TIME_LABEL
import com.yahpz.domain.EVENT_FORM_DETAILS_SECTION
import com.yahpz.domain.EVENT_FORM_LEADS_SECTION
import com.yahpz.domain.EVENT_NEW_TITLE
import com.yahpz.domain.EVENT_START_TIME_LABEL
import com.yahpz.domain.LookupOption
import com.yahpz.domain.eventFormPrimarySaveTitle

/** `adb shell am start -n com.yahpz.responder/.EventFormPreviewActivity --es state default|scrolled|error|saving` */
class EventFormPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val state = intent.getStringExtra("state") ?: "default"
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(
                    colorScheme = lightColorScheme(
                        primary = FieldTheme.accent,
                        onPrimary = FieldTheme.textOnAccent,
                        background = FieldTheme.page,
                        surface = FieldTheme.raised,
                        onBackground = FieldTheme.textPrimary,
                        onSurface = FieldTheme.textPrimary,
                    ),
                    typography = yahpazTypography(),
                ) {
                    val scroll = rememberScrollState(if (state == "scrolled") 520 else 0)
                    Column(Modifier.fillMaxSize().background(FieldTheme.page).statusBarsPadding()) {
                        EventCreateHeader(
                            title = EVENT_NEW_TITLE,
                            saveTitle = eventFormPrimarySaveTitle(editing = false),
                            saving = state == "saving",
                            enabled = true,
                            error = if (state == "error") EVENT_DRAFT_FORM_ERROR else null,
                            scrolled = scroll.value > 0,
                            onBack = {},
                            onSave = {},
                        )
                        Column(
                            Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll)
                                .padding(horizontal = 16.dp).padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            FormCheckbox(label = EVENT_CANCELLED_LABEL, checked = false, onCheckedChange = {})
                            FormSectionHeading(EVENT_FORM_LEADS_SECTION)
                            FormField(label = "אחמ״ש", value = "עמרי לנדמן · 120", onValueChange = {}, modifier = Modifier.fillMaxWidth())
                            FormSectionHeading(EVENT_FORM_DETAILS_SECTION)
                            ReturnDateField(label = "תאריך", value = "03.11.2026", onValueChange = {}, showCalendar = true, modifier = Modifier.fillMaxWidth())
                            FormFieldRow {
                                LookupPickerField(
                                    label = "סוג אירוע",
                                    options = listOf(LookupOption("1", "תאונה")),
                                    selectedId = if (state == "error") "" else "1",
                                    onSelect = {},
                                    placeholder = "בחירת סוג",
                                    searchPlaceholder = "חיפוש סוג אירוע",
                                    error = if (state == "error") "יש לבחור סוג אירוע" else null,
                                    modifier = Modifier.weight(1f),
                                )
                                FormField(label = "מספר אירוע", value = "6389", onValueChange = {}, mono = true, ltr = true, modifier = Modifier.weight(1f))
                            }
                            FormFieldRow {
                                FormField(label = "אוק - כינוי", value = "אביב", onValueChange = {}, modifier = Modifier.weight(1f))
                                FormField(label = "אוק - מס", value = "511", onValueChange = {}, mono = true, ltr = true, modifier = Modifier.weight(1f))
                            }
                            FormFieldRow {
                                TimeField(label = EVENT_START_TIME_LABEL, value = "14:20", onValueChange = {}, showNow = true, modifier = Modifier.weight(1f))
                                TimeField(label = EVENT_END_TIME_LABEL, value = "", onValueChange = {}, showNow = true, modifier = Modifier.weight(1f))
                            }
                            LookupPickerField(label = "שלוחה", options = listOf(LookupOption("d", "תל אביב")), selectedId = "d", onSelect = {}, placeholder = "בחירת שלוחה", searchPlaceholder = "חיפוש שלוחה", modifier = Modifier.fillMaxWidth())
                            LookupPickerField(label = "כביש", options = listOf(LookupOption("r", "2")), selectedId = "r", onSelect = {}, placeholder = "בחירת כביש", searchPlaceholder = "חיפוש כביש", modifier = Modifier.fillMaxWidth())
                            FormField(label = "מיקום", value = "מחלף גלילות", onValueChange = {}, modifier = Modifier.fillMaxWidth())
                            FormArea(label = "הערות", value = "", onValueChange = {}, minHeight = 96)
                            Spacer(Modifier.height(400.dp))
                        }
                    }
                }
            }
        }
    }
}
