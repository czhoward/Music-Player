# Music Player Modernization Handoff

## Intent

Bring this Android music player up to current standards while keeping the core listening experience stable. The work is intended to modernize the codebase, reduce legacy compatibility debt, add missing regression coverage, and simplify the implementation where possible.

The current scope is broader than bug fixes alone. A support-floor change is acceptable if it unlocks meaningful cleanup and reduces long-term maintenance cost.

## What Has Already Been Done

- Added a real fallback notification for the `MediaSessionService` foreground-service start-not-allowed case instead of leaving the previous TODO path.
- Centralized notification construction in `NotificationHelper` so the permission, scanner, and fallback notifications share one builder path.
- Removed the custom Room query executor from `SongsDatabase` and deleted the now-unused `MyExecutor` singleton.
- Verified the patch structure with `git diff --check`.

## Current Working Tree

The repo currently has these local changes in progress:

- `app/src/main/kotlin/org/fossify/musicplayer/playback/PlaybackService.kt`
- `app/src/main/kotlin/org/fossify/musicplayer/helpers/NotificationHelper.kt`
- `app/src/main/kotlin/org/fossify/musicplayer/databases/SongsDatabase.kt`
- `app/src/main/kotlin/org/fossify/musicplayer/objects/MyExecutor.kt` deleted

## Known Environment Blockers

- Java is installed on the machine, but Android SDK configuration is still missing for Gradle.
- `local.properties` is not present in the repo.
- `ANDROID_HOME` and `ANDROID_SDK_ROOT` are unset.
- `./gradlew :app:compileCoreDebugKotlin` currently fails because Gradle cannot locate the Android SDK.

## Recommended Next Steps

1. Configure the Android SDK for the current machine by setting `local.properties` or `ANDROID_HOME` / `ANDROID_SDK_ROOT`.
2. Re-run a focused compile for the relevant flavor, then expand to lint and detekt once the build is healthy.
3. Add regression tests around the playback/service flow and the media library loading path.
4. Continue replacing or isolating legacy threading patterns, starting with controller acquisition and any remaining background helpers.
5. Simplify the media stack by reviewing the legacy compatibility surface versus the Media3 path.
6. Clean up manifest/storage compatibility for the new support floor.
7. Review stale dependencies and UI helpers for removal or replacement.

## Planned Modernization TODO

### P0: Stabilize and Validate

- Get Android SDK configuration working on the new OS.
- Compile the active flavor(s) successfully.
- Run lint and detekt after the compile is green.
- Add the first regression tests for playback startup and queue behavior.

### P1: Remove Legacy Surfaces

- Revisit `SimpleMediaController` and the controller acquisition flow to reduce executor usage.
- Replace scattered background-thread helpers with structured concurrency where practical.
- Audit `EventBus` usage and decide whether each path should move to a state-holder or be kept behind a smaller boundary.
- Review any deprecated Media3 workarounds, especially shuffle-order behavior in `SimpleMusicPlayer`.

### P2: Simplify App Behavior

- Remove or isolate storage and permission compatibility that is no longer needed after the support-floor decision.
- Review the manifest for obsolete permissions, exported declarations, and legacy compatibility flags.
- Reduce UI duplication in adapters, fragments, and activity code.
- Tighten the startup path so the app controller lifecycle is easier to reason about.

### P3: Dependency and Architecture Cleanup

- Evaluate stale libraries such as `autofittextview`, `EventBus`, and `jAudioTagger`.
- Decide whether the app should stay single-module or move toward a smaller internal module split.
- Improve test coverage for database, playback, and library scanning logic.

## Useful Files to Resume From

- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/kotlin/org/fossify/musicplayer/App.kt`
- `app/src/main/kotlin/org/fossify/musicplayer/helpers/NotificationHelper.kt`
- `app/src/main/kotlin/org/fossify/musicplayer/playback/PlaybackService.kt`
- `app/src/main/kotlin/org/fossify/musicplayer/playback/player/SimpleMusicPlayer.kt`
- `app/src/main/kotlin/org/fossify/musicplayer/helpers/SimpleMediaController.kt`
- `app/src/main/kotlin/org/fossify/musicplayer/databases/SongsDatabase.kt`
- `gradle/libs.versions.toml`

## Suggested Resume Command Sequence

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew --no-daemon --max-workers=1 -Dorg.gradle.jvmargs='-Xmx1024m -Dfile.encoding=UTF-8' :app:compileCoreDebugKotlin
```

If the Android SDK is not yet configured, add it first via `local.properties` or environment variables and then rerun the build.
