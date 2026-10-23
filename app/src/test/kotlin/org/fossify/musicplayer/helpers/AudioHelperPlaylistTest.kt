package org.fossify.musicplayer.helpers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.fossify.musicplayer.extensions.tracksDAO
import org.fossify.musicplayer.models.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = SimpleMediaScannerTestApplication::class, sdk = [36])
class AudioHelperPlaylistTest {
    @Test
    fun removingTrackFromPlaylistPreservesLibraryTrack() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val audioHelper = AudioHelper(context)
        val mediaStoreId = System.nanoTime()
        val playlistId = 731
        val libraryTrack = track(mediaStoreId)
        val playlistTrack = libraryTrack.copy(id = 0, playListId = playlistId)

        withContext(Dispatchers.IO) {
            try {
                audioHelper.insertTracks(listOf(libraryTrack, playlistTrack))

                audioHelper.removeTracksFromPlaylist(playlistId, listOf(mediaStoreId))

                assertEquals(
                    listOf(mediaStoreId),
                    audioHelper.getPlaylistTracks(0).map { it.mediaStoreId }
                )
                assertTrue(audioHelper.getPlaylistTracks(playlistId).isEmpty())
            } finally {
                context.tracksDAO.removeTrack(mediaStoreId)
            }
        }
    }

    private fun track(mediaStoreId: Long) = Track(
        id = 0,
        mediaStoreId = mediaStoreId,
        title = "Playlist removal test",
        artist = "Test Artist",
        path = "/music/playlist-removal-test.mp3",
        duration = 180,
        album = "Test Album",
        genre = "",
        coverArt = "",
        playListId = 0,
        trackId = null,
        discNumber = null,
        folderName = "Music",
        albumId = 1,
        artistId = 1,
        genreId = 0,
        year = 2026,
        dateAdded = 0,
        orderInPlaylist = 0
    )
}
