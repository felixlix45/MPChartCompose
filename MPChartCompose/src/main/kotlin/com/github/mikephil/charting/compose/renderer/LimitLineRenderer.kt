package com.github.mikephil.charting.compose.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import com.github.mikephil.charting.compose.data.LimitLine
import com.github.mikephil.charting.compose.util.Transformer

internal object LimitLineRenderer {
    fun DrawScope.drawLimitLines(
        transformer: Transformer,
        limitLines: List<LimitLine>,
        textMeasurer: TextMeasurer,
    ) {
        val contentRect = transformer.contentRect

        for (line in limitLines) {
            val py = transformer.dataToPixel(0f, line.value).y
            if (py !in contentRect.top..contentRect.bottom) continue

            drawLine(
                color = line.color,
                start = Offset(contentRect.left, py),
                end = Offset(contentRect.right, py),
                strokeWidth = line.lineWidth.toPx(),
                pathEffect = line.dashEffect,
            )

            if (line.label.isNotEmpty()) {
                val style = TextStyle(color = line.color, fontSize = 10.sp)
                val measured = textMeasurer.measure(line.label, style)
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(contentRect.left + 4f, py - measured.size.height - 2f),
                )
            }
        }
    }
}
