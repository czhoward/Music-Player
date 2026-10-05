package org.fossify.musicplayer.extensions

import com.google.common.util.concurrent.SettableFuture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FutureTest {
    @Test
    fun listenerReceivesValueWhenFutureCompletes() {
        val future = SettableFuture.create<String>()
        var result: String? = null

        future.addListenerWithResult { result = it }

        assertNull(result)
        future.set("ready")

        assertEquals("ready", result)
    }
}
