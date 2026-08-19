#!/usr/bin/env bash
# Publish a built APK + version.json into op-yh-26 (yahpz.com /android).
# Usage: scripts/publish-apk-to-website.sh [path-to-apk]
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APK_SRC="${1:-}"
VERSION_CODE="$(sed -n 's/.*versionCode *= *\([0-9][0-9]*\).*/\1/p' "$ROOT/app/build.gradle.kts" | head -1)"
VERSION_NAME="$(sed -n 's/.*versionName *= *"\([^"]*\)".*/\1/p' "$ROOT/app/build.gradle.kts" | head -1)"
APK_NAME="yahpaz-${VERSION_NAME}.apk"
APK_URL="https://yahpz.com/android/${APK_NAME}"

if [[ -z "$APK_SRC" ]]; then
  APK_SRC="$ROOT/app/build/outputs/apk/release/app-release.apk"
fi
if [[ ! -f "$APK_SRC" ]]; then
  echo "Missing APK at $APK_SRC" >&2
  exit 1
fi

WEB_ROOT="${YAHPZ_WEB_ROOT:-$ROOT/../op-yh-26}"
if [[ ! -d "$WEB_ROOT/.git" ]]; then
  git clone --depth 1 https://github.com/omriland/op-yh-26.git "$WEB_ROOT"
fi
cd "$WEB_ROOT"
git fetch origin infra/bootstrap
git checkout infra/bootstrap
git pull origin infra/bootstrap

mkdir -p public/android
cp "$APK_SRC" "public/android/$APK_NAME"
rm -f public/android/yahpaz.apk
cat > public/android/version.json <<JSON
{
  "minVersionCode": ${VERSION_CODE},
  "latestVersionCode": ${VERSION_CODE},
  "latestVersionName": "${VERSION_NAME}",
  "apkUrl": "${APK_URL}",
  "messageHe": "יש גרסה חדשה של האפליקציה (כולל סריקת לוחיות ניסיונית). אם ההתקנה נכשלת — הסירו את הגרסה הישנה ואז התקינו מחדש."
}
JSON

BRANCH="cursor/android-${VERSION_NAME}-website-2580"
git checkout -B "$BRANCH"
git add "public/android/$APK_NAME" public/android/version.json
git -c user.email=cursoragent@cursor.com -c user.name=Cursor commit -m "Force-update Android clients to ${VERSION_NAME}."
git push -u origin "$BRANCH"
echo "Pushed $BRANCH — merge to infra/bootstrap for production."
