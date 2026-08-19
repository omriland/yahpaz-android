#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

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

./gradlew :app:bundleRelease

SRC="$ROOT/app/build/outputs/bundle/release/app-release.aab"
mkdir -p "$ROOT/dist"
cp "$SRC" "$ROOT/dist/com.yahpz.responder-release.aab"
echo "AAB: $ROOT/dist/com.yahpz.responder-release.aab"
