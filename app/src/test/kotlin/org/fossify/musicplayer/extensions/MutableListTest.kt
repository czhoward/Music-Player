package org.fossify.musicplayer.extensions

import org.junit.Assert.assertEquals
import org.junit.Test

class MutableListTest {
    @Test
    fun moveItemBeforeLaterTarget() {
        val items = mutableListOf("current", "queued", "next", "last")

        items.move(currentIndex = 1, newIndex = 3)

        assertEquals(listOf("current", "next", "queued", "last"), items)
    }

    @Test
    fun moveItemBeforeEarlierTarget() {
        val items = mutableListOf("current", "queued", "next", "last")

        items.move(currentIndex = 3, newIndex = 1)

        assertEquals(listOf("current", "last", "queued", "next"), items)
    }
}
