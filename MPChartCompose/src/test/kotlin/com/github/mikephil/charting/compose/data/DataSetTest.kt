package com.github.mikephil.charting.compose.data

import org.junit.Assert.*
import org.junit.Test

class DataSetTest {

    @Test
    fun `LineDataSet default values`() {
        val ds = LineDataSet(entries = emptyList(), label = "test")
        assertEquals("test", ds.label)
        assertTrue(ds.drawCircles)
        assertEquals(LineMode.Linear, ds.mode)
        assertFalse(ds.fillEnabled)
        assertNull(ds.glow)
    }

    @Test
    fun `BarDataSet default values`() {
        val ds = BarDataSet(entries = emptyList(), label = "bars")
        assertEquals("bars", ds.label)
        assertNull(ds.barBrush)
    }

    @Test
    fun `CandleDataSet default colors`() {
        val ds = CandleDataSet(entries = emptyList(), label = "candles")
        assertEquals(0.7f, ds.bodyWidth, 0.001f)
    }

    @Test
    fun `LineData holds multiple datasets`() {
        val ds1 = LineDataSet(entries = listOf(Entry(0f, 1f)), label = "A")
        val ds2 = LineDataSet(entries = listOf(Entry(0f, 2f)), label = "B")
        val data = LineData(dataSets = listOf(ds1, ds2))
        assertEquals(2, data.dataSets.size)
    }

    @Test
    fun `BarData barWidth default`() {
        val data = BarData(dataSets = emptyList())
        assertEquals(0.85f, data.barWidth, 0.001f)
    }

    @Test
    fun `CandleData holds datasets`() {
        val data = CandleData(dataSets = emptyList())
        assertTrue(data.dataSets.isEmpty())
    }

    @Test
    fun `LineGlow default values`() {
        val glow = LineGlow()
        assertTrue(glow.enabled)
        assertEquals(0.4f, glow.alpha, 0.001f)
    }

    @Test
    fun `LimitLine default values`() {
        val line = LimitLine(value = 100f)
        assertEquals(100f, line.value, 0.001f)
        assertEquals("", line.label)
        assertNull(line.dashEffect)
    }

    @Test
    fun `LineMode enum values`() {
        val modes = LineMode.values()
        assertEquals(4, modes.size)
        assertTrue(modes.contains(LineMode.Linear))
        assertTrue(modes.contains(LineMode.CubicBezier))
        assertTrue(modes.contains(LineMode.HorizontalBezier))
        assertTrue(modes.contains(LineMode.Stepped))
    }

    @Test
    fun `AxisPosition enum values`() {
        assertEquals(2, AxisPosition.values().size)
    }
}
