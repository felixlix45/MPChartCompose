package com.github.mikephil.charting.compose.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

class Transformer(
    val contentRect: Rect,
    val dataXRange: ClosedFloatingPointRange<Float>,
    val dataYRange: ClosedFloatingPointRange<Float>,
) {
    private val xSpan get() = dataXRange.endInclusive - dataXRange.start
    private val ySpan get() = dataYRange.endInclusive - dataYRange.start
    private val width get() = contentRect.width
    private val height get() = contentRect.height

    fun dataToPixel(dataX: Float, dataY: Float): Offset {
        val px = contentRect.left + ((dataX - dataXRange.start) / xSpan) * width
        val py = contentRect.bottom - ((dataY - dataYRange.start) / ySpan) * height
        return Offset(px, py)
    }

    fun pixelToData(pixel: Offset): Pair<Float, Float> {
        val dataX = dataXRange.start + ((pixel.x - contentRect.left) / width) * xSpan
        val dataY = dataYRange.start + ((contentRect.bottom - pixel.y) / height) * ySpan
        return dataX to dataY
    }
}
