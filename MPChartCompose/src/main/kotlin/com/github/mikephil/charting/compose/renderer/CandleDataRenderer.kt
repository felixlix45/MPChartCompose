package com.github.mikephil.charting.compose.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.github.mikephil.charting.compose.data.CandleDataSet
import com.github.mikephil.charting.compose.util.Transformer

internal object CandleDataRenderer {

    fun DrawScope.drawCandleData(
        dataSets: List<CandleDataSet>,
        transformer: Transformer,
        appearPhase: Float,
    ) {
        val dataXRange = transformer.dataXRange
        val xSpan = dataXRange.endInclusive - dataXRange.start
        if (xSpan <= 0f) return

        val contentWidth = transformer.contentRect.width

        for (dataSet in dataSets) {
            val bodyWidthPx = (dataSet.bodyWidth / xSpan) * contentWidth

            for (entry in dataSet.entries) {
                val isIncreasing = entry.close >= entry.open
                val bodyColor = if (isIncreasing) dataSet.increasingColor else dataSet.decreasingColor

                val centerPixel = transformer.dataToPixel(entry.x, 0f)
                val centerX = centerPixel.x

                // Apply appear phase (expand from center)
                val midPrice = (entry.open + entry.close) / 2f
                val open = midPrice + (entry.open - midPrice) * appearPhase
                val close = midPrice + (entry.close - midPrice) * appearPhase
                val high = midPrice + (entry.high - midPrice) * appearPhase
                val low = midPrice + (entry.low - midPrice) * appearPhase

                // Shadow/wick line (high to low)
                val highPixel = transformer.dataToPixel(entry.x, high)
                val lowPixel = transformer.dataToPixel(entry.x, low)
                drawLine(
                    color = dataSet.shadowColor,
                    start = Offset(centerX, highPixel.y),
                    end = Offset(centerX, lowPixel.y),
                    strokeWidth = 1f,
                )

                // Body rectangle (open to close)
                val openPixel = transformer.dataToPixel(entry.x, open)
                val closePixel = transformer.dataToPixel(entry.x, close)
                val bodyTop = minOf(openPixel.y, closePixel.y)
                val bodyBottom = maxOf(openPixel.y, closePixel.y)
                val bodyHeight = (bodyBottom - bodyTop).coerceAtLeast(1f) // Min 1px

                val bodyRect = Rect(
                    Offset(centerX - bodyWidthPx / 2f, bodyTop),
                    Size(bodyWidthPx, bodyHeight),
                )

                drawRect(
                    color = bodyColor,
                    topLeft = bodyRect.topLeft,
                    size = bodyRect.size,
                )
            }
        }
    }
}
