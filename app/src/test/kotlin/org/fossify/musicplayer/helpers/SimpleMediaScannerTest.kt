package org.fossify.musicplayer.helpers

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import org.fossify.musicplayer.extensions.audioHelper
import org.fossify.musicplayer.extensions.config
import org.fossify.musicplayer.helpers.ALL_TRACKS_PLAYLIST_ID
import org.fossify.musicplayer.models.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.fakes.RoboCursor
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(application = SimpleMediaScannerTestApplication::class, sdk = [28])
class SimpleMediaScannerTest {
    @Test
    fun scanImportsMediaStoreTrackAndRelatedMetadata() {
        val context = RuntimeEnvironment.getApplication()
        val trackId = 17L
        val artistId = 41L
        val albumId = 31L
        val genreId = 51L
        val mediaUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        registerCursor(
            context.contentResolver,
            mediaUri,
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.DATE_ADDED
            ),
            arrayOf(
                trackId,
                188000L,
                "/music/test.mp3",
                "Test Track",
                "Test Artist",
                "Test Album",
                albumId,
                artistId,
                "1002/10",
                2024,
                10
            )
        )
        registerCursor(
            context.contentResolver,
            MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Audio.Artists._ID,
                MediaStore.Audio.Artists.ARTIST,
                MediaStore.Audio.Artists.NUMBER_OF_TRACKS,
                MediaStore.Audio.Artists.NUMBER_OF_ALBUMS
            ),
            arrayOf(artistId, "Test Artist", 1, 1)
        )
        registerCursor(
            context.contentResolver,
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Audio.Albums._ID,
                MediaStore.Audio.Albums.ARTIST,
                MediaStore.Audio.Albums.FIRST_YEAR,
                MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.NUMBER_OF_SONGS
            ),
            arrayOf(albumId, "Test Artist", 2024, "Test Album", 1)
        )
        registerCursor(
            context.contentResolver,
            MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME),
            arrayOf(genreId, "Jazz")
        )
        registerCursor(
            context.contentResolver,
            Uri.parse("content://media/external/audio/genres/all/members"),
            arrayOf(MediaStore.Audio.Genres.Members.GENRE_ID, MediaStore.Audio.Genres.Members.AUDIO_ID),
            arrayOf(genreId, trackId)
        )

        context.config.wasAllTracksPlaylistCreated = false
        context.config.excludedFolders = mutableSetOf()
        context.config.tracksRemovedFromAllTracksPlaylist = mutableSetOf()
        val scanCompleted = CountDownLatch(1)
        var completedSuccessfully = false
        SimpleMediaScanner(context).scan { complete ->
            if (complete) {
                completedSuccessfully = true
                scanCompleted.countDown()
            }
        }

        assertTrue("MediaStore scan did not complete", scanCompleted.await(10, TimeUnit.SECONDS))
        assertTrue(completedSuccessfully)

        val tracksRead = CountDownLatch(1)
        var tracks: List<Track>? = null
        var playlistTracks: List<Track>? = null
        var readFailure: Throwable? = null
        Thread {
            try {
                tracks = context.audioHelper.getAllTracks()
                playlistTracks = context.audioHelper.getPlaylistTracks(ALL_TRACKS_PLAYLIST_ID)
            } catch (throwable: Throwable) {
                readFailure = throwable
            } finally {
                tracksRead.countDown()
            }
        }.start()
        assertTrue("Could not read scanned tracks", tracksRead.await(10, TimeUnit.SECONDS))
        readFailure?.let { throw AssertionError("Could not read scanned tracks", it) }

        val track = tracks!!.single { it.mediaStoreId == trackId }
        assertEquals("Test Track", track.title)
        assertEquals("Test Artist", track.artist)
        assertEquals("Test Album", track.album)
        assertEquals(2, track.trackId)
        assertEquals(1, track.discNumber)
        assertEquals(artistId, track.artistId)
        assertEquals(albumId, track.albumId)
        assertEquals(genreId, track.genreId)
        assertEquals(trackId, playlistTracks!!.single().mediaStoreId)
    }

    private fun registerCursor(
        resolver: ContentResolver,
        uri: Uri,
        columns: Array<String>,
        row: Array<Any>
    ) {
        val cursor = RoboCursor().apply {
            setColumnNames(columns.toList())
            setResults(arrayOf(row))
        }
        shadowOf(resolver).setCursor(uri, cursor)
    }
}

class SimpleMediaScannerTestApplication : Application()
