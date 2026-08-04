package com.github.mikephil.charting.compose.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.TextUnit
import com.github.mikephil.charting.compose.util.Transformer

internal object AxisLabelRenderer {

    fun DrawScope.drawXAxisLabels(
        transformer: Transformer,
        labelValues: List<Float>,
        formatter: ((Float) -> String)?,
        textMeasurer: TextMeasurer,
        textColor: Color,
        textSize: TextUnit,
        bottomOffset: Float = 4f,
    ) {
        val style = TextStyle(color = textColor, fontSize = textSize)
        val contentRect = transformer.contentRect

        for (value in labelValues) {
            val text = formatter?.invoke(value) ?: value.toString()
            val measured = textMeasurer.measure(text, style)
            val px = transformer.dataToPixel(value, 0f).x
            val x = (px - measured.size.width / 2f).coerceIn(
                contentRect.left,
                contentRect.right - measured.size.width
            )
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(x, contentRect.bottom + bottomOffset),
            )
        }
    }

    fun DrawScope.drawYAxisLabels(
        transformer: Transformer,
        labelValues: List<Float>,
        formatter: ((Float) -> String)?,
        textMeasurer: TextMeasurer,
        textColor: Color,
        textSize: TextUnit,
        yAxisOnRight: Boolean = false,
        labelOffset: Float = 4f,
    ) {
        val style = TextStyle(color = textColor, fontSize = textSize)
        val contentRect = transformer.contentRect

        for (value in labelValues) {
            val text = formatter?.invoke(value) ?: value.toString()
            val measured = textMeasurer.measure(text, style)
            val py = transformer.dataToPixel(0f, value).y
            val y = (py - measured.size.height / 2f).coerceIn(
                contentRect.top,
                contentRect.bottom - measured.size.height
            )
            val x = if (yAxisOnRight) {
                contentRect.right + labelOffset
            } else {
                contentRect.left - measured.size.width - labelOffset
            }
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(x, y),
            )
        }
    }
}
