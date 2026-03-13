package com.github.mikephil.charting.compose.util

import com.github.mikephil.charting.compose.data.Entry
import org.junit.Assert.*
import org.junit.Test

class EntryFinderTest {

    @Test
    fun `findNearest returns closest entry by x`() {
        val entries = listOf(Entry(0f, 1f), Entry(5f, 2f), Entry(10f, 3f))
        val result = EntryFinder.findNearest(entries, targetX = 4f)
        assertEquals(Entry(5f, 2f), result)
    }

    @Test
    fun `findNearest returns first when equidistant`() {
        val entries = listOf(Entry(0f, 1f), Entry(10f, 2f))
        val result = EntryFinder.findNearest(entries, targetX = 5f)
        assertEquals(Entry(0f, 1f), result)
    }

    @Test
    fun `findNearest returns null for empty list`() {
        val result = EntryFinder.findNearest(emptyList(), targetX = 5f)
        assertNull(result)
    }

    @Test
    fun `findNearest with single entry`() {
        val entries = listOf(Entry(5f, 10f))
        val result = EntryFinder.findNearest(entries, targetX = 100f)
        assertEquals(Entry(5f, 10f), result)
    }

    @Test
    fun `findNearestIndex returns index`() {
        val entries = listOf(Entry(0f, 1f), Entry(5f, 2f), Entry(10f, 3f))
        val idx = EntryFinder.findNearestIndex(entries, targetX = 8f)
        assertEquals(2, idx)
    }

    @Test
    fun `findNearestIndex returns -1 for empty list`() {
        val idx = EntryFinder.findNearestIndex(emptyList(), targetX = 5f)
        assertEquals(-1, idx)
    }
}
