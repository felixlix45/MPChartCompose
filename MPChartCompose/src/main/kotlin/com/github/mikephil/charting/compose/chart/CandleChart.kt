package com.github.mikephil.charting.compose.chart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import com.github.mikephil.charting.compose.animation.ChartAnimator
import com.github.mikephil.charting.compose.data.CandleData
import com.github.mikephil.charting.compose.data.CandleEntry
import com.github.mikephil.charting.compose.data.YAxisPosition
import com.github.mikephil.charting.compose.gesture.chartGestureHandler
import com.github.mikephil.charting.compose.highlight.Highlight
import com.github.mikephil.charting.compose.renderer.AxisLabelRenderer
import com.github.mikephil.charting.compose.renderer.CandleDataRenderer
import com.github.mikephil.charting.compose.renderer.GridRenderer
import com.github.mikephil.charting.compose.renderer.HighlightRenderer
import com.github.mikephil.charting.compose.renderer.LegendRenderer
import com.github.mikephil.charting.compose.renderer.LimitLineRenderer
import com.github.mikephil.charting.compose.scope.AnimationScope
import com.github.mikephil.charting.compose.scope.ChartStyleScope
import com.github.mikephil.charting.compose.scope.GestureScope
import com.github.mikephil.charting.compose.scope.HighlightScope
import com.github.mikephil.charting.compose.scope.LegendScope
import com.github.mikephil.charting.compose.scope.ViewportScope
import com.github.mikephil.charting.compose.scope.XAxisScope
import com.github.mikephil.charting.compose.scope.YAxisScope
import com.github.mikephil.charting.compose.state.CandleChartState
import com.github.mikephil.charting.compose.style.LocalChartStyle
import com.github.mikephil.charting.compose.util.AxisComputer
import com.github.mikephil.charting.compose.util.EntryFinder
import com.github.mikephil.charting.compose.util.Transformer
import kotlin.math.abs

@Composable
fun CandleChart(
    modifier: Modifier = Modifier,
    data: CandleData? = null,
    state: CandleChartState? = null,
    xAxis: (XAxisScope.() -> Unit)? = null,
    yAxis: (YAxisScope.() -> Unit)? = null,
    gestures: (GestureScope.() -> Unit)? = null,
    highlight: (HighlightScope.() -> Unit)? = null,
    marker: (@Composable (CandleEntry) -> Unit)? = null,
    animation: (AnimationScope.() -> Unit)? = null,
    style: (ChartStyleScope.() -> Unit)? = null,
    legend: (LegendScope.() -> Unit)? = null,
    viewport: (ViewportScope.() -> Unit)? = null,
    onValueSelected: ((CandleEntry) -> Unit)? = null,
    onNothingSelected: (() -> Unit)? = null,
) {
    // Resolve configurations
    val baseStyle = LocalChartStyle.current
    val chartStyle = if (style != null) {
        val scope = ChartStyleScope().apply(style)
        baseStyle.mergeWith(scope)
    } else baseStyle

    val xAxisConfig = remember { XAxisScope() }.also { xAxis?.invoke(it) }
    val yAxisConfig = remember { YAxisScope() }.also { yAxis?.invoke(it) }
    val gestureConfig = remember { GestureScope() }.also { gestures?.invoke(it) }
    val highlightConfig = remember { HighlightScope() }.also { highlight?.invoke(it) }
    val animConfig = remember { AnimationScope() }.also { animation?.invoke(it) }
    val legendConfig = remember { LegendScope() }.also { legend?.invoke(it) }
    val viewportConfig = remember { ViewportScope() }.also { viewport?.invoke(it) }

    // State
    val highlightState = remember { mutableStateOf<Highlight?>(null) }
    val currentData = state?.data ?: data ?: CandleData(dataSets = emptyList())

    // Appear animation
    val animator = remember { ChartAnimator() }
    var appearPhase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        animator.animateAppear(animConfig.appear) { appearPhase = it }
    }

    // Transformer state (shared between draw and gestures)
    var currentTransformer by remember { mutableStateOf<Transformer?>(null) }

    // All entries from first data set for gesture lookup
    val allEntries = currentData.dataSets.firstOrNull()?.entries ?: emptyList()

    // Legend entries
    val legendEntries = if (legendConfig.enabled) {
        currentData.dataSets.map { ds ->
            LegendRenderer.LegendEntry(
                label = ds.label,
                color = ds.increasingColor,
            )
        }
    } else emptyList()

    // Gesture modifier — for candle charts, use close price as the Y value for highlights
    val gestureModifier = Modifier.chartGestureHandler(
        tapToHighlight = gestureConfig.tapToHighlight,
        dragToPan = gestureConfig.dragToPan,
        dragToScrub = gestureConfig.dragToScrub,
        longPressToDrag = gestureConfig.longPressToDrag,
        flingEnabled = gestureConfig.flingDeceleration,
        scrubThreshold = gestureConfig.scrubThreshold,
        highlightState = highlightState,
        transformerProvider = { currentTransformer },
        findNearestIndexByX = { targetX ->
            EntryFinder.findNearestIndexByX(allEntries.map { it.x }, targetX)
        },
        getEntryX = { idx -> allEntries.getOrNull(idx)?.x ?: 0f },
        getEntryY = { idx -> allEntries.getOrNull(idx)?.close ?: 0f },
        onTranslationChange = { /* TODO: update viewport translation */ },
        onValueSelected = { idx ->
            allEntries.getOrNull(idx)?.let { onValueSelected?.invoke(it) }
        },
        onNothingSelected = onNothingSelected,
    )

    // Marker wrapper
    val markerContent: (@Composable () -> Unit)? = if (marker != null) {
        val hl = highlightState.value
        if (hl != null) {
            val entry = allEntries.getOrNull(hl.entryIndex)
            if (entry != null) {
                { marker(entry) }
            } else null
        } else null
    } else null

    ChartCore(
        modifier = modifier,
        style = chartStyle,
        xAxisConfig = xAxisConfig,
        yAxisConfig = yAxisConfig,
        gestureConfig = gestureConfig,
        highlightConfig = highlightConfig,
        legendConfig = legendConfig,
        viewportConfig = viewportConfig,
        highlightState = highlightState,
        legendEntries = legendEntries,
        gestureModifier = gestureModifier,
        drawData = { _, textMeasurer ->
            // Compute data ranges using high/low values
            val allEntriesNow = currentData.dataSets.flatMap { it.entries }
            if (allEntriesNow.isEmpty()) return@ChartCore

            val dataXMin = allEntriesNow.minOf { it.x }
            val dataXMax = allEntriesNow.maxOf { it.x }
            val dataYMin = allEntriesNow.minOf { it.low }
            val dataYMax = allEntriesNow.maxOf { it.high }

            // Y-axis range with spaceTop/spaceBottom (matching original YAxis.calculate())
            val yDataRange = abs(dataYMax - dataYMin)
            val adjYMin = if (yDataRange == 0f) dataYMin - 1f else dataYMin
            val adjYMax = if (yDataRange == 0f) dataYMax + 1f else dataYMax
            val adjYRange = abs(adjYMax - adjYMin)
            val yMin = yAxisConfig.axisMinimum ?: (adjYMin - (adjYRange / 100f) * yAxisConfig.spaceBottom)
            val yMax = yAxisConfig.axisMaximum ?: (adjYMax + (adjYRange / 100f) * yAxisConfig.spaceTop)

            // X-axis range with spaceMin/spaceMax and custom bounds
            val baseXMin = xAxisConfig.axisMinimum ?: (dataXMin - xAxisConfig.spaceMin)
            val baseXMax = xAxisConfig.axisMaximum ?: (dataXMax + xAxisConfig.spaceMax)
            val xMin = if (baseXMin == baseXMax) baseXMin - 1f else baseXMin
            val xMax = if (baseXMin == baseXMax) baseXMax + 1f else baseXMax

            val xRange = xMin..xMax
            val yRange = yMin..yMax

            // Compute labels before contentRect so we can measure y-axis label width
            val xLabels = AxisComputer.computeAxisValues(xRange.start, xRange.endInclusive, xAxisConfig.labelCount, xAxisConfig.granularity, xAxisConfig.forceLabels).labels
            val yLabels = AxisComputer.computeAxisValues(yRange.start, yRange.endInclusive, yAxisConfig.labelCount, yAxisConfig.granularity, yAxisConfig.forceLabels).labels

            // Measure widest y-axis label to size the content rect
            val labelStyle = androidx.compose.ui.text.TextStyle(color = chartStyle.axisLabelColor, fontSize = chartStyle.axisLabelSize)
            val yAxisWidth = if (yAxisConfig.drawLabels && yLabels.isNotEmpty()) {
                yLabels.maxOf { value ->
                    val text = yAxisConfig.formatter?.invoke(value) ?: value.toString()
                    textMeasurer.measure(text, labelStyle).size.width
                }.toFloat() + 8f
            } else 8f

            val yAxisOnRight = yAxisConfig.position == YAxisPosition.Right
            val contentRect = Rect(
                left = if (yAxisOnRight) 8f else yAxisWidth,
                top = if (legendConfig.enabled) 24f else 8f,
                right = if (yAxisOnRight) size.width - yAxisWidth else size.width - 8f,
                bottom = size.height - 32f
            )

            val transformer = Transformer(contentRect, xRange, yRange)
            currentTransformer = transformer

            // Layer 2: Grid + Axes
            with(GridRenderer) {
                drawGrid(
                    transformer = transformer,
                    xLabelValues = xLabels,
                    yLabelValues = yLabels,
                    gridColor = chartStyle.gridColor,
                    gridLineWidth = chartStyle.gridLineWidth,
                    drawXGrid = yAxisConfig.gridEnabled,
                    drawYGrid = xAxisConfig.gridEnabled,
                    axisLineColor = chartStyle.axisLineColor,
                    drawXAxisLine = xAxisConfig.axisLineEnabled,
                    drawYAxisLine = yAxisConfig.axisLineEnabled,
                    yAxisOnRight = yAxisOnRight,
                )
            }

            // Axis labels
            if (xAxisConfig.drawLabels) {
                with(AxisLabelRenderer) {
                    drawXAxisLabels(transformer, xLabels, xAxisConfig.formatter, textMeasurer, chartStyle.axisLabelColor, chartStyle.axisLabelSize)
                }
            }
            if (yAxisConfig.drawLabels) {
                with(AxisLabelRenderer) {
                    drawYAxisLabels(transformer, yLabels, yAxisConfig.formatter, textMeasurer, chartStyle.axisLabelColor, chartStyle.axisLabelSize, yAxisOnRight = yAxisOnRight)
                }
            }

            // Limit lines
            if (yAxisConfig.limitLines.isNotEmpty()) {
                with(LimitLineRenderer) {
                    drawLimitLines(transformer, yAxisConfig.limitLines, textMeasurer)
                }
            }

            // Layer 3: Data
            with(CandleDataRenderer) {
                drawCandleData(currentData.dataSets, transformer, appearPhase)
            }

            // Layer 4: Highlights
            val hl = highlightState.value
            if (hl != null) {
                with(HighlightRenderer) {
                    drawHighlight(
                        highlight = hl,
                        transformer = transformer,
                        crosshairEnabled = highlightConfig.crosshairEnabled,
                        crosshairMode = highlightConfig.crosshairMode,
                        crosshairColor = highlightConfig.crosshairColor,
                        crosshairWidth = highlightConfig.crosshairWidth,
                        circleRadius = highlightConfig.circleRadius,
                        circleColor = highlightConfig.circleColor,
                        circleStrokeWidth = highlightConfig.circleStrokeWidth,
                        circleStrokeColor = highlightConfig.circleStrokeColor,
                    )
                }
            }

            // Layer 5: Legend
            if (legendConfig.enabled && legendEntries.isNotEmpty()) {
                with(LegendRenderer) {
                    drawLegend(
                        entries = legendEntries,
                        position = legendConfig.position,
                        orientation = legendConfig.orientation,
                        formSize = legendConfig.formSize,
                        textColor = if (legendConfig.textColor != Color.Unspecified) legendConfig.textColor else chartStyle.legendTextColor,
                        textSize = if (legendConfig.textSize != TextUnit.Unspecified) legendConfig.textSize else chartStyle.legendTextSize,
                        textMeasurer = textMeasurer,
                        chartRect = contentRect,
                    )
                }
            }
        },
        markerContent = markerContent,
    )
}
