package org.fossify.musicplayer.playback.player

import androidx.media3.common.MediaItem
import org.fossify.musicplayer.extensions.currentMediaItems
import org.fossify.musicplayer.extensions.shuffledMediaItemsIndices
import org.fossify.musicplayer.playback.PlaybackService
import org.fossify.musicplayer.playback.library.MediaItemProviderTestApplication
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = MediaItemProviderTestApplication::class, sdk = [35])
class SimpleMusicPlayerTest {
    @Test
    fun insertsNewItemImmediatelyAfterCurrentItem() {
        val serviceController = Robolectric.buildService(PlaybackService::class.java).create()
        val player = serviceController.get().player
        player.setMediaItems(listOf(mediaItem("1"), mediaItem("3")))

        @Suppress("DEPRECATION")
        player.setNextMediaItem(mediaItem("2"))

        assertEquals(listOf("1", "2", "3"), player.currentMediaItems.map { it.mediaId })
        serviceController.destroy()
    }

    @Test
    fun movesExistingItemToNextPosition() {
        val serviceController = Robolectric.buildService(PlaybackService::class.java).create()
        val player = serviceController.get().player
        player.setMediaItems(
            listOf(mediaItem("1"), mediaItem("2"), mediaItem("3"), mediaItem("4"))
        )

        @Suppress("DEPRECATION")
        player.setNextMediaItem(mediaItem("4"))

        assertEquals(
            listOf("1", "4", "2", "3"),
            player.currentMediaItems.map { it.mediaId }
        )
        serviceController.destroy()
    }

    @Test
    fun movesExistingItemToNextPositionInShuffleOrder() {
        val serviceController = Robolectric.buildService(PlaybackService::class.java).create()
        val player = serviceController.get().player
        player.setMediaItems(listOf(mediaItem("1"), mediaItem("2"), mediaItem("3"), mediaItem("4")))
        player.shuffleModeEnabled = true

        @Suppress("DEPRECATION")
        player.setShuffleIndices(intArrayOf(0, 2, 1, 3))

        @Suppress("DEPRECATION")
        player.setNextMediaItem(mediaItem("2"))

        assertEquals(listOf(0, 1, 2, 3), player.shuffledMediaItemsIndices)
        serviceController.destroy()
    }

    private fun mediaItem(id: String) = MediaItem.Builder()
        .setMediaId(id)
        .setUri("file:///music/$id.mp3")
        .build()
}
