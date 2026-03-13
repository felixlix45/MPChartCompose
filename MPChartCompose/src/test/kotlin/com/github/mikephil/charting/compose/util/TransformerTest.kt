package com.github.mikephil.charting.compose.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.*
import org.junit.Test

class TransformerTest {

    @Test
    fun `dataToPixel converts data point to pixel coordinates`() {
        val transformer = Transformer(
            contentRect = Rect(0f, 0f, 100f, 100f),
            dataXRange = 0f..10f,
            dataYRange = 0f..100f,
        )
        val pixel = transformer.dataToPixel(5f, 50f)
        assertEquals(50f, pixel.x, 0.5f)
        assertEquals(50f, pixel.y, 0.5f)
    }

    @Test
    fun `pixelToData converts pixel coordinates back to data space`() {
        val transformer = Transformer(
            contentRect = Rect(0f, 0f, 100f, 100f),
            dataXRange = 0f..10f,
            dataYRange = 0f..100f,
        )
        val data = transformer.pixelToData(Offset(50f, 50f))
        assertEquals(5f, data.first, 0.5f)
        assertEquals(50f, data.second, 0.5f)
    }

    @Test
    fun `dataToPixel handles Y-axis inversion`() {
        val transformer = Transformer(
            contentRect = Rect(0f, 0f, 100f, 100f),
            dataXRange = 0f..10f,
            dataYRange = 0f..100f,
        )
        val top = transformer.dataToPixel(0f, 100f)
        assertEquals(0f, top.y, 0.5f)
        val bottom = transformer.dataToPixel(0f, 0f)
        assertEquals(100f, bottom.y, 0.5f)
    }

    @Test
    fun `roundtrip conversion is identity`() {
        val transformer = Transformer(
            contentRect = Rect(20f, 10f, 200f, 150f),
            dataXRange = -5f..15f,
            dataYRange = 10f..90f,
        )
        val pixel = transformer.dataToPixel(3f, 45f)
        val (dx, dy) = transformer.pixelToData(pixel)
        assertEquals(3f, dx, 0.01f)
        assertEquals(45f, dy, 0.01f)
    }

    @Test
    fun `contentRect with offsets works correctly`() {
        val transformer = Transformer(
            contentRect = Rect(50f, 20f, 250f, 220f),
            dataXRange = 0f..100f,
            dataYRange = 0f..100f,
        )
        val bottomLeft = transformer.dataToPixel(0f, 0f)
        assertEquals(50f, bottomLeft.x, 0.5f)
        assertEquals(220f, bottomLeft.y, 0.5f)
    }
}
