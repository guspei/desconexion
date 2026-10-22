#!/bin/sh
# Builds desconexion.apk, the file to share and install.
set -e
cd "$(dirname "$0")"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home}"

./gradlew -q assembleRelease
apk=app/build/outputs/apk/release/app-release.apk

# The first installs were signed with the debug key; the lineage lets them update to the release key.
if [ -f firma/linaje ]; then
  signer=$(ls -d "$(sed -n 's/^sdk.dir=//p' local.properties)"/build-tools/*/apksigner | tail -1)
  pass=$(sed -n 's/^storePassword=//p' firma/clave.properties)
  "$signer" sign --v4-signing-enabled false --rotation-min-sdk-version 28 --lineage firma/linaje \
    --ks ~/.android/debug.keystore --ks-key-alias androiddebugkey --ks-pass pass:android \
    --next-signer --ks firma/desconexion.jks --ks-key-alias desconexion --ks-pass "pass:$pass" \
    --out desconexion.apk "$apk"
else
  cp "$apk" desconexion.apk
fi
