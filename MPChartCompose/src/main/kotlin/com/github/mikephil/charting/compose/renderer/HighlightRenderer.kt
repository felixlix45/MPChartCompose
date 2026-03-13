package com.github.mikephil.charting.compose.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.github.mikephil.charting.compose.highlight.Highlight
import com.github.mikephil.charting.compose.util.Transformer

internal object HighlightRenderer {

    fun DrawScope.drawHighlight(
        highlight: Highlight,
        transformer: Transformer,
        crosshairEnabled: Boolean,
        crosshairColor: Color,
        crosshairWidth: Dp,
        circleRadius: Dp,
        circleColor: Color,
        circleStrokeWidth: Dp,
        circleStrokeColor: Color,
    ) {
        val contentRect = transformer.contentRect
        val pos = highlight.pixelPosition

        // Crosshair lines
        if (crosshairEnabled) {
            val strokeWidthPx = crosshairWidth.toPx()

            // Vertical line
            drawLine(
                color = crosshairColor,
                start = Offset(pos.x, contentRect.top),
                end = Offset(pos.x, contentRect.bottom),
                strokeWidth = strokeWidthPx,
            )

            // Horizontal line
            drawLine(
                color = crosshairColor,
                start = Offset(contentRect.left, pos.y),
                end = Offset(contentRect.right, pos.y),
                strokeWidth = strokeWidthPx,
            )
        }

        // Scrub indicator circle
        val radiusPx = circleRadius.toPx()
        if (radiusPx > 0f) {
            // Outer stroke
            val strokeColor = if (circleStrokeColor != Color.Unspecified) circleStrokeColor else circleColor
            drawCircle(
                color = strokeColor,
                radius = radiusPx + circleStrokeWidth.toPx(),
                center = pos,
            )
            // Inner fill
            drawCircle(
                color = circleColor,
                radius = radiusPx,
                center = pos,
            )
        }
    }
}
