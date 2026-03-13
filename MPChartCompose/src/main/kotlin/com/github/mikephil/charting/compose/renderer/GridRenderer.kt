package com.github.mikephil.charting.compose.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import com.github.mikephil.charting.compose.util.Transformer

internal object GridRenderer {
    fun DrawScope.drawGrid(
        transformer: Transformer,
        xLabelValues: List<Float>,
        yLabelValues: List<Float>,
        gridColor: Color,
        gridLineWidth: Dp,
        drawXGrid: Boolean,
        drawYGrid: Boolean,
        axisLineColor: Color,
        drawXAxisLine: Boolean,
        drawYAxisLine: Boolean,
    ) {
        val strokeWidth = gridLineWidth.toPx()
        val contentRect = transformer.contentRect

        if (drawYGrid) {
            for (xVal in xLabelValues) {
                val px = transformer.dataToPixel(xVal, 0f).x
                if (px in contentRect.left..contentRect.right) {
                    drawLine(
                        color = gridColor,
                        start = Offset(px, contentRect.top),
                        end = Offset(px, contentRect.bottom),
                        strokeWidth = strokeWidth,
                    )
                }
            }
        }

        if (drawXGrid) {
            for (yVal in yLabelValues) {
                val py = transformer.dataToPixel(0f, yVal).y
                if (py in contentRect.top..contentRect.bottom) {
                    drawLine(
                        color = gridColor,
                        start = Offset(contentRect.left, py),
                        end = Offset(contentRect.right, py),
                        strokeWidth = strokeWidth,
                    )
                }
            }
        }

        if (drawXAxisLine) {
            drawLine(
                color = axisLineColor,
                start = Offset(contentRect.left, contentRect.bottom),
                end = Offset(contentRect.right, contentRect.bottom),
                strokeWidth = strokeWidth * 2,
            )
        }
        if (drawYAxisLine) {
            drawLine(
                color = axisLineColor,
                start = Offset(contentRect.left, contentRect.top),
                end = Offset(contentRect.left, contentRect.bottom),
                strokeWidth = strokeWidth * 2,
            )
        }
    }
}
