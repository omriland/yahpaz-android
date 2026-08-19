#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ -z "${JAVA_HOME:-}" ]]; then
  if [[ -x /opt/homebrew/opt/openjdk@21/bin/keytool ]]; then
    export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
  elif [[ -x /opt/homebrew/opt/openjdk/bin/keytool ]]; then
    export JAVA_HOME="/opt/homebrew/opt/openjdk"
  fi
fi
if [[ -n "${JAVA_HOME:-}" ]]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi

KEYSTORE="$ROOT/keystore/play-upload.jks"
PROPS="$ROOT/key.properties"

if [[ -f "$KEYSTORE" && -f "$PROPS" ]]; then
  echo "Upload keystore already exists at $KEYSTORE"
  echo "Keep key.properties and the .jks file backed up. Losing them means you cannot update the Play listing."
  exit 0
fi

mkdir -p "$ROOT/keystore"
PASSWORD="$(openssl rand -base64 24 | tr -d '/+=' | cut -c1-24)"

keytool -genkeypair -v \
  -keystore "$KEYSTORE" \
  -storetype JKS \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias upload \
  -dname "CN=Yahpaz, OU=Yahpaz, O=Yahpaz, L=Kfar Saba, ST=Israel, C=IL" \
  -storepass "$PASSWORD" \
  -keypass "$PASSWORD"

cat > "$PROPS" <<EOF
storePassword=$PASSWORD
keyPassword=$PASSWORD
keyAlias=upload
storeFile=keystore/play-upload.jks
EOF

chmod 600 "$PROPS" "$KEYSTORE"
echo "Created $KEYSTORE and $PROPS (gitignored)."
echo "Back both files up somewhere that is not this repo. Google Play App Signing will keep the app signing key; this file is the upload key."
