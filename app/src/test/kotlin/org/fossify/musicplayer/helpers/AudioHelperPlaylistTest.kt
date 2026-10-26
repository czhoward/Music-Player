package org.fossify.musicplayer.helpers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.fossify.musicplayer.extensions.playlistDAO
import org.fossify.musicplayer.extensions.tracksDAO
import org.fossify.musicplayer.models.Playlist
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
    fun deletingPlaylistRemovesItsTracksButPreservesLibraryTracks() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val audioHelper = AudioHelper(context)
        val mediaStoreId = System.nanoTime()
        val playlistTitle = "Playlist deletion test $mediaStoreId"
        var playlistId: Int? = null

        withContext(Dispatchers.IO) {
            try {
                playlistId = audioHelper.insertPlaylist(Playlist(0, playlistTitle)).toInt()
                val libraryTrack = track(mediaStoreId)
                val playlistTrack = libraryTrack.copy(id = 0, playListId = playlistId!!)
                audioHelper.insertTracks(listOf(libraryTrack, playlistTrack))

                audioHelper.deletePlaylists(arrayListOf(Playlist(playlistId!!, playlistTitle)))

                assertEquals(null, context.playlistDAO.getPlaylistWithId(playlistId!!))
                assertTrue(audioHelper.getPlaylistTracks(playlistId!!).isEmpty())
                assertEquals(listOf(mediaStoreId), audioHelper.getPlaylistTracks(0).map { it.mediaStoreId })
            } finally {
                context.tracksDAO.removeTrack(mediaStoreId)
                playlistId?.let { id ->
                    context.playlistDAO.getPlaylistWithId(id)?.let {
                        context.playlistDAO.deletePlaylists(listOf(it))
                    }
                }
            }
        }
    }

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
