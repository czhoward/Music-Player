# Music Player Modernization Handoff

## Intent

Bring this Android music player up to current standards while keeping the core listening experience stable. The work is intended to modernize the codebase, reduce legacy compatibility debt, add missing regression coverage, and simplify the implementation where possible.

The current scope is broader than bug fixes alone. A support-floor change is acceptable if it unlocks meaningful cleanup and reduces long-term maintenance cost.

## What Has Already Been Done

- Added a real fallback notification for the `MediaSessionService` foreground-service start-not-allowed case instead of leaving the previous TODO path.
- Centralized notification construction in `NotificationHelper` so the permission, scanner, and fallback notifications share one builder path.
- Removed the custom Room query executor from `SongsDatabase` and deleted the now-unused `MyExecutor` singleton.
- Added JUnit coverage for moving queued list items before earlier and later targets in `MutableListTest`.
- Added a Robolectric test for `MediaItemProvider` root construction and its pre-reload readiness contract.
- Fixed `MediaItemProvider.reload()` so a build failure remains `STATE_ERROR` instead of being overwritten by `STATE_INITIALIZED`.
- Added Robolectric coverage for a successful empty-library reload and `PlaybackService.onCreate()` initialization.
- Replaced `SimpleMediaController`'s single-thread executor and blocking controller-future reads with completion listeners; callbacks now wait for an available connected controller, and releasing before first acquisition is safe.
- Added `addListenerWithResult` and a regression test proving callbacks run only after their future completes.
- Reformatted a long `AlbumHeader` constructor call in `TracksActivity` to satisfy Detekt.
- Compiled the `coreDebug`, `fossDebug`, and `gplayDebug` variants successfully.
- Ran lint successfully for all three debug variants and Detekt successfully across 102 Kotlin files.
- Ran the core JVM/Robolectric unit tests successfully: 6 tests, 0 failures.
- Verified the final patch with `git diff --check`.

## Current Working Tree

Current uncommitted work on `main`:

- `app/src/main/kotlin/org/fossify/musicplayer/playback/library/MediaItemProvider.kt`
- `app/src/test/kotlin/org/fossify/musicplayer/playback/library/MediaItemProviderTest.kt`
- `app/src/test/kotlin/org/fossify/musicplayer/playback/PlaybackServiceTest.kt`
- `COPILOT_HANDOFF.md`

The queue-order tests, Robolectric setup, provider reload test, and service startup test are in the current worktree.

## Environment and Validation Notes

- Java 17 is available at `/usr/lib/jvm/java-17-openjdk-amd64`.
- `ANDROID_HOME` is `/home/vagrant/android-sdk`; API 36 and Build-Tools 36 were installed there. `local.properties` is intentionally absent.
- The host now has 3.8 GiB RAM (about 2.8 GiB available at last check). Earlier combined builds OOM-killed daemons when the host had 1.9 GiB. Serial builds with a 1 GiB Gradle heap and Kotlin compilation in-process have passed; the repository default heap is 8 GiB.
- Lint passes with the existing baseline, which suppresses 6 errors and 194 warnings; each flavor also reports 14-16 warnings. Four lint baseline entries no longer match current findings and should be reviewed separately.
- The tests cover queue list moves, future completion callbacks, provider root construction and successful empty-library reload, and service `onCreate()` initialization. A populated MediaStore scan and Android system-level foreground-service behavior remain untested.

## Recommended Next Steps

1. Add player-level queue tests beyond the list-move primitive.
2. Cover populated MediaStore library loading and Android system-level foreground-service behavior.
3. Replace scattered background-thread helpers with structured concurrency where practical; controller acquisition no longer uses a dedicated executor.
4. Audit `EventBus` usage and decide which flows should move to a state-holder.
5. Review deprecated Media3 workarounds, especially shuffle-order behavior in `SimpleMusicPlayer`.
6. Decide the new minimum supported Android version, then review storage permissions and manifest compatibility against that floor.
7. Review the four stale lint-baseline entries and existing lint warnings without broadening into unrelated dependency updates.
8. Evaluate stale libraries and UI helpers, including `autofittextview`, `EventBus`, and `jAudioTagger`.

## Planned Modernization TODO

### P0: Stabilize and Validate

- [x] Get Android SDK configuration working on the new OS.
- [x] Compile `coreDebug`, `fossDebug`, and `gplayDebug` successfully.
- [x] Run lint for all three debug variants and Detekt.
- [x] Add initial queue-order primitive tests.
- [x] Add a Robolectric test for provider root construction and pre-reload readiness.
- [x] Cover successful empty-library reload and playback-service `onCreate()` initialization.
- [ ] Add player-level queue behavior tests.

### P1: Remove Legacy Surfaces

- [x] Revisit `SimpleMediaController` and remove its dedicated controller-acquisition executor.
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
./gradlew --no-daemon --max-workers=1 -Pkotlin.compiler.execution.strategy=in-process -Dorg.gradle.jvmargs='-Xmx1024m -Dfile.encoding=UTF-8' :app:compileCoreDebugKotlin
```

For analysis, run one task at a time with the same options, for example `:app:detekt` or `:app:lintCoreDebug`. The SDK is available at `/home/vagrant/android-sdk`; set `ANDROID_HOME` to that path if it is not already exported in the shell.
