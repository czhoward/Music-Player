package org.fossify.musicplayer.extensions

import android.app.Activity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.fossify.musicplayer.playback.library.MediaItemProviderTestApplication
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
@Config(application = MediaItemProviderTestApplication::class, sdk = [35])
class FolderTracksAwaitTest {
    @Test
    fun awaitsTheEmptyFolderCallback() = runBlocking {
        val activityController = Robolectric.buildActivity(FolderTracksTestActivity::class.java).setup()
        val folder = Files.createTempDirectory("empty-music-folder").toFile()

        try {
            val tracks = withContext(Dispatchers.IO) {
                activityController.get().awaitFolderTracks(folder.path, rescanWrongPaths = false)
            }

            assertTrue(tracks.isEmpty())
        } finally {
            folder.deleteRecursively()
            activityController.destroy()
        }
    }
}

class FolderTracksTestActivity : Activity()
