package org.fossify.musicplayer.playback

import org.fossify.musicplayer.playback.library.MediaItemProviderTestApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = MediaItemProviderTestApplication::class, sdk = [35])
class PlaybackServiceTest {
    @Test
    fun createInitializesPlayerSessionAndLibraryRoot() {
        val serviceController = Robolectric.buildService(PlaybackService::class.java).create()
        val service = serviceController.get()

        assertNotNull(service.player)
        assertNotNull(service.mediaSession)
        assertEquals("__ROOT__", service.mediaItemProvider.getRootItem().mediaId)

        serviceController.destroy()
    }
}
