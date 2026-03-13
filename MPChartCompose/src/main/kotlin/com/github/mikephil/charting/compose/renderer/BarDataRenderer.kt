package com.github.mikephil.charting.compose.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.github.mikephil.charting.compose.data.BarDataSet
import com.github.mikephil.charting.compose.data.BarEntry
import com.github.mikephil.charting.compose.util.Transformer

internal object BarDataRenderer {

    fun DrawScope.drawBarData(
        dataSets: List<BarDataSet>,
        transformer: Transformer,
        barWidth: Float,
        appearPhase: Float,
        defaultBarColor: Color,
    ) {
        val dataXRange = transformer.dataXRange
        val xSpan = dataXRange.endInclusive - dataXRange.start
        if (xSpan <= 0f) return

        val contentWidth = transformer.contentRect.width
        val barWidthPx = (barWidth / xSpan) * contentWidth

        for (dataSet in dataSets) {
            val barColor = if (dataSet.barColor != Color.Unspecified) dataSet.barColor else defaultBarColor

            for (entry in dataSet.entries) {
                if (entry.stackValues != null) {
                    drawStackedBar(entry, transformer, barWidthPx, appearPhase, barColor, dataSet.barBrush)
                } else {
                    drawSingleBar(entry, transformer, barWidthPx, appearPhase, barColor, dataSet.barBrush)
                }
            }
        }
    }

    private fun DrawScope.drawSingleBar(
        entry: BarEntry,
        transformer: Transformer,
        barWidthPx: Float,
        appearPhase: Float,
        color: Color,
        brush: Brush?,
    ) {
        val top = transformer.dataToPixel(entry.x, entry.y * appearPhase)
        val baseline = transformer.dataToPixel(entry.x, 0f)
        val left = top.x - barWidthPx / 2f
        val barTop = if (entry.y >= 0f) top.y else baseline.y
        val barBottom = if (entry.y >= 0f) baseline.y else top.y

        val rect = Rect(Offset(left, barTop), Size(barWidthPx, barBottom - barTop))

        if (brush != null) {
            drawRect(brush = brush, topLeft = rect.topLeft, size = rect.size)
        } else {
            drawRect(color = color, topLeft = rect.topLeft, size = rect.size)
        }
    }

    private fun DrawScope.drawStackedBar(
        entry: BarEntry,
        transformer: Transformer,
        barWidthPx: Float,
        appearPhase: Float,
        color: Color,
        brush: Brush?,
    ) {
        val stackValues = entry.stackValues ?: return
        var posY = 0f
        var negY = 0f
        val centerX = transformer.dataToPixel(entry.x, 0f).x
        val left = centerX - barWidthPx / 2f

        for (value in stackValues) {
            val scaledValue = value * appearPhase
            if (scaledValue >= 0f) {
                val segTop = transformer.dataToPixel(0f, posY + scaledValue).y
                val segBottom = transformer.dataToPixel(0f, posY).y
                val rect = Rect(Offset(left, segTop), Size(barWidthPx, segBottom - segTop))
                if (brush != null) {
                    drawRect(brush = brush, topLeft = rect.topLeft, size = rect.size)
                } else {
                    drawRect(color = color, topLeft = rect.topLeft, size = rect.size)
                }
                posY += scaledValue
            } else {
                val segTop = transformer.dataToPixel(0f, negY).y
                val segBottom = transformer.dataToPixel(0f, negY + scaledValue).y
                val rect = Rect(Offset(left, segTop), Size(barWidthPx, segBottom - segTop))
                if (brush != null) {
                    drawRect(brush = brush, topLeft = rect.topLeft, size = rect.size)
                } else {
                    drawRect(color = color, topLeft = rect.topLeft, size = rect.size)
                }
                negY += scaledValue
            }
        }
    }
}
