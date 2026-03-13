package com.github.mikephil.charting.compose.state

import com.github.mikephil.charting.compose.data.*
import org.junit.Assert.*
import org.junit.Test

class ChartStateTest {

    @Test
    fun `initial data is accessible`() {
        val initial = LineData(dataSets = listOf(
            LineDataSet(entries = listOf(Entry(0f, 1f)), label = "A")
        ))
        val state = LineChartStateImpl(capacity = 100, initialData = initial)
        assertEquals(1, state.data.dataSets.size)
        assertEquals(1, state.data.dataSets[0].entries.size)
    }

    @Test
    fun `append adds entry to dataset`() {
        val initial = LineData(dataSets = listOf(
            LineDataSet(entries = listOf(Entry(0f, 1f)), label = "A")
        ))
        val state = LineChartStateImpl(capacity = 100, initialData = initial)
        state.appendImmediate(0, Entry(1f, 2f))
        assertEquals(2, state.data.dataSets[0].entries.size)
    }

    @Test
    fun `append respects ring buffer capacity`() {
        val initial = LineData(dataSets = listOf(
            LineDataSet(entries = emptyList(), label = "A")
        ))
        val state = LineChartStateImpl(capacity = 3, initialData = initial)
        state.appendImmediate(0, Entry(0f, 1f))
        state.appendImmediate(0, Entry(1f, 2f))
        state.appendImmediate(0, Entry(2f, 3f))
        state.appendImmediate(0, Entry(3f, 4f))
        val entries = state.data.dataSets[0].entries
        assertEquals(3, entries.size)
        assertEquals(1f, entries[0].x, 0.01f)
    }

    @Test
    fun `replaceData swaps all data`() {
        val initial = LineData(dataSets = listOf(
            LineDataSet(entries = listOf(Entry(0f, 1f)), label = "A")
        ))
        val state = LineChartStateImpl(capacity = 100, initialData = initial)
        val newData = LineData(dataSets = listOf(
            LineDataSet(entries = listOf(Entry(10f, 20f), Entry(11f, 21f)), label = "B")
        ))
        state.replaceData(newData)
        assertEquals("B", state.data.dataSets[0].label)
        assertEquals(2, state.data.dataSets[0].entries.size)
    }

    @Test
    fun `clear empties all data`() {
        val initial = LineData(dataSets = listOf(
            LineDataSet(entries = listOf(Entry(0f, 1f)), label = "A")
        ))
        val state = LineChartStateImpl(capacity = 100, initialData = initial)
        state.clear()
        assertTrue(state.data.dataSets[0].entries.isEmpty())
    }
}
