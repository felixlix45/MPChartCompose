package com.github.mikephil.charting.compose.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.TextUnit
import com.github.mikephil.charting.compose.data.Entry
import com.github.mikephil.charting.compose.scope.MinMaxLabelScope
import com.github.mikephil.charting.compose.util.Transformer

internal object MinMaxLabelRenderer {

    fun DrawScope.drawMinMaxLabels(
        maxEntry: Entry,
        minEntry: Entry,
        transformer: Transformer,
        config: MinMaxLabelScope,
        textMeasurer: TextMeasurer,
        defaultColor: Color,
        defaultTextSize: TextUnit,
    ) {
        if (!config.enabled) return

        val contentRect = transformer.contentRect
        val textSize = if (config.textSize != TextUnit.Unspecified) config.textSize else defaultTextSize

        // Max label — pinned to the top of the content area, centered on the highest data point's x
        val maxColor = if (config.maxTextColor != Color.Unspecified) config.maxTextColor else defaultColor
        val maxText = config.formatter?.invoke(maxEntry.y) ?: maxEntry.y.toString()
        val maxMeasured = textMeasurer.measure(maxText, TextStyle(color = maxColor, fontSize = textSize, fontWeight = config.fontWeight))
        val maxPixelX = transformer.dataToPixel(maxEntry.x, maxEntry.y).x
        val maxX = (maxPixelX - maxMeasured.size.width / 2f)
            .coerceIn(contentRect.left, contentRect.right - maxMeasured.size.width)
        drawText(maxMeasured, topLeft = Offset(maxX, contentRect.top + 4f))

        // Min label — pinned to the bottom of the content area, centered on the lowest data point's x
        val minColor = if (config.minTextColor != Color.Unspecified) config.minTextColor else defaultColor
        val minText = config.formatter?.invoke(minEntry.y) ?: minEntry.y.toString()
        val minMeasured = textMeasurer.measure(minText, TextStyle(color = minColor, fontSize = textSize, fontWeight = config.fontWeight))
        val minPixelX = transformer.dataToPixel(minEntry.x, minEntry.y).x
        val minX = (minPixelX - minMeasured.size.width / 2f)
            .coerceIn(contentRect.left, contentRect.right - minMeasured.size.width)
        drawText(minMeasured, topLeft = Offset(minX, contentRect.bottom - minMeasured.size.height - 4f))
    }
}
