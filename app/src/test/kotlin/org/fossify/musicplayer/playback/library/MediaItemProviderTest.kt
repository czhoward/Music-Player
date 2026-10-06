package org.fossify.musicplayer.playback.library

import android.app.Application
import android.os.Looper
import org.fossify.musicplayer.extensions.tracksDAO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.fossify.musicplayer.models.Track
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(application = MediaItemProviderTestApplication::class, sdk = [35])
class MediaItemProviderTest {
    @Test
    fun rootIsAvailableBeforeLibraryReload() {
        val provider = MediaItemProvider(RuntimeEnvironment.getApplication())
        var listenerCalled = false

        assertEquals("__ROOT__", provider.getRootItem().mediaId)
        assertFalse(provider.whenReady { listenerCalled = true })
        assertFalse(listenerCalled)
    }

    @Test
    fun reloadSignalsReadyAfterLibraryIsBuilt() {
        val provider = MediaItemProvider(RuntimeEnvironment.getApplication())
        val ready = CountDownLatch(1)
        var succeeded: Boolean? = null

        assertFalse(provider.whenReady {
            succeeded = it
            ready.countDown()
        })
        provider.reload()

        val mainLooper = shadowOf(Looper.getMainLooper())
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20)
        while (ready.count > 0 && System.nanoTime() < deadline) {
            mainLooper.idle()
            ready.await(10, TimeUnit.MILLISECONDS)
        }

        assertTrue(ready.count == 0L)
        assertEquals(true, succeeded)
    }

    @Test
    fun reloadPublishesTracksFromDatabase() {
        val context = RuntimeEnvironment.getApplication()
        val mediaStoreId = System.nanoTime()
        val title = "Library reload test $mediaStoreId"
        val track = Track(
            id = 0,
            mediaStoreId = mediaStoreId,
            title = title,
            artist = "Test Artist",
            path = "/music/reload-test.mp3",
            duration = 1000,
            album = "Test Album",
            genre = "Test Genre",
            coverArt = "",
            playListId = 0,
            trackId = 1,
            discNumber = 1,
            folderName = "Test Folder",
            albumId = 0,
            artistId = 0,
            genreId = 0,
            year = 2026,
            dateAdded = 0,
            orderInPlaylist = 0
        )
        var insertFailure: Throwable? = null
        val inserted = CountDownLatch(1)
        Thread {
            try {
                context.tracksDAO.insert(track)
            } catch (throwable: Throwable) {
                insertFailure = throwable
            } finally {
                inserted.countDown()
            }
        }.start()

        assertTrue(inserted.await(10, TimeUnit.SECONDS))
        insertFailure?.let { throw AssertionError("Could not seed the test track", it) }

        val provider = MediaItemProvider(context)
        val ready = CountDownLatch(1)
        var succeeded: Boolean? = null
        assertFalse(provider.whenReady {
            succeeded = it
            ready.countDown()
        })
        provider.reload()
        awaitReady(ready)

        assertEquals(true, succeeded)
        assertEquals(mediaStoreId.toString(), provider.getItemFromSearch(title.lowercase())?.mediaId)
    }

    private fun awaitReady(ready: CountDownLatch) {
        val mainLooper = shadowOf(Looper.getMainLooper())
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20)
        while (ready.count > 0 && System.nanoTime() < deadline) {
            mainLooper.idle()
            ready.await(10, TimeUnit.MILLISECONDS)
        }
        assertTrue(ready.count == 0L)
    }
}

class MediaItemProviderTestApplication : Application()
