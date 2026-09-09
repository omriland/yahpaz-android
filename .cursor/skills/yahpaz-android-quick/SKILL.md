---
name: yahpaz-android-quick
description: >-
  Small, focused changes on the Yahpaz Android app only — no iOS, web, or backend.
  Use when the user asks for Android app changes, yahpaz-android edits, Compose/responder
  fixes, or quick Android-only work on אבן דרך.
---

# Yahpaz Android — quick changes

Small Android-only tasks. Ship code, not docs.

## Scope

- **Android only.** Repo: `/Users/omrilandman/CursorProjects/today-i/yahpaz-android`
- Do **not** touch `yahpaz-ios`, `op-yh-26`, Supabase, or any web/edge/DB work.
- Domain logic lives in `:domain`; UI in `:app` (Compose). Match existing patterns.

## How to work

1. **Implement immediately.** No plans, no todo lists, no new `.md` files, no brainstorming skill.
2. **Ask clarifying questions only if truly blocked** (ambiguous behavior, missing API shape, destructive choice).
3. Keep the diff minimal — only what the request needs.

## Verify and install (required after every change)

From the Android repo root:

```bash
cd /Users/omrilandman/CursorProjects/today-i/yahpaz-android

# Domain tests when :domain changed
./gradlew :domain:test

# Build + install debug APK to the connected device (Pixel 9)
./gradlew :app:installDebug
```

If `installDebug` fails on device detection:

```bash
~/Library/Android/sdk/platform-tools/adb devices   # expect one device (Pixel 9)
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Fix build/test failures before reporting done. Report install success or the exact error.

## Conventions (don't re-read unless needed)

- Package: `com.yahpz.responder`. Hebrew-only RTL. Field/Command look (`#1D4E89`).
- Backend is shared Supabase — **do not change it** in this skill's scope; if the request needs API/DB work, say so and stop.
- **In-app APK updates stay in-app.** Force/optional sideload updates use `SideloadApkInstaller` + `InAppUpdateActions`. Never send the APK URL to the browser (`ACTION_VIEW`) on those screens.
