package org.fossify.musicplayer.helpers

import android.os.Looper
import org.fossify.musicplayer.models.QueueItem
import org.fossify.musicplayer.models.Track
import org.fossify.musicplayer.playback.library.MediaItemProviderTestApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(RobolectricTestRunner::class)
@Config(application = MediaItemProviderTestApplication::class, sdk = [35])
class AudioHelperQueueTest {
    @Test
    fun lazyQueueLoadEmitsCurrentTrackThenFullQueueOffMain() {
        val context = RuntimeEnvironment.getApplication()
        val audioHelper = AudioHelper(context)
        val firstTrackId = 101L
        val currentTrackId = 202L
        val setupComplete = CountDownLatch(1)
        var setupFailure: Throwable? = null
        Thread {
            try {
                audioHelper.insertTracks(listOf(track(firstTrackId), track(currentTrackId)))
                audioHelper.resetQueue(
                    listOf(
                        QueueItem(firstTrackId, 0, false, 0),
                        QueueItem(currentTrackId, 1, true, 12)
                    )
                )
            } catch (throwable: Throwable) {
                setupFailure = throwable
            } finally {
                setupComplete.countDown()
            }
        }.start()

        assertTrue("Could not seed the test queue", setupComplete.await(10, TimeUnit.SECONDS))
        setupFailure?.let { throw AssertionError("Could not seed the test queue", it) }

        val emissions = Collections.synchronizedList(mutableListOf<QueueEmission>())
        val callbackOnMain = AtomicBoolean(false)
        val callbacksComplete = CountDownLatch(2)
        audioHelper.getQueuedTracksLazily { tracks, startIndex, startPositionMs ->
            callbackOnMain.set(Looper.myLooper() == Looper.getMainLooper())
            emissions.add(QueueEmission(tracks.map { it.mediaStoreId }, startIndex, startPositionMs))
            callbacksComplete.countDown()
        }

        assertTrue("Queue callbacks did not complete", callbacksComplete.await(10, TimeUnit.SECONDS))
        assertFalse("Queue callbacks must not run on main", callbackOnMain.get())
        assertEquals(2, emissions.size)
        assertEquals(QueueEmission(listOf(currentTrackId), 0, 12000L), emissions[0])
        assertEquals(QueueEmission(listOf(firstTrackId, currentTrackId), 1, 12000L), emissions[1])
    }

    private fun track(mediaStoreId: Long) = Track(
        id = 0,
        mediaStoreId = mediaStoreId,
        title = "Track $mediaStoreId",
        artist = "Test Artist",
        path = "/music/$mediaStoreId.mp3",
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

    private data class QueueEmission(val trackIds: List<Long>, val startIndex: Int, val startPositionMs: Long)
}
