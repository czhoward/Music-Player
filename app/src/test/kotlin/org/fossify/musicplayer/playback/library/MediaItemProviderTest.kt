package org.fossify.musicplayer.playback.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MediaItemProviderTest {
    @Test
    fun rootIsAvailableBeforeLibraryReload() {
        val provider = MediaItemProvider(RuntimeEnvironment.getApplication())
        var listenerCalled = false

        assertEquals("__ROOT__", provider.getRootItem().mediaId)
        assertFalse(provider.whenReady { listenerCalled = true })
        assertFalse(listenerCalled)
    }
}
