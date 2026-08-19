#!/usr/bin/env bash
# Materialize Android upload keystore from environment secrets.
# Expected secrets (either pair works):
#   YAHPZ_ANDROID_KEYSTORE_BASE64 + YAHPZ_ANDROID_KEY_PROPERTIES
#   or YAHPZ_ANDROID_KEYSTORE_BASE64 + YAHPZ_ANDROID_STORE_PASSWORD + YAHPZ_ANDROID_KEY_PASSWORD
#     (+ optional YAHPZ_ANDROID_KEY_ALIAS, default upload)
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ -f "$ROOT/key.properties" && -f "$ROOT/keystore/play-upload.jks" ]]; then
  echo "Keystore already present."
  exit 0
fi

if [[ -z "${YAHPZ_ANDROID_KEYSTORE_BASE64:-}" ]]; then
  echo "Missing YAHPZ_ANDROID_KEYSTORE_BASE64" >&2
  exit 1
fi

mkdir -p "$ROOT/keystore"
echo "$YAHPZ_ANDROID_KEYSTORE_BASE64" | base64 -d > "$ROOT/keystore/play-upload.jks"
chmod 600 "$ROOT/keystore/play-upload.jks"

if [[ -n "${YAHPZ_ANDROID_KEY_PROPERTIES:-}" ]]; then
  printf '%s\n' "$YAHPZ_ANDROID_KEY_PROPERTIES" > "$ROOT/key.properties"
else
  STORE_PASSWORD="${YAHPZ_ANDROID_STORE_PASSWORD:?missing store password}"
  KEY_PASSWORD="${YAHPZ_ANDROID_KEY_PASSWORD:-$STORE_PASSWORD}"
  KEY_ALIAS="${YAHPZ_ANDROID_KEY_ALIAS:-upload}"
  cat > "$ROOT/key.properties" <<PROP
storePassword=$STORE_PASSWORD
keyPassword=$KEY_PASSWORD
keyAlias=$KEY_ALIAS
storeFile=keystore/play-upload.jks
PROP
fi
chmod 600 "$ROOT/key.properties"
echo "Wrote keystore/play-upload.jks and key.properties"
