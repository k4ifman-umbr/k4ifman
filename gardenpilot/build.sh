#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
GP_SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -z "$GP_SDK" ]]; then echo "Set ANDROID_HOME to an Android SDK installation"; exit 1; fi
GP_BT="$GP_SDK/build-tools/35.0.0"
GP_JAR="$GP_SDK/platforms/android-35/android.jar"
mkdir -p build/classes build/generated build/dex build/tests dist
javac -encoding UTF-8 --release 8 -d build/tests src/app/gardenpilot/Engine.java tests/EngineTest.java
java -ea -cp build/tests app.gardenpilot.EngineTest | tee build/test-results.txt
"$GP_BT/aapt2" compile --dir res -o build/resources.zip
"$GP_BT/aapt2" link -o build/resources.apk -I "$GP_JAR" --manifest AndroidManifest.xml --java build/generated --min-sdk-version 34 --target-sdk-version 35 build/resources.zip
find src build/generated -name '*.java' -print > build/sources.txt
javac -encoding UTF-8 --release 8 -classpath "$GP_JAR" -d build/classes @build/sources.txt
jar cf build/classes.jar -C build/classes .
"$GP_BT/d8" --release --min-api 34 --lib "$GP_JAR" --output build/dex build/classes.jar
cp build/resources.apk build/unsigned.apk
(cd build/dex && zip -q -X ../unsigned.apk classes*.dex)
"$GP_BT/zipalign" -f -p 4 build/unsigned.apk build/aligned.apk
if [[ ! -f build/development.keystore ]]; then
 keytool -genkeypair -keystore build/development.keystore -storepass android -keypass android -alias gardenpilot -keyalg RSA -keysize 2048 -validity 3650 -dname "CN=GardenPilot Development" -noprompt
fi
"$GP_BT/apksigner" sign --ks build/development.keystore --ks-key-alias gardenpilot --ks-pass pass:android --key-pass pass:android --out dist/GardenPilot.apk build/aligned.apk
"$GP_BT/apksigner" verify --verbose --print-certs dist/GardenPilot.apk | tee build/signature.txt
"$GP_BT/aapt2" dump badging dist/GardenPilot.apk > build/package.txt
(cd dist && sha256sum GardenPilot.apk > SHA256SUMS.txt)
cp README.md dist/README.md
