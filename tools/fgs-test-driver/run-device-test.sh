#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}"
export ANDROID_HOME="${ANDROID_HOME:-/home/colin/android-sdk}"
export PATH="$JAVA_HOME/bin:$PATH"

ADB="$ANDROID_HOME/platform-tools/adb"
TARGET_PACKAGE="org.fossify.musicplayer.debug"
DRIVER_PACKAGE="org.fossify.musicplayer.fgstest"

if [[ -z "${ANDROID_SERIAL:-}" ]]; then
    ANDROID_SERIAL="$("$ADB" devices | awk '$2 == "device" && $1 ~ /^emulator-/ { print $1; exit }')"
fi

if [[ -z "${ANDROID_SERIAL:-}" ]]; then
    echo "No running emulator found. Set ANDROID_SERIAL to an API 36 emulator." >&2
    exit 1
fi

adb() {
    "$ADB" -s "$ANDROID_SERIAL" "$@"
}

if [[ "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" != "1" ]]; then
    echo "This destructive playback test is restricted to an emulator." >&2
    exit 1
fi

if [[ "$(adb shell getprop ro.build.version.sdk | tr -d '\r')" != "36" ]]; then
    echo "This test requires an API 36 emulator." >&2
    exit 1
fi

if [[ "${FGS_TEST_RESET_APP_DATA:-}" != "1" ]]; then
    echo "This test resets the debug app's data. Re-run with FGS_TEST_RESET_APP_DATA=1 to confirm." >&2
    exit 1
fi

cd "$ROOT_DIR"
./gradlew --no-daemon --max-workers=1 \
    -Pkotlin.compiler.execution.strategy=in-process \
    -Dorg.gradle.jvmargs="-Xmx1024m -Dfile.encoding=UTF-8" \
    :app:assembleCoreDebug
./gradlew --no-daemon --max-workers=1 \
    -Dorg.gradle.jvmargs="-Xmx1024m -Dfile.encoding=UTF-8" \
    -p tools/fgs-test-driver :app:assembleDebug

adb install -r app/build/outputs/apk/core/debug/musicplayer-14-core-debug.apk
adb install -r tools/fgs-test-driver/app/build/outputs/apk/debug/app-debug.apk
adb shell pm clear "$TARGET_PACKAGE" >/dev/null
adb shell pm grant "$TARGET_PACKAGE" android.permission.READ_MEDIA_AUDIO
adb shell pm grant "$TARGET_PACKAGE" android.permission.POST_NOTIFICATIONS

track_file="$(mktemp --suffix=.wav)"
track_name="fgs-denial-probe-$(date +%s).wav"

cleanup() {
    adb shell am force-stop "$DRIVER_PACKAGE" >/dev/null 2>&1 || true
    track_id="$(adb shell content query \
        --uri content://media/external/audio/media \
        --projection _id:_display_name 2>/dev/null |
        tr -d '\r' |
        grep -F "$track_name" |
        sed -n 's/.*_id=\([0-9][0-9]*\).*/\1/p' |
        head -n 1 || true)"
    if [[ -n "$track_id" ]]; then
        adb shell content delete \
            --uri "content://media/external/audio/media/$track_id" >/dev/null 2>&1 || true
    fi
    adb shell am force-stop "$TARGET_PACKAGE" >/dev/null 2>&1 || true
    adb shell pm clear "$TARGET_PACKAGE" >/dev/null 2>&1 || true
    rm -f "$track_file"
}
trap cleanup EXIT

python3 - "$track_file" <<'PY'
import math
import struct
import sys
import wave

with wave.open(sys.argv[1], "wb") as audio:
    audio.setparams((1, 2, 44100, 0, "NONE", "not compressed"))
    audio.writeframes(
        b"".join(
            struct.pack("<h", int(5000 * math.sin(2 * math.pi * 440 * i / 44100)))
            for i in range(44100 * 60)
        )
    )
PY

adb shell mkdir -p /sdcard/Music
adb push "$track_file" "/sdcard/Music/$track_name" >/dev/null
adb shell am broadcast \
    -a android.intent.action.MEDIA_SCANNER_SCAN_FILE \
    -d "file:///sdcard/Music/$track_name" >/dev/null
media_store_id=""
for attempt in $(seq 1 30); do
    media_store_id="$(adb shell content query \
        --uri content://media/external/audio/media \
        --projection _id:_display_name |
        tr -d '\r' |
        grep -F "$track_name" |
        sed -n 's/.*_id=\([0-9][0-9]*\).*/\1/p' |
        head -n 1 || true)"
    if [[ -n "$media_store_id" ]]; then
        break
    fi
    sleep 1
done

if [[ -z "$media_store_id" ]]; then
    echo "The emulator did not index the generated test track." >&2
    exit 1
fi

adb shell am force-stop "$TARGET_PACKAGE"
adb shell am start \
    -n "$TARGET_PACKAGE/org.fossify.musicplayer.activities.MainActivity" >/dev/null

echo "Waiting for the app to import $track_name into its library..."
track_imported=false
for attempt in $(seq 1 60); do
    track_count="$(adb shell "run-as $TARGET_PACKAGE sqlite3 databases/songs.db \"select count(*) from tracks where path like '%/$track_name';\"" 2>/dev/null | tr -d '\r' || true)"
    if [[ "$track_count" =~ ^[1-9][0-9]*$ ]]; then
        track_imported=true
        break
    fi
    sleep 1
done

if [[ "$track_imported" != "true" ]]; then
    echo "The target app did not import the test track into Room." >&2
    exit 1
fi

adb shell am force-stop "$TARGET_PACKAGE"
adb shell am start \
    -n "$TARGET_PACKAGE/org.fossify.musicplayer.activities.MainActivity" >/dev/null
sleep 3

service_state="$(adb shell dumpsys activity services "$TARGET_PACKAGE")"
if grep -F "isForeground=true" <<<"$service_state" >/dev/null; then
    echo "The playback service is already foreground; refusing a false-positive run." >&2
    exit 1
fi

adb shell input keyevent HOME
adb shell logcat -c
adb shell am start -n "$DRIVER_PACKAGE/.FgsProbeActivity" >/dev/null

driver_pid=""
for attempt in $(seq 1 10); do
    driver_pid="$(adb shell pidof "$DRIVER_PACKAGE" | tr -d '\r' || true)"
    if [[ -n "$driver_pid" ]]; then
        break
    fi
    sleep 1
done

if [[ -z "$driver_pid" ]]; then
    echo "The driver process did not start." >&2
    exit 1
fi
adb shell cmd activity unfreeze --sticky "$driver_pid" >/dev/null

echo "Driver connected. Waiting for the 15-second allowlist expiry and fallback notification..."
notification_found=false
for attempt in $(seq 1 45); do
    if adb shell dumpsys notification --noredact |
        grep -F "pkg=$TARGET_PACKAGE" |
        grep -F "id=42" >/dev/null; then
        notification_found=true
        break
    fi
    sleep 1
done

if [[ "$notification_found" != "true" ]]; then
    adb shell logcat -d -t 1500 | grep -i -E \
        "ForegroundServiceStartNotAllowed|Background started FGS|FgsProbeActivity" || true
    echo "Fallback notification ID 42 was not posted." >&2
    exit 1
fi

logcat_output="$(adb shell logcat -d -t 2000)"
if ! grep -F "Background started FGS: Disallowed" <<<"$logcat_output" |
    grep -F "code:DENIED" >/dev/null; then
    echo "$logcat_output" | grep -i -E \
        "ForegroundServiceStartNotAllowed|Background started FGS|FgsProbeActivity" || true
    echo "The emulator did not log a denied foreground-service start." >&2
    exit 1
fi

echo "PASS: API 36 denied the background start and the app posted fallback notification ID 42."
