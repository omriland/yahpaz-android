# Profile availability + my cars Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move duty availability off the Android bottom bar onto Profile (compact row → bottom sheet), and show the signed-in user’s linked cars as a view-only list on that same page.

**Architecture:** No API or schema change. Domain helper `visibleProfileVehicles` filters `vehicles` rows (drop archived / empty plates). `YahpazAPI.fetchMyVehicles` loads them; `AppModel` holds list + loading/failed like events/shifts. Profile becomes the hub: identity, tappable availability row that opens existing `AvailabilityScreen` in a `ModalBottomSheet`, then the cars list. Bottom bar is three tabs.

**Tech Stack:** Kotlin, Android Compose, Material3, JUnit on `:domain`, Supabase Postgrest `vehicles` table.

## Global Constraints

- Android only (`yahpaz-android`). Do not edit `yahpaz-ios` or `op-yh-26`.
- Hebrew-only RTL. Field tokens (`FieldTheme`, `TypeScale`). Touch targets ≥ 44dp.
- Cars are view-only. No add/edit/archive in the app.
- Do not persist `logo_slug` on `vehicles`. Resolve at display with `resolveCarLogoSlug(model)`.
- Do not change fill’s “keep archived if currently selected” plate rule.
- Forced password-change Profile: no availability row, no cars.
- Copy locked: empty cars «לא מקושר רכב. פנו למנהל המערכת.»; load fail «טעינת הרכבים נכשלה. בדקו את החיבור ונסו שוב.»; retry «רענון»; section title «הרכבים שלי»; save toast unchanged «הזמינות עודכנה.»
- Verify with `./gradlew :domain:test` and `./gradlew :app:assembleDebug`. No Compose UI tests.

---

## File structure

| File | Responsibility |
|---|---|
| `domain/src/main/kotlin/com/yahpz/domain/ProfileVehicles.kt` | `VehicleRowInput`, `ProfileVehicle`, `visibleProfileVehicles` |
| `domain/src/test/kotlin/com/yahpz/domain/ProfileVehiclesTest.kt` | Unit tests for the filter |
| `app/src/main/java/com/yahpz/responder/YahpazAPI.kt` | `fetchMyVehicles()` |
| `app/src/main/java/com/yahpz/responder/AppModel.kt` | Drop `AppTab.AVAILABILITY`; vehicles state; `reloadVehicles()` |
| `app/src/main/java/com/yahpz/responder/Components.kt` | Shared `CarLogo` (moved from Fill) |
| `app/src/main/java/com/yahpz/responder/FillScreen.kt` | Delete private `CarLogo`; keep calling the shared one |
| `app/src/main/java/com/yahpz/responder/AvailabilityScreen.kt` | Add `onSaved`; `fillMaxWidth` so it fits a sheet |
| `app/src/main/java/com/yahpz/responder/ProfileScreen.kt` | Compact availability, sheet, cars section |
| `app/src/main/java/com/yahpz/responder/RootScreen.kt` | Three-item bottom bar; no availability tab |

Existing types reused: `VehicleOption` in `Models.kt`, `plateDigits` in `Format.kt`, `resolveCarLogoSlug` in `CarLogoMap.kt`, `EmptyState` / `FieldCard` / `LicensePlate`.

---

### Task 1: Domain — visible profile vehicles

**Files:**
- Create: `domain/src/main/kotlin/com/yahpz/domain/ProfileVehicles.kt`
- Test: `domain/src/test/kotlin/com/yahpz/domain/ProfileVehiclesTest.kt`

**Interfaces:**
- Consumes: `plateDigits(value: String): String` from `Format.kt`
- Produces:
  - `data class VehicleRowInput(val plateRaw: String, val modelRaw: String?, val archived: Boolean?)`
  - `data class ProfileVehicle(val plate: String, val model: String)`
  - `fun visibleProfileVehicles(rows: List<VehicleRowInput>): List<ProfileVehicle>`

- [ ] **Step 1: Write the failing test**

Create `domain/src/test/kotlin/com/yahpz/domain/ProfileVehiclesTest.kt`:

```kotlin
package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileVehiclesTest {
    @Test
    fun emptyInputYieldsEmptyList() {
        assertEquals(emptyList<ProfileVehicle>(), visibleProfileVehicles(emptyList()))
    }

    @Test
    fun dropsArchived() {
        val rows = listOf(
            VehicleRowInput("1234567", "טויוטה", false),
            VehicleRowInput("7654321", "קיה", true),
            VehicleRowInput("1111111", "הונדה", null),
        )
        assertEquals(
            listOf(
                ProfileVehicle("1234567", "טויוטה"),
                ProfileVehicle("1111111", "הונדה"),
            ),
            visibleProfileVehicles(rows),
        )
    }

    @Test
    fun dropsEmptyAndNonDigitPlates() {
        val rows = listOf(
            VehicleRowInput("", "טויוטה", false),
            VehicleRowInput("abc", "קיה", false),
            VehicleRowInput("12-345-67", "הונדה", false),
        )
        assertEquals(
            listOf(ProfileVehicle("1234567", "הונדה")),
            visibleProfileVehicles(rows),
        )
    }

    @Test
    fun trimsModelAndKeepsBlank() {
        val rows = listOf(
            VehicleRowInput("1234567", "  טויוטה  ", false),
            VehicleRowInput("7654321", null, false),
            VehicleRowInput("1111111", "   ", false),
        )
        assertEquals(
            listOf(
                ProfileVehicle("1234567", "טויוטה"),
                ProfileVehicle("7654321", ""),
                ProfileVehicle("1111111", ""),
            ),
            visibleProfileVehicles(rows),
        )
    }

    @Test
    fun preservesOrderOfRemainingRows() {
        val rows = listOf(
            VehicleRowInput("1111111", "א", false),
            VehicleRowInput("2222222", "ב", true),
            VehicleRowInput("3333333", "ג", false),
        )
        assertEquals(
            listOf(
                ProfileVehicle("1111111", "א"),
                ProfileVehicle("3333333", "ג"),
            ),
            visibleProfileVehicles(rows),
        )
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
cd /Users/omrilandman/CursorProjects/today-i/yahpaz-android
./gradlew :domain:test --tests com.yahpz.domain.ProfileVehiclesTest
```

Expected: FAIL — `Unresolved reference: visibleProfileVehicles` (and `VehicleRowInput` / `ProfileVehicle`).

- [ ] **Step 3: Write minimal implementation**

Create `domain/src/main/kotlin/com/yahpz/domain/ProfileVehicles.kt`:

```kotlin
package com.yahpz.domain

data class VehicleRowInput(
    val plateRaw: String,
    val modelRaw: String?,
    val archived: Boolean?,
)

data class ProfileVehicle(
    val plate: String,
    val model: String,
)

fun visibleProfileVehicles(rows: List<VehicleRowInput>): List<ProfileVehicle> {
    return rows.mapNotNull { row ->
        val plate = plateDigits(row.plateRaw)
        if (plate.isEmpty()) return@mapNotNull null
        if (row.archived == true) return@mapNotNull null
        ProfileVehicle(
            plate = plate,
            model = row.modelRaw?.trim().orEmpty(),
        )
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
./gradlew :domain:test --tests com.yahpz.domain.ProfileVehiclesTest
```

Expected: PASS (5 tests).

Then run the full domain suite so nothing else broke:

```bash
./gradlew :domain:test
```

Expected: BUILD SUCCESSFUL, all tests PASS.

- [ ] **Step 5: Commit**

```bash
git add domain/src/main/kotlin/com/yahpz/domain/ProfileVehicles.kt domain/src/test/kotlin/com/yahpz/domain/ProfileVehiclesTest.kt
git commit -m "$(cat <<'EOF'
Add profile vehicle filter that drops archived and empty plates.

EOF
)"
```

---

### Task 2: Load vehicles in API and AppModel

**Files:**
- Modify: `app/src/main/java/com/yahpz/responder/YahpazAPI.kt` (add `fetchMyVehicles` next to `fetchFillContext`)
- Modify: `app/src/main/java/com/yahpz/responder/AppModel.kt` (`AppUiState` + `reloadVehicles` + session wiring)

**Interfaces:**
- Consumes: `visibleProfileVehicles`, `VehicleRowInput`, `ProfileVehicle`, existing `VehicleOption`, `sessionUserId()`
- Produces:
  - `suspend fun YahpazAPI.fetchMyVehicles(): List<ProfileVehicle>`
  - `AppUiState.vehicles: List<ProfileVehicle>` (default `emptyList()`)
  - `AppUiState.vehiclesLoading: Boolean` (default `false`)
  - `AppUiState.vehiclesFailed: Boolean` (default `false`)
  - `suspend fun AppModel.reloadVehicles()`

- [ ] **Step 1: Add `fetchMyVehicles`**

In `YahpazAPI.kt`, add imports:

```kotlin
import com.yahpz.domain.ProfileVehicle
import com.yahpz.domain.VehicleRowInput
import com.yahpz.domain.visibleProfileVehicles
```

Insert this function immediately after `fetchMyShifts()` (after the `return fetchByIds(...)` block, before `fetchByIds`):

```kotlin
    suspend fun fetchMyVehicles(): List<ProfileVehicle> {
        val userId = sessionUserId() ?: return emptyList()
        val rows = client.from("vehicles").select(Columns.raw("plate_number, model, archived")) {
            filter { eq("user_id", userId) }
        }.decodeList<VehicleOption>()
        return visibleProfileVehicles(
            rows.map { VehicleRowInput(it.plateNumber, it.model, it.archived) },
        )
    }
```

Do not change `fetchFillContext`.

- [ ] **Step 2: Add vehicles fields and `reloadVehicles`**

In `AppModel.kt`:

Add import:

```kotlin
import com.yahpz.domain.ProfileVehicle
```

Add three fields to `AppUiState` after `shiftsLoading`, matching events/shifts:

```kotlin
    val vehicles: List<ProfileVehicle> = emptyList(),
    val vehiclesFailed: Boolean = false,
    val vehiclesLoading: Boolean = false,
```

Add `reloadVehicles` immediately after `reloadShifts()` (before `showToast`):

```kotlin
    suspend fun reloadVehicles() {
        if (_state.value.userId == null) return
        val hadVehicles = _state.value.vehicles.isNotEmpty()
        _state.update {
            it.copy(
                vehiclesLoading = if (hadVehicles) it.vehiclesLoading else true,
                vehiclesFailed = false,
            )
        }
        try {
            val vehicles = YahpazAPI.fetchMyVehicles()
            _state.update { it.copy(vehicles = vehicles, vehiclesFailed = false) }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            if (hadVehicles) {
                showToast("טעינת הרכבים נכשלה. בדקו את החיבור ונסו שוב.", StampTone.PENDING)
            } else {
                _state.update { it.copy(vehiclesFailed = true) }
            }
        } finally {
            _state.update { it.copy(vehiclesLoading = false) }
        }
    }
```

In `applySession`, after `reloadShifts()` add `reloadVehicles()`. In the catch, also clear vehicles:

```kotlin
            reloadEvents()
            reloadShifts()
            reloadVehicles()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            _state.update {
                it.copy(
                    userId = null,
                    profile = null,
                    vehicles = emptyList(),
                    vehiclesFailed = false,
                    vehiclesLoading = false,
                )
            }
            throw error
        }
```

`signOut()` already assigns a fresh `AppUiState(booting = false)`, which clears vehicles. Leave it.

- [ ] **Step 3: Compile**

No Android unit test harness for `AppModel`. Verify compile:

```bash
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/yahpz/responder/YahpazAPI.kt app/src/main/java/com/yahpz/responder/AppModel.kt
git commit -m "$(cat <<'EOF'
Load the signed-in user's vehicles with the session.

EOF
)"
```

---

### Task 3: Lift `CarLogo` into Components

**Files:**
- Modify: `app/src/main/java/com/yahpz/responder/Components.kt` (append public `CarLogo`)
- Modify: `app/src/main/java/com/yahpz/responder/FillScreen.kt` (delete private `CarLogo` and its now-unused imports)

**Interfaces:**
- Consumes: `assets/car-logos/{slug}.png`
- Produces: `@Composable fun CarLogo(slug: String?)` in `Components.kt` — same behavior as today’s private Fill helper (28dp, omit if slug blank or file missing)

- [ ] **Step 1: Add public `CarLogo` to Components.kt**

Add imports at the top of `Components.kt`:

```kotlin
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
```

`LocalContext` and `remember` are already imported.

Append at the end of `Components.kt`:

```kotlin
@Composable
fun CarLogo(slug: String?) {
    val context = LocalContext.current
    val bitmap = remember(slug) {
        val trimmed = slug?.trim().orEmpty()
        if (trimmed.isEmpty()) {
            null
        } else {
            runCatching {
                context.assets.open("car-logos/$trimmed.png").use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }.getOrNull()
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            contentScale = ContentScale.Fit,
        )
    }
}
```

- [ ] **Step 2: Remove private `CarLogo` from FillScreen.kt**

Delete the entire private function at the bottom of `FillScreen.kt` (the `@Composable private fun CarLogo(slug: String?) { ... }` block).

`TreatedPlateRow` already calls `CarLogo(slug = row.logoSlug)` — that now resolves to the public function in the same package.

Remove these imports from `FillScreen.kt` only if nothing else in the file uses them (today they are only used by `CarLogo`):

```kotlin
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import android.graphics.BitmapFactory
```

Keep `LocalContext` if still referenced; it is not, after the move.

- [ ] **Step 3: Compile**

```bash
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/yahpz/responder/Components.kt app/src/main/java/com/yahpz/responder/FillScreen.kt
git commit -m "$(cat <<'EOF'
Share CarLogo so Profile can reuse the fill logo renderer.

EOF
)"
```

---

### Task 4: Profile hub — drop the tab, add sheet + cars

Do this as one task so availability stays reachable: removing the tab without the Profile row would hide the editor.

**Files:**
- Modify: `app/src/main/java/com/yahpz/responder/AppModel.kt` — `enum class AppTab { INBOX, SHIFTS, PROFILE }`
- Modify: `app/src/main/java/com/yahpz/responder/RootScreen.kt` — three bar items; drop `AppTab.AVAILABILITY` branch and unused `Circle` import
- Modify: `app/src/main/java/com/yahpz/responder/AvailabilityScreen.kt` — `onSaved` callback; `fillMaxWidth` instead of `fillMaxSize`
- Modify: `app/src/main/java/com/yahpz/responder/ProfileScreen.kt` — compact row, sheet, cars

**Interfaces:**
- Consumes: `AppModel.reloadVehicles`, `AppUiState.vehicles` / `vehiclesLoading` / `vehiclesFailed`, `AvailabilityScreen`, `CarLogo`, `LicensePlate`, `resolveCarLogoSlug`, `effectiveAvailability`, `availabilityLabel`, `availabilityReturnCaption`
- Produces: Profile as specified; `fun AvailabilityScreen(app: AppModel, ui: AppUiState, onSaved: () -> Unit = {})`

- [ ] **Step 1: Narrow `AppTab` and the bottom bar**

In `AppModel.kt` change the enum to:

```kotlin
enum class AppTab { INBOX, SHIFTS, PROFILE }
```

In `RootScreen.kt` `MainTabs`:

1. Remove `import androidx.compose.material.icons.outlined.Circle`.
2. Replace the `items` list with:

```kotlin
                val items = listOf(
                    Triple(AppTab.INBOX, "האירועים שלי", Icons.AutoMirrored.Outlined.ListAlt),
                    Triple(AppTab.SHIFTS, "המשמרות שלי", Icons.Outlined.CalendarMonth),
                    Triple(AppTab.PROFILE, "פרופיל", Icons.Outlined.Person),
                )
```

3. Replace the `when (ui.tab)` with:

```kotlin
            when (ui.tab) {
                AppTab.INBOX -> InboxScreen(app, ui)
                AppTab.SHIFTS -> MyShiftsScreen(app, ui)
                AppTab.PROFILE -> ProfileScreen(app, ui)
            }
```

Leave `mustChangePassword -> ProfileScreen(app, ui)` in `RootScreen` unchanged.

- [ ] **Step 2: Sheet-friendly `AvailabilityScreen`**

Change the signature and save handler. Replace the function header and the `Column` modifier + save `onClick` as follows.

Signature:

```kotlin
@Composable
fun AvailabilityScreen(app: AppModel, ui: AppUiState, onSaved: () -> Unit = {}) {
```

Column modifier — `fillMaxWidth()` not `fillMaxSize()`, so the sheet wraps the form:

```kotlin
        modifier = Modifier
            .fillMaxWidth()
            .background(FieldTheme.page)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
```

Save button `onClick`:

```kotlin
            onClick = {
                scope.launch {
                    busy = true
                    error = app.saveAvailability(
                        status,
                        if (status == AvailabilityStatus.UNAVAILABLE && returnDate.isNotBlank()) returnDate else null,
                    )
                    busy = false
                    if (error == null) onSaved()
                }
            },
```

Keep `fillMaxWidth` import; `fillMaxSize` can be removed if unused.

- [ ] **Step 3: Rewrite `ProfileScreen`**

Replace `ProfileScreen.kt` with:

```kotlin
package com.yahpz.responder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yahpz.domain.AvailabilityStatus
import com.yahpz.domain.ProfileVehicle
import com.yahpz.domain.availabilityLabel
import com.yahpz.domain.availabilityReturnCaption
import com.yahpz.domain.effectiveAvailability
import com.yahpz.domain.israelToday
import com.yahpz.domain.resolveCarLogoSlug
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(app: AppModel, ui: AppUiState) {
    val scope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var editingAvailability by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.page)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("פרופיל", style = TypeScale.title, color = FieldTheme.textPrimary)
        ui.profile?.let { profile ->
            FieldCard {
                LedgerRow("שם", profile.fullName)
                LedgerRow("או״ק", profile.callsign)
                LedgerRow("דוא״ל", profile.email)
                LedgerRow("טלפון", profile.phone.orEmpty())
            }
            if (!ui.mustChangePassword) {
                AvailabilityRow(
                    availability = profile.availability,
                    availableFrom = profile.availableFrom,
                    onClick = { editingAvailability = true },
                )
                VehiclesSection(
                    vehicles = ui.vehicles,
                    loading = ui.vehiclesLoading,
                    failed = ui.vehiclesFailed,
                    onRetry = { scope.launch { app.reloadVehicles() } },
                )
            }
            FieldCard {
                Text("סיכום פעילות", style = TypeScale.section, color = FieldTheme.textPrimary)
                LedgerRow("אירועים", profile.eventCount.toString())
                LedgerRow("קילומטרים", profile.km.toInt().toString())
            }
        }
        if (ui.mustChangePassword) {
            FieldCard {
                Text("יש לבחור סיסמה חדשה", style = TypeScale.section, color = FieldTheme.textPrimary)
                FormField("סיסמה חדשה", password, { password = it }, password = true, keyboardType = KeyboardType.Password)
                FormField("אימות סיסמה", confirm, { confirm = it }, password = true, keyboardType = KeyboardType.Password)
                error?.let { Text(it, style = TypeScale.caption, color = FieldTheme.alert) }
                PrimaryButton(
                    title = "שמירת סיסמה",
                    busy = busy,
                    onClick = {
                        if (password != confirm) {
                            error = "הסיסמאות אינן זהות."
                            return@PrimaryButton
                        }
                        scope.launch {
                            busy = true
                            error = app.completePasswordChange(password)
                            busy = false
                        }
                    },
                )
            }
        }
        PrivacyPolicyLink()
        GhostButton(title = "יציאה", onClick = { app.signOut() })
    }

    if (editingAvailability) {
        ModalBottomSheet(onDismissRequest = { editingAvailability = false }) {
            AvailabilityScreen(
                app = app,
                ui = ui,
                onSaved = { editingAvailability = false },
            )
        }
    }
}

@Composable
private fun AvailabilityRow(
    availability: AvailabilityStatus,
    availableFrom: String?,
    onClick: () -> Unit,
) {
    val effective = effectiveAvailability(availability, availableFrom, israelToday())
    val label = availabilityLabel(effective)
    val caption = if (effective == AvailabilityStatus.UNAVAILABLE) {
        availabilityReturnCaption(availableFrom)
    } else {
        null
    }
    FieldCard(
        modifier = Modifier
            .clickable(onClickLabel = "עריכת זמינות", onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "זמינות: $label"
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(
                        if (effective == AvailabilityStatus.AVAILABLE) FieldTheme.done else FieldTheme.alert,
                        CircleShape,
                    ),
            )
            Column(Modifier.weight(1f)) {
                Text("זמינות: $label", style = TypeScale.bodyStrong, color = FieldTheme.textPrimary)
                if (caption != null) {
                    Text(caption, style = TypeScale.caption, color = FieldTheme.textMuted)
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = FieldTheme.textMuted,
            )
        }
    }
}

@Composable
private fun VehiclesSection(
    vehicles: List<ProfileVehicle>,
    loading: Boolean,
    failed: Boolean,
    onRetry: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("הרכבים שלי", style = TypeScale.section, color = FieldTheme.textPrimary)
        when {
            failed && vehicles.isEmpty() -> EmptyState(
                title = "טעינת הרכבים נכשלה. בדקו את החיבור ונסו שוב.",
                actionTitle = "רענון",
                onAction = onRetry,
            )
            loading && vehicles.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = FieldTheme.accent, modifier = Modifier.size(28.dp))
            }
            vehicles.isEmpty() -> Text(
                "לא מקושר רכב. פנו למנהל המערכת.",
                style = TypeScale.body,
                color = FieldTheme.textSecondary,
            )
            else -> FieldCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    vehicles.forEach { vehicle ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CarLogo(slug = resolveCarLogoSlug(vehicle.model))
                            LicensePlate(plate = vehicle.plate)
                            if (vehicle.model.isNotEmpty()) {
                                Text(
                                    text = vehicle.model,
                                    style = TypeScale.body,
                                    color = FieldTheme.textSecondary,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
```

Dismissing the sheet (`editingAvailability = false`) destroys `AvailabilityScreen`, so unsaved draft state is discarded. Successful save also sets `editingAvailability = false` via `onSaved`. Compact row reads `ui.profile`, which `saveAvailability` already updates in memory.

- [ ] **Step 4: Run domain tests and assemble**

```bash
./gradlew :domain:test :app:assembleDebug
```

Expected: BUILD SUCCESSFUL, all domain tests PASS.

Manual check on device (if USB debugging is available): Profile tab is the third item; tapping זמינות opens the sheet; save dismisses it and updates the row; cars list or empty copy shows; forced-password path (if you have such a user) has no availability/cars.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/yahpz/responder/AppModel.kt app/src/main/java/com/yahpz/responder/RootScreen.kt app/src/main/java/com/yahpz/responder/AvailabilityScreen.kt app/src/main/java/com/yahpz/responder/ProfileScreen.kt
git commit -m "$(cat <<'EOF'
Move availability into Profile and show the user's cars there.

EOF
)"
```

---

## Spec coverage (self-review)

| Spec requirement | Task |
|---|---|
| Three-item bottom bar; remove `AppTab.AVAILABILITY` | 4 |
| Compact availability row (dot, label, return caption, chevron, 44dp, «עריכת זמינות») | 4 |
| Bottom sheet with existing editor; save dismisses; dismiss discards draft | 4 |
| `onSaved` after successful `saveAvailability`; toast unchanged | 4 |
| Cars view-only on Profile; not tappable | 4 |
| `visibleProfileVehicles` rules + tests | 1 |
| `fetchMyVehicles` + session load / retry / stale toast | 2 |
| Empty / loading / failed copy | 4 |
| Logo via `resolveCarLogoSlug(model)` at render | 4 |
| Shared `CarLogo` | 3 |
| Hide availability + cars when `mustChangePassword` | 4 |
| No iOS / no schema / fill archived-exception untouched | all (global) |
