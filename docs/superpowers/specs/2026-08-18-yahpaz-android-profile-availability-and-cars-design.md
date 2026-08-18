# Yahpaz Android — Profile: availability + my cars

**Date:** 2026-08-18  
**Repo:** `yahpaz-android`  
**Status:** Approved in brainstorming (one Profile page; compact availability → bottom sheet; view-only cars on the page)  
**Out of scope:** `yahpaz-ios` (native iOS on hold — do not touch) · `op-yh-26` (no API, schema, or web change)

## Problem

Availability is a fourth bottom-bar tab even though it is a personal setting, not a primary destination. Profile does not show the cars already linked to the signed-in user (the same `vehicles` rows fill uses).

## Goals

- Drop **זמינות** from the bottom bar. Bar is three items: **האירועים שלי**, **המשמרות שלי**, **פרופיל**.
- Show current availability on Profile as a compact tappable row. Tap opens a bottom sheet with the existing editor.
- Show **הרכבים שלי** on Profile as a view-only list (plate, model, logo when resolvable).
- Hebrew-only RTL, Field tokens, 44dp minimum touch targets.

## Non-goals

- Adding, editing, or archiving cars from the app (admin-linked only).
- Persisting a logo slug on `vehicles` (resolve at display from `model`).
- Changing availability rules, storage, or web UI.
- iOS port.
- Unifying fill’s “keep archived if currently selected” rule with this list.

## Decisions (locked)

| Topic | Choice |
|---|---|
| Bottom bar | `INBOX`, `SHIFTS`, `PROFILE`. Remove `AppTab.AVAILABILITY`. |
| Availability on Profile | Compact row: effective status (dot + label + return caption). Tap → `ModalBottomSheet`. |
| Sheet editor | Same fields and validation as today’s `AvailabilityScreen`. |
| Save | Existing `saveAvailability`. Success: dismiss sheet, toast «הזמינות עודכנה.», update `profile` in memory. |
| Dismiss without save | Discard local edits. Compact row stays on last saved effective status. |
| Cars | View-only section on the Profile scroll. Rows are not tappable. |
| Car source | `vehicles` where `user_id` = session user. Columns: `plate_number`, `model`, `archived`. |
| Visible cars | Non-empty plate after `plateDigits`; **drop archived**. No “keep archived if selected” exception. |
| Car row | `[CarLogo?] [LicensePlate] [model]`. Logo via `resolveCarLogoSlug(model)` at render; miss → no image. |
| Empty cars | «לא מקושר רכב. פנו למנהל המערכת.» No add button. |
| Forced password change | Full-screen Profile, no tabs, **no** availability row and **no** cars. Password card only (plus privacy / sign-out as today). |
| `CarLogo` | Lift out of `FillScreen` into shared UI. Fill and Profile both use it. |

## Navigation

`MainTabs` items:

1. האירועים שלי — `AppTab.INBOX`
2. המשמרות שלי — `AppTab.SHIFTS`
3. פרופיל — `AppTab.PROFILE`

`RootScreen` still routes `mustChangePassword` to `ProfileScreen` **before** `MainTabs`. That gate is unchanged except Profile hides availability and cars in that mode.

## Profile layout (signed-in, password already set)

Top to bottom, one vertical scroll, 16dp page padding, 16dp section spacing:

1. Title «פרופיל»
2. Identity `FieldCard` — שם, או״ק, דוא״ל, טלפון (unchanged)
3. Availability compact row (see below)
4. Cars section — heading «הרכבים שלי» then list / loading / empty / error
5. Activity `FieldCard` — אירועים, קילומטרים (unchanged)
6. Privacy policy link + יציאה (unchanged)

One job per section. Do not nest cards inside cards: identity stays one card; cars are one card (or one empty/error block); inner car rows use spacing, not inner cards.

## Availability

### Compact row

- Surface: one `FieldCard`, entire row clickable, height ≥ 44dp.
- Content (RTL): status disc (10dp, `FieldTheme.done` if effective available else `FieldTheme.alert`) · «זמינות: {availabilityLabel(effective)}» · if effective unavailable, `availabilityReturnCaption` in caption muted · chevron signaling it opens something.
- Effective status uses existing `effectiveAvailability(stored, availableFrom, israelToday())`.
- Accessibility: content description states current availability; row is a single action «עריכת זמינות».

### Bottom sheet

- Material3 modal bottom sheet. Scrim tap / swipe-down / back dismisses without saving.
- Body is `AvailabilityScreen` (same composable, no longer a tab). Title «זמינות», explainer, live effective preview, segmented זמין / לא זמין, optional return date, validation error, שמירת זמינות.
- Local draft state lives in the sheet. Opening the sheet seeds from `ui.profile`. Closing without a successful save throws the draft away.
- Validation and write path unchanged: `buildAvailabilityWrite` / `YahpazAPI.saveAvailability` / `AppModel.saveAvailability`.
- After successful save the compact row reflects the new stored values immediately (in-memory `profile` copy, same as today).

## Cars

### Domain

New pure helper in `:domain`:

```
data class VehicleRowInput(val plateRaw: String, val modelRaw: String?, val archived: Boolean?)
data class ProfileVehicle(val plate: String, val model: String)

fun visibleProfileVehicles(rows: List<VehicleRowInput>): List<ProfileVehicle>
```

Rules, in order:

1. `plate = plateDigits(plateRaw)`; drop if empty.
2. Drop if `archived == true`.
3. `model = modelRaw?.trim().orEmpty()`.
4. Preserve input order.

Fill keeps its own archived-exception logic. Do not change fill in this work except to call the shared `CarLogo` composable.

### API / state

- `YahpazAPI.fetchMyVehicles(): List<ProfileVehicle>` — select `plate_number, model, archived` from `vehicles` filtered by `user_id`, then `visibleProfileVehicles`.
- `AppUiState`: `vehicles`, `vehiclesLoading`, `vehiclesFailed` (same shape as events/shifts).
- Load in `applySession` alongside events/shifts. `reloadVehicles()` for retry.
- Sign-out / failed session clears the list.

### Section states

| State | UI |
|---|---|
| Loading (no list yet) | Spinner in the section. **Not** the empty copy. |
| Failed (no list yet) | EmptyState title «טעינת הרכבים נכשלה. בדקו את החיבור ונסו שוב.» action «רענון» → `reloadVehicles()`. Rest of Profile still shows. |
| Failed (stale list exists) | Keep the list; toast like shifts («טעינת הרכבים נכשלה…»). |
| Empty (loaded, zero rows) | «לא מקושר רכב. פנו למנהל המערכת.» No action. |
| Loaded | One `FieldCard`. Each row: optional `CarLogo` (28dp, omit if slug null or asset missing) · `LicensePlate` · model text (`TypeScale.body`, secondary). If model is blank, plate only. Vertical stack, 8dp+ between rows. |

Rows are not buttons. No search, sort, or archive badge.

## Shared UI

Move `CarLogo` from private `FillScreen` to `Components.kt`. Signature stays: `CarLogo(slug: String?)`. Assets remain `assets/car-logos/{slug}.png`.

## Error handling

- Availability save errors stay **on the sheet** (inline), not as a toast, matching today.
- Availability save success toast stays global («הזמינות עודכנה.»).
- Vehicle load failure does not fail the session and does not hide identity / availability / stats.
- No new backend errors: same RLS as fill’s vehicle select.

## Testing

`:domain` unit tests for `visibleProfileVehicles`:

- Drops archived.
- Drops empty / non-digit plates.
- Trims model; keeps blank model.
- Preserves order of remaining rows.
- Empty input → empty list.

Existing availability tests are unchanged.

No Compose UI tests in this slice. Verify with `:domain:test` and `:app:assembleDebug`.

## Files (expected)

| Area | Touch |
|---|---|
| `AppTab` / `RootScreen` | Remove availability tab; Profile only. |
| `ProfileScreen` | Compact row, sheet, cars section; hide extra sections when `mustChangePassword`. |
| `AvailabilityScreen` | Called from the Profile sheet; no tab route. |
| `AppModel` / `AppUiState` | Vehicles state + `reloadVehicles`. |
| `YahpazAPI` | `fetchMyVehicles`. |
| `:domain` | `visibleProfileVehicles` + tests. |
| `Components.kt` / `FillScreen` | Shared `CarLogo`. |
