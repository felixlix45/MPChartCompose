package com.github.mikephil.charting.compose.data

import org.junit.Assert.*
import org.junit.Test

class EntryTest {

    @Test
    fun `Entry equality by value`() {
        val a = Entry(1f, 2f)
        val b = Entry(1f, 2f)
        assertEquals(a, b)
    }

    @Test
    fun `Entry with data field`() {
        val entry = Entry(1f, 2f, data = "tag")
        assertEquals("tag", entry.data)
    }

    @Test
    fun `BarEntry basic construction`() {
        val entry = BarEntry(x = 1f, y = 10f)
        assertEquals(1f, entry.x, 0.001f)
        assertEquals(10f, entry.y, 0.001f)
        assertNull(entry.stackValues)
    }

    @Test
    fun `BarEntry with stackValues derives y as sum`() {
        val entry = BarEntry(x = 1f, y = 0f, stackValues = listOf(3f, 4f, 5f))
        assertEquals(12f, entry.effectiveY, 0.001f)
    }

    @Test
    fun `BarEntry stackValues equality uses structural comparison`() {
        val a = BarEntry(x = 1f, y = 0f, stackValues = listOf(1f, 2f))
        val b = BarEntry(x = 1f, y = 0f, stackValues = listOf(1f, 2f))
        assertEquals(a, b)
    }

    @Test
    fun `BarEntry with data field`() {
        val entry = BarEntry(x = 1f, y = 5f, data = 42)
        assertEquals(42, entry.data)
    }

    @Test
    fun `CandleEntry construction`() {
        val entry = CandleEntry(x = 1f, open = 10f, high = 15f, low = 8f, close = 12f)
        assertEquals(10f, entry.open, 0.001f)
        assertEquals(15f, entry.high, 0.001f)
        assertEquals(8f, entry.low, 0.001f)
        assertEquals(12f, entry.close, 0.001f)
    }

    @Test
    fun `CandleEntry equality`() {
        val a = CandleEntry(1f, 10f, 15f, 8f, 12f)
        val b = CandleEntry(1f, 10f, 15f, 8f, 12f)
        assertEquals(a, b)
    }

    @Test
    fun `CandleEntry with data field`() {
        val entry = CandleEntry(1f, 10f, 15f, 8f, 12f, data = "candle")
        assertEquals("candle", entry.data)
    }
}
