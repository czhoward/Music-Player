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
- Replaced `MediaItemProvider`'s private executor with a serial IO coroutine scope; `PlaybackService.onDestroy()` closes the scope while allowing launched work to drain.
- Moved periodic sleep-timer seconds from EventBus to lifecycle-collected `StateFlow`; kept expiry as a one-shot EventBus event to avoid replaying a stale finish signal.
- Audited EventBus use: playlist, track, and fragment refreshes remain one-shot invalidations; recurring timer state is now modeled explicitly.
- Added Robolectric coverage for empty and seeded-Room library reloads, `PlaybackService.onCreate()` initialization, and fallback notification delivery when foreground start is blocked.
- Added player-level Robolectric coverage for inserting a new next item, moving an existing queued item, and preserving next-item behavior under a deterministic shuffle order.
- Converted source-track loading in `TracksActivity` to lifecycle-owned IO coroutine work, keeping adapter updates and placeholder state on the main dispatcher.
- Converted the direct Room-backed loads in the Albums, Artists, Folders, Genres, and Playlists pager views to the hosting Activity's lifecycle scope with IO queries and main-thread rendering.
- Converted `SimpleControllerActivity.refreshQueueAndTracks()` to fetch queue tracks in `Dispatchers.IO` and resume player updates from the Activity lifecycle scope.
- Replaced `SimpleMediaController`'s single-thread executor and blocking controller-future reads with completion listeners; callbacks now wait for an available connected controller, and releasing before first acquisition is safe.
- Added `addListenerWithResult` and a regression test proving callbacks run only after their future completes.
- Converted the artist-album load in `AlbumsActivity` from `ensureBackgroundThread` plus `runOnUiThread` to `lifecycleScope` and `Dispatchers.IO`, so work is canceled with the Activity.
- Converted source-track loading in `TracksActivity` to lifecycle-owned IO coroutine work, keeping adapter updates and placeholder state on the main dispatcher.
- Converted queue-to-playlist Room insertion in `QueueActivity` to lifecycle-owned IO work.
- Converted `SimpleControllerActivity.refreshQueueAndTracks()` to fetch queue tracks in `Dispatchers.IO` and resume player updates from the Activity lifecycle scope.
- Converted `MainActivity`'s M3U parsing and playlist database insert to lifecycle-owned IO work, then handles the result and fragment refresh on main.
- Converted `TracksFragment`'s Room-backed track query to the owning Activity's lifecycle scope, keeping custom-view updates on main.
- Added `awaitFolderTracks()` to bridge the callback-based folder scanner to suspension; `MainActivity` and `TracksActivity` now await folder results in lifecycle scopes and perform playlist writes/refresh queries on IO.
- Converted the shared add-to-playlist Room insert and playlist-row deletion to Activity lifecycle IO; adapter selection reads for queue/properties actions now stay on main.
- Moved polymorphic selected-track resolution in `BaseMusicAdapter` to lifecycle-owned IO, then dispatched share/dialog/player UI actions on main.
- Moved `TracksHeaderAdapter`'s album lookup to the Activity lifecycle IO scope while keeping artwork loading on its existing callback path.
- Moved track-deletion selection and row-position snapshots in both track adapters back to main before the existing MediaStore deletion workflow.
- Updated `AlbumsTracksAdapter` to snapshot selected models and its item list on main before its Room track lookup and delete callback.
- Moved album, artist, and genre deletion selection/position snapshots to main before their existing track/database deletion work.
- Converted `SelectPlaylistDialog`'s playlist query to the host `BaseSimpleActivity` lifecycle scope and IO dispatcher.
- Converted `NewPlaylistDialog` duplicate-title checks and Room create/rename operations to lifecycle-owned IO while keeping dialog feedback on main.
- Kept `ExportPlaylistDialog` callbacks on main for picker/file-stream work and moved its export-path preference update to lifecycle IO.
- Moved `EditDialog`'s Room track-info update to lifecycle-owned IO; its permission-sensitive tag write and file-rename flow remain callback-driven.
- Converted `TracksAdapter` playlist removal and reorder persistence to lifecycle-owned IO, while keeping selected-item snapshots and adapter mutations on main.
- Reformatted a long `AlbumHeader` constructor call in `TracksActivity` to satisfy Detekt.
- Compiled the `coreDebug`, `fossDebug`, and `gplayDebug` variants successfully.
- Ran lint successfully for all three debug variants and Detekt successfully across 105 Kotlin files.
- Ran the core JVM/Robolectric unit tests successfully: 13 tests, 0 failures.
- Verified the final patch with `git diff --check`.

## Current Working Tree

Current uncommitted work on `main`:

- `app/src/main/kotlin/org/fossify/musicplayer/adapters/AlbumsTracksAdapter.kt`
- `COPILOT_HANDOFF.md`

## Environment and Validation Notes

- Java 17 is available at `/usr/lib/jvm/java-17-openjdk-amd64`.
- `ANDROID_HOME` is `/home/vagrant/android-sdk`; API 36 and Build-Tools 36 were installed there. `local.properties` is intentionally absent.
- The host now has 3.8 GiB RAM (about 2.8 GiB available at last check). Earlier combined builds OOM-killed daemons when the host had 1.9 GiB. Serial builds with a 1 GiB Gradle heap and Kotlin compilation in-process have passed; the repository default heap is 8 GiB.
- Lint passes with the existing baseline, which suppresses 6 errors and 194 warnings; core reports 16 warnings and FOSS/GPlay report 18 each. Four lint baseline entries no longer match current findings and should be reviewed separately.
- The tests cover list movement, player-level next-item insertion/reordering including shuffle order, future and folder-callback completion, sleep-timer state, provider root construction and empty/seeded-Room reloads, service `onCreate()` initialization, and fallback notification delivery. A real populated MediaStore scan and Android OS enforcement of foreground-service restrictions remain untested. The folder bridge ignores callbacks after cancellation, but the external rescan itself is not cancellable.

## Recommended Next Steps

1. Add an instrumentation or controlled MediaStore scan test and cover Android system-level foreground-service behavior.
2. Continue replacing remaining callback-heavy `ensureBackgroundThread` uses with lifecycle-owned structured concurrency where practical; adapter selection snapshots stay on main and subtype track queries run on IO. MediaStore deletion itself and permission-sensitive tag-edit flows remain callback-based.
3. Review whether any remaining one-shot EventBus invalidations need a state-holder; recurring sleep-timer state has already moved to `StateFlow`.
4. Review remaining deprecated Media3 workarounds; the current shuffle-order next-item behavior is regression-covered but still depends on the deprecated API.
5. Decide the new minimum supported Android version, then review storage permissions and manifest compatibility against that floor.
6. Review the four stale lint-baseline entries and existing lint warnings without broadening into unrelated dependency updates.
7. Evaluate stale libraries and UI helpers, including `autofittextview`, `EventBus`, and `jAudioTagger`.

## Planned Modernization TODO

### P0: Stabilize and Validate

- [x] Get Android SDK configuration working on the new OS.
- [x] Compile `coreDebug`, `fossDebug`, and `gplayDebug` successfully.
- [x] Run lint for all three debug variants and Detekt.
- [x] Add initial queue-order primitive tests.
- [x] Add a Robolectric test for provider root construction and pre-reload readiness.
- [x] Cover successful empty-library reload and playback-service `onCreate()` initialization.
- [x] Verify a seeded Room track is published by provider reload.
- [x] Add player-level queue insertion and reordering tests.

### P1: Remove Legacy Surfaces

- [x] Revisit `SimpleMediaController` and remove its dedicated controller-acquisition executor.
- [x] Replace the media provider's executor with service-lifecycle-scoped coroutine work.
- [x] Move recurring sleep-timer updates to lifecycle-collected state while retaining expiry as a one-shot event.
- [x] Audit EventBus flows; keep one-shot refresh events and migrate recurring timer state.
- [x] Add deterministic regression coverage for the current shuffle-order next-item workaround.
- [x] Convert `AlbumsActivity`'s artist-album loading to lifecycle-owned IO work.
- [x] Convert `TracksActivity`'s source-track loading to lifecycle-owned IO work.
- [x] Convert `SimpleControllerActivity`'s queue refresh query to lifecycle-owned IO work.
- [x] Convert `QueueActivity`'s queue-to-playlist Room write to lifecycle-owned IO work.
- [x] Convert `MainActivity`'s M3U import processing to lifecycle-owned IO work.
- [x] Convert `TracksFragment`'s Room-backed track query to the owning Activity lifecycle scope.
- [x] Bridge folder-track callbacks to suspension and convert MainActivity/TracksActivity folder-playlist flows.
- [x] Move shared playlist inserts and playlist-row deletion to lifecycle-owned IO; keep adapter selection reads on main.
- [x] Keep adapter-owned queue/properties selection and share-intent operations on main.
- [x] Resolve polymorphic selected-track queries on IO before dispatching adapter player/share/properties actions.
- [x] Keep album/artist/genre deletion selection and adapter-position snapshots on main.
- [x] Move TracksAdapter playlist removal and reorder Room writes to lifecycle-owned IO.
- [x] Snapshot track-deletion selection and row positions on main before the MediaStore deletion workflow.
- [x] Snapshot AlbumsTracksAdapter selection and items on main before background album-track lookup.
- [x] Move the TracksHeaderAdapter album lookup to lifecycle-owned IO.
- [x] Move `SelectPlaylistDialog`'s Room query into the host Activity lifecycle scope.
- [x] Move playlist create/rename database work into the dialog Activity lifecycle scope.
- [x] Keep export callbacks on main and move export-path persistence to lifecycle IO.
- [x] Move edited-song Room metadata updates to lifecycle IO; retain the permission-sensitive tag flow separately.
- [x] Convert Albums, Artists, Folders, Genres, and Playlists pager queries to their hosting Activity's lifecycle scope.
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
- `app/src/test/kotlin/org/fossify/musicplayer/playback/player/SimpleMusicPlayerTest.kt`
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
