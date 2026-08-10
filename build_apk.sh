#!/usr/bin/env bash
# Cross-platform manual APK build (works on local Windows and CI Linux runners).
# Requires ANDROID_SDK_ROOT and JAVA_HOME. On this machine they are provided by
# the local tooling env.sh; on GitHub Actions they are supplied by setup-java /
# android-actions/setup-android.
set -e

# ---- project root = directory containing this script ----
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP="$SCRIPT_DIR"
# Native Windows tools (aapt2/zipalign/apksigner) need Windows-style paths,
# not the POSIX form MSYS `pwd` returns. On CI (Linux) cygpath is absent.
if command -v cygpath >/dev/null 2>&1; then
  APP="$(cygpath -m "$APP")"
fi

# ---- toolchain resolution ----
LOCAL_TOOLING="/c/Users/30332/.workbuddy/tooling"
if [ -z "$ANDROID_SDK_ROOT" ] && [ -f "$LOCAL_TOOLING/env.sh" ]; then
  # shellcheck disable=SC1091
  source "$LOCAL_TOOLING/env.sh"
fi
if [ -z "$JAVA_HOME" ] && [ -f "$LOCAL_TOOLING/env.sh" ]; then
  # shellcheck disable=SC1091
  source "$LOCAL_TOOLING/env.sh"
fi

if [ -z "$JAVA_HOME" ] || [ -z "$ANDROID_SDK_ROOT" ]; then
  echo "ERROR: set JAVA_HOME and ANDROID_SDK_ROOT (or keep local tooling at $LOCAL_TOOLING)."
  exit 1
fi

# ---- OS-aware binary extension for native Windows executables ----
EXT=""
case "$(uname -s 2>/dev/null)" in
  MINGW*|MSYS*|CYGWIN*|*_NT-*) EXT=".exe" ;;
esac

SRC="$APP/app/src/main/java"
RES="$APP/app/src/main/res"
MAN="$APP/app/src/main/AndroidManifest.xml"
ANDROID_JAR="$ANDROID_SDK_ROOT/platforms/android-33/android.jar"
BT="$ANDROID_SDK_ROOT/build-tools/33.0.2"

BUILD="$APP/build"
GEN="$BUILD/gen"
CLASSES="$BUILD/classes"
RESCMP="$BUILD/res-compiled"
rm -rf "$BUILD"
mkdir -p "$GEN" "$CLASSES" "$RESCMP"

AAPT2="$BT/aapt2$EXT"
ZIPALIGN="$BT/zipalign$EXT"
D8_JAR="$BT/lib/d8.jar"
JAVAC="$JAVA_HOME/bin/javac$EXT"
JAR="$JAVA_HOME/bin/jar$EXT"
JAVAEXE="$JAVA_HOME/bin/java$EXT"
KEYTOOL="$JAVA_HOME/bin/keytool$EXT"
if [ -f "$BT/apksigner.bat" ]; then APKSIGNER="$BT/apksigner.bat"; else APKSIGNER="$BT/apksigner"; fi

echo "JAVA_HOME=$JAVA_HOME"
echo "ANDROID_SDK_ROOT=$ANDROID_SDK_ROOT"

echo "=== [1/6] aapt2 compile resources ==="
"$AAPT2" compile -o "$RESCMP" --dir "$RES"
echo "compiled $(ls "$RESCMP" | wc -l) flat files"

echo "=== [2/6] aapt2 link -> unsigned apk + R.java ==="
FLATS=$(find "$RESCMP" -name '*.flat' -printf '%p ')
"$AAPT2" link -o "$BUILD/app-unsigned.apk" \
  -I "$ANDROID_JAR" \
  --manifest "$MAN" \
  --java "$GEN" \
  $FLATS
echo "link done; R.java exists: $(test -f "$GEN/com/icssettings/app/R.java" && echo yes)"

echo "=== [3/6] javac compile Java (release 11) ==="
SRC_FILES=$(find "$SRC" -name '*.java')
"$JAVAC" --release 11 -encoding UTF-8 \
  -classpath "$ANDROID_JAR" \
  -sourcepath "$SRC;$GEN" \
  -d "$CLASSES" \
  $SRC_FILES \
  "$GEN/com/icssettings/app/R.java"
echo "compiled classes: $(find "$CLASSES" -name '*.class' | wc -l)"

echo "=== [4/6] d8 -> classes.dex ==="
"$JAR" cf "$BUILD/classes.jar" -C "$CLASSES" .
mkdir -p "$BUILD/dexout"
"$JAVAEXE" -cp "$D8_JAR" com.android.tools.r8.D8 --lib "$ANDROID_JAR" --output "$BUILD/dexout" "$BUILD/classes.jar"
cp "$BUILD/dexout/classes.dex" "$BUILD/classes.dex"
echo "dex built: $(ls -la "$BUILD/classes.dex" | awk '{print $5}') bytes"

echo "=== [5/6] add dex to apk (store, uncompressed) + zipalign ==="
cd "$BUILD"
"$JAR" u0f "$BUILD/app-unsigned.apk" classes.dex
"$ZIPALIGN" -p 4 "$BUILD/app-unsigned.apk" "$BUILD/app-aligned.apk"
echo "zipalign done"

echo "=== [6/6] generate keystore (if missing) + apksigner sign ==="
KS="$APP/ics_settings.keystore"
ALIAS=icssettings
STOREPASS="IcsSettings@2026!key"
if [ ! -f "$KS" ]; then
  "$KEYTOOL" -genkeypair -v \
    -keystore "$KS" -alias "$ALIAS" \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass "$STOREPASS" -keypass "$STOREPASS" \
    -dname "CN=ICS Settings, OU=ICSSettings, O=ICSSettings, L=Unknown, ST=Unknown, C=CN"
  echo "generated new keystore: $KS"
else
  echo "reusing existing keystore: $KS"
fi
"$APKSIGNER" sign --ks "$KS" --ks-key-alias "$ALIAS" \
  --ks-pass "pass:$STOREPASS" --key-pass "pass:$STOREPASS" \
  --out "$APP/ICS_Settings_4.0.apk" "$BUILD/app-aligned.apk"
echo "APK signed -> $APP/ICS_Settings_4.0.apk"

echo "=== verify ==="
"$APKSIGNER" verify --print-certs "$APP/ICS_Settings_4.0.apk" 2>&1 | head -20
ls -la "$APP/ICS_Settings_4.0.apk"
