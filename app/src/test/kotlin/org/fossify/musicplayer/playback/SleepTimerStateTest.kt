package org.fossify.musicplayer.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class SleepTimerStateTest {
    @Test
    fun publishesActiveAndClearedTimerValues() {
        SleepTimerState.update(45)
        assertEquals(45, SleepTimerState.seconds.value)

        SleepTimerState.update(null)
        assertEquals(null, SleepTimerState.seconds.value)
    }
}
