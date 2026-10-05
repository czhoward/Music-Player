package org.fossify.musicplayer.playback.library

import android.app.Application
import android.os.Looper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
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
}

class MediaItemProviderTestApplication : Application()
