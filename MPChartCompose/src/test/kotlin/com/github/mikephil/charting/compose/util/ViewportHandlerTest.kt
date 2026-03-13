package com.github.mikephil.charting.compose.util

import com.github.mikephil.charting.compose.data.Entry
import org.junit.Assert.*
import org.junit.Test

class ViewportHandlerTest {

    @Test
    fun `computeVisibleRange shows all data when visibleXRange is null`() {
        val vp = ViewportHandler(visibleXRange = null, translationX = 0f)
        val range = vp.computeVisibleXRange(dataXMin = 0f, dataXMax = 20f)
        assertEquals(0f, range.start, 0.01f)
        assertEquals(20f, range.endInclusive, 0.01f)
    }

    @Test
    fun `computeVisibleRange limits to visibleXRange from the end`() {
        val vp = ViewportHandler(visibleXRange = 10f, translationX = 0f)
        val range = vp.computeVisibleXRange(dataXMin = 0f, dataXMax = 20f)
        assertEquals(10f, range.start, 0.01f)
        assertEquals(20f, range.endInclusive, 0.01f)
    }

    @Test
    fun `translationX pans the visible range`() {
        val vp = ViewportHandler(visibleXRange = 10f, translationX = -5f)
        val range = vp.computeVisibleXRange(dataXMin = 0f, dataXMax = 20f)
        assertEquals(5f, range.start, 0.01f)
        assertEquals(15f, range.endInclusive, 0.01f)
    }

    @Test
    fun `computeVisibleYRange with auto-scale`() {
        val entries = listOf(Entry(5f, 10f), Entry(6f, 30f), Entry(7f, 20f))
        val vp = ViewportHandler(visibleXRange = null, translationX = 0f)
        val yRange = vp.computeVisibleYRange(
            entries = entries,
            visibleXRange = 0f..20f,
            yAutoScale = true,
            yMin = null,
            yMax = null,
        )
        assertEquals(10f, yRange.start, 0.01f)
        assertEquals(30f, yRange.endInclusive, 0.01f)
    }

    @Test
    fun `computeVisibleYRange with fixed min and max`() {
        val entries = listOf(Entry(5f, 10f), Entry(6f, 30f))
        val vp = ViewportHandler(visibleXRange = null, translationX = 0f)
        val yRange = vp.computeVisibleYRange(
            entries = entries,
            visibleXRange = 0f..20f,
            yAutoScale = true,
            yMin = 0f,
            yMax = 50f,
        )
        assertEquals(0f, yRange.start, 0.01f)
        assertEquals(50f, yRange.endInclusive, 0.01f)
    }

    @Test
    fun `isAtEnd returns true when viewport shows latest data`() {
        val vp = ViewportHandler(visibleXRange = 10f, translationX = 0f)
        assertTrue(vp.isAtEnd(dataXMax = 20f))
    }

    @Test
    fun `isAtEnd returns false when panned away`() {
        val vp = ViewportHandler(visibleXRange = 10f, translationX = -5f)
        assertFalse(vp.isAtEnd(dataXMax = 20f))
    }
}
