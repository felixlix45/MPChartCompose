package com.github.mikephil.charting.compose.chart

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import com.github.mikephil.charting.compose.highlight.Highlight
import com.github.mikephil.charting.compose.marker.DynamicXAxisLabel
import com.github.mikephil.charting.compose.marker.DynamicYAxisLabel
import com.github.mikephil.charting.compose.marker.MarkerPositioner
import com.github.mikephil.charting.compose.renderer.BackgroundRenderer
import com.github.mikephil.charting.compose.renderer.LegendRenderer
import com.github.mikephil.charting.compose.scope.GestureScope
import com.github.mikephil.charting.compose.scope.HighlightScope
import com.github.mikephil.charting.compose.scope.LegendScope
import com.github.mikephil.charting.compose.scope.XAxisLabelPosition
import com.github.mikephil.charting.compose.scope.ViewportScope
import com.github.mikephil.charting.compose.scope.XAxisScope
import com.github.mikephil.charting.compose.scope.YAxisScope
import com.github.mikephil.charting.compose.style.ChartStyle
import com.github.mikephil.charting.compose.util.Transformer

@Composable
internal fun ChartCore(
    modifier: Modifier,
    style: ChartStyle,
    xAxisConfig: XAxisScope,
    yAxisConfig: YAxisScope,
    gestureConfig: GestureScope,
    highlightConfig: HighlightScope,
    legendConfig: LegendScope,
    viewportConfig: ViewportScope,
    highlightState: MutableState<Highlight?>,
    legendEntries: List<LegendRenderer.LegendEntry>,
    gestureModifier: Modifier,
    drawData: DrawScope.(Transformer, TextMeasurer) -> Unit,
    markerContent: (@Composable () -> Unit)?,
) {
    var chartSize by remember { mutableStateOf(IntSize.Zero) }
    val textMeasurer = rememberTextMeasurer()

    // Compute content rect margins for axis labels
    val leftMargin = if (yAxisConfig.drawLabels) 48f else 0f
    val bottomMargin = if (xAxisConfig.drawLabels) 32f else 0f
    val topMargin = if (legendConfig.enabled) 24f else 8f
    val rightMargin = 8f

    Box(
        modifier = modifier
            .onSizeChanged { chartSize = it }
            .then(gestureModifier)
            .drawBehind {
                if (chartSize == IntSize.Zero) return@drawBehind

                val contentRect = Rect(
                    left = leftMargin,
                    top = topMargin,
                    right = size.width - rightMargin,
                    bottom = size.height - bottomMargin,
                )

                // Layer 1: Background
                with(BackgroundRenderer) { drawBackground(style.backgroundColor) }

                // Draw data — the caller provides the real transformer
                drawData(
                    Transformer(contentRect, 0f..1f, 0f..1f),
                    textMeasurer,
                )
            }
    ) {
        // Overlay composition layer for markers
        val highlight = highlightState.value
        if (highlight != null) {
            if (markerContent != null) {
                MarkerPositioner(
                    position = highlight.pixelPosition,
                    chartSize = chartSize,
                ) {
                    markerContent()
                }
            }

            if (highlightConfig.yAxisLabel) {
                DynamicYAxisLabel(
                    pixelY = highlight.pixelPosition.y,
                    value = highlight.y,
                    formatter = highlightConfig.yAxisLabelFormatter,
                    labelColor = highlightConfig.yAxisLabelColor,
                    backgroundColor = highlightConfig.yAxisLabelBackground,
                    chartLeft = leftMargin,
                )
            }

            if (highlightConfig.xAxisLabel) {
                val labelY = when (highlightConfig.xAxisLabelPosition) {
                    XAxisLabelPosition.Bottom -> chartSize.height - bottomMargin
                    XAxisLabelPosition.Top -> topMargin - 20f
                }
                DynamicXAxisLabel(
                    pixelX = highlight.pixelPosition.x,
                    value = highlight.x,
                    formatter = highlightConfig.xAxisLabelFormatter,
                    labelColor = highlightConfig.yAxisLabelColor,
                    backgroundColor = highlightConfig.yAxisLabelBackground,
                    labelY = labelY,
                )
            }
        }
    }
}
