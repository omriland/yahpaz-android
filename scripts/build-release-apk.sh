#!/bin/zsh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

WEB_PUBLIC="${1:-$ROOT/../op-yh-26/public/android}"

if [[ -z "${JAVA_HOME:-}" ]]; then
  if [[ -x /opt/homebrew/opt/openjdk@21/bin/java ]]; then
    export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
  elif [[ -x /opt/homebrew/opt/openjdk/bin/java ]]; then
    export JAVA_HOME="/opt/homebrew/opt/openjdk"
  fi
fi
if [[ -n "${JAVA_HOME:-}" ]]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi

if [[ ! -f "$ROOT/key.properties" ]]; then
  echo "Missing key.properties. Run scripts/create-upload-keystore.sh first." >&2
  exit 1
fi

VERSION_CODE="$(sed -n 's/.*versionCode *= *\([0-9][0-9]*\).*/\1/p' "$ROOT/app/build.gradle.kts" | head -1)"
VERSION_NAME="$(sed -n 's/.*versionName *= *"\([^"]*\)".*/\1/p' "$ROOT/app/build.gradle.kts" | head -1)"
if [[ -z "$VERSION_CODE" || -z "$VERSION_NAME" ]]; then
  echo "Could not read versionCode/versionName from app/build.gradle.kts" >&2
  exit 1
fi

APK_NAME="yahpaz-${VERSION_NAME}.apk"
APK_URL="https://yahpz.com/android/${APK_NAME}"

./gradlew :app:assembleRelease

SRC="$ROOT/app/build/outputs/apk/release/app-release.apk"
mkdir -p "$ROOT/dist" "$WEB_PUBLIC"
cp "$SRC" "$ROOT/dist/$APK_NAME"
cp "$SRC" "$WEB_PUBLIC/$APK_NAME"
# Drop the unversioned name so browsers/download UIs don't reuse a stale file.
rm -f "$ROOT/dist/yahpaz.apk" "$WEB_PUBLIC/yahpaz.apk"

cat > "$WEB_PUBLIC/version.json" <<EOF
{
  "minVersionCode": ${VERSION_CODE},
  "latestVersionCode": ${VERSION_CODE},
  "latestVersionName": "${VERSION_NAME}",
  "apkUrl": "${APK_URL}",
  "messageHe": "יש גרסה חדשה של האפליקציה. יש להוריד ולהתקין כדי להמשיך."
}
EOF

echo "APK: $WEB_PUBLIC/$APK_NAME"
echo "Version: $WEB_PUBLIC/version.json (code=$VERSION_CODE name=$VERSION_NAME)"
echo "URL: $APK_URL"
