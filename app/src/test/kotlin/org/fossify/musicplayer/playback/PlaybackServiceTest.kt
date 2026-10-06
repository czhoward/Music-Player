package org.fossify.musicplayer.playback

import android.app.NotificationManager
import org.fossify.musicplayer.helpers.NotificationHelper
import org.fossify.musicplayer.playback.library.MediaItemProviderTestApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
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

    @Test
    fun foregroundStartBlockedPostsFallbackNotification() {
        val serviceController = Robolectric.buildService(PlaybackService::class.java).create()
        val service = serviceController.get()

        service.onForegroundServiceStartNotAllowedException()

        val notificationManager = service.getSystemService(NotificationManager::class.java)
        val notification = shadowOf(notificationManager).getNotification(NotificationHelper.NOTIFICATION_ID)
        assertNotNull(notification)

        serviceController.destroy()
    }
}
