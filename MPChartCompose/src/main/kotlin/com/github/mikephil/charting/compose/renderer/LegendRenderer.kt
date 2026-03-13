package com.github.mikephil.charting.compose.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.github.mikephil.charting.compose.data.LegendOrientation
import com.github.mikephil.charting.compose.data.LegendPosition

internal object LegendRenderer {

    data class LegendEntry(
        val label: String,
        val color: Color,
    )

    fun DrawScope.drawLegend(
        entries: List<LegendEntry>,
        position: LegendPosition,
        orientation: LegendOrientation,
        formSize: Dp,
        textColor: Color,
        textSize: TextUnit,
        textMeasurer: TextMeasurer,
        chartRect: Rect,
    ) {
        if (entries.isEmpty()) return

        val formSizePx = formSize.toPx()
        val spacing = formSizePx * 0.5f
        val style = TextStyle(color = textColor, fontSize = textSize)

        when (orientation) {
            LegendOrientation.Horizontal -> {
                var x = chartRect.left
                val y = when (position) {
                    LegendPosition.Top -> chartRect.top - formSizePx - spacing
                    LegendPosition.Bottom -> chartRect.bottom + spacing * 2
                    else -> chartRect.top - formSizePx - spacing
                }

                for (entry in entries) {
                    // Draw color form (small square)
                    drawRect(
                        color = entry.color,
                        topLeft = Offset(x, y),
                        size = Size(formSizePx, formSizePx),
                    )
                    x += formSizePx + spacing

                    // Draw label text
                    val measured = textMeasurer.measure(entry.label, style)
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(x, y + (formSizePx - measured.size.height) / 2f),
                    )
                    x += measured.size.width + spacing * 2
                }
            }
            LegendOrientation.Vertical -> {
                val x = when (position) {
                    LegendPosition.Left -> chartRect.left - formSizePx * 3
                    LegendPosition.Right -> chartRect.right + spacing
                    else -> chartRect.left
                }
                var y = chartRect.top

                for (entry in entries) {
                    drawRect(
                        color = entry.color,
                        topLeft = Offset(x, y),
                        size = Size(formSizePx, formSizePx),
                    )

                    val measured = textMeasurer.measure(entry.label, style)
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(x + formSizePx + spacing, y + (formSizePx - measured.size.height) / 2f),
                    )
                    y += formSizePx + spacing * 2
                }
            }
        }
    }
}
