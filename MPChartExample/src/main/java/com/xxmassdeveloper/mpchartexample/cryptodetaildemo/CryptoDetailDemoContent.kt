package com.xxmassdeveloper.mpchartexample.cryptodetaildemo

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.mikephil.charting.compose.chart.LineChart
import com.github.mikephil.charting.compose.data.Entry
import com.github.mikephil.charting.compose.data.LimitLine
import com.github.mikephil.charting.compose.data.LineData
import com.github.mikephil.charting.compose.data.LineDataSet
import com.github.mikephil.charting.compose.data.LineGlow
import com.github.mikephil.charting.compose.data.LineMode
import com.github.mikephil.charting.compose.data.YAxisPosition
import com.github.mikephil.charting.compose.scope.CrosshairMode
import com.github.mikephil.charting.compose.scope.XAxisLabelPosition
import com.github.mikephil.charting.compose.state.rememberLineChartState
import java.util.Locale
import kotlin.math.sin

private val ChartGreen = Color(0xFF00AB6B)
private val ChartRed = Color(0xFFEE4A49)

data class DemoChartPoint(
    val index: Int,
    val price: Float,
    val change: Float,
    val changePercent: String,
)

fun generateDemoChartPoints(count: Int = 48): List<DemoChartPoint> {
    val previousClose = 1_650_000_000f
    return (0 until count).map { index ->
        val price = previousClose + sin(index * 0.35f) * 45_000_000f + index * 1_200_000f
        val change = price - previousClose
        val pct = (change / previousClose) * 100f
        DemoChartPoint(
            index = index,
            price = price,
            change = change,
            changePercent = String.format(Locale.US, "%+.2f%%", pct),
        )
    }
}

@Composable
fun CryptoDetailDemoHeaderContent(
    chartPoints: List<DemoChartPoint>,
    scrubbedPoint: DemoChartPoint?,
    onScrub: (DemoChartPoint) -> Unit,
    onScrubEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayPoint = scrubbedPoint ?: chartPoints.lastOrNull()
    val previousClose = chartPoints.firstOrNull()?.let { it.price - it.change } ?: 0f
    val isPositive = (displayPoint?.change ?: 0f) >= 0f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = "BTC",
            fontSize = 14.sp,
            color = Color(0xFF757575),
        )
        Text(
            text = "Bitcoin",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF212121),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = displayPoint?.let { formatIdr(it.price) } ?: "—",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF212121),
        )
        Text(
            text = displayPoint?.let {
                "${formatIdr(it.change)} (${it.changePercent})"
            } ?: "",
            fontSize = 14.sp,
            color = if (isPositive) ChartGreen else ChartRed,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(185.dp)
                .padding(start = 14.dp, end = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            CryptoDetailDemoChart(
            chartPoints = chartPoints,
            previousClose = previousClose,
            onScrub = onScrub,
                onScrubEnd = onScrubEnd,
            )
        }
    }
}

@Composable
private fun CryptoDetailDemoChart(
    chartPoints: List<DemoChartPoint>,
    previousClose: Float,
    onScrub: (DemoChartPoint) -> Unit,
    onScrubEnd: () -> Unit,
) {
    val view = LocalView.current
    var lastScrubIndex by remember { mutableIntStateOf(-1) }

    val entries = remember(chartPoints) {
        chartPoints.map { DemoChartPoint ->
            Entry(x = DemoChartPoint.index.toFloat(), y = DemoChartPoint.price)
        }
    }
    if (entries.isEmpty()) return

    val lastPrice = entries.last().y
    val lineColor = if (lastPrice >= previousClose) ChartGreen else ChartRed

    val lineData = remember(entries, lineColor) {
        LineData(
            dataSets = listOf(
                LineDataSet(
                    entries = entries,
                    label = "BTC",
                    lineColor = lineColor,
                    lineWidth = 1.5.dp,
                    drawCircles = false,
                    mode = LineMode.CubicBezier,
                    glow = LineGlow(
                        enabled = true,
                        radius = 6.dp,
                        alpha = 0.2f,
                        color = lineColor,
                    ),
                )
            )
        )
    }

    val limitLines = remember(previousClose) {
        if (previousClose > 0f) {
            listOf(
                LimitLine(
                    value = previousClose,
                    label = "",
                    color = Color.Gray,
                    lineWidth = 0.5.dp,
                    dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                )
            )
        } else {
            emptyList()
        }
    }

    val chartState = rememberLineChartState()
    LaunchedEffect(lineData) {
        chartState.replaceData(lineData)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        LineChart(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            data = lineData,
            state = chartState,
            xAxis = {
                drawLabels = false
                gridEnabled = false
                axisLineEnabled = false
            },
            yAxis = {
                position = YAxisPosition.Right
                drawLabels = true
                gridEnabled = false
                axisLineEnabled = false
                this.limitLines = limitLines
                formatter = ::formatYAxisLabel
            },
            gestures = {
                dragToScrub = true
                longPressToDrag = true
                tapToHighlight = false
                scrubThreshold = 1000.dp
            },
            highlight = {
                crosshairEnabled = true
                crosshairMode = CrosshairMode.VerticalOnly
                yAxisLabel = false
                xAxisLabel = true
                xAxisLabelPosition = XAxisLabelPosition.Top
                xAxisLabelFormatter = { x ->
                    val index = x.toInt().coerceIn(0, chartPoints.lastIndex)
                    formatScrubPrice(chartPoints[index].price)
                }
                circleRadius = 3.dp
                circleColor = lineColor
                circleStrokeWidth = 0.dp
            },
            legend = { enabled = false },
            style = { backgroundColor = Color.Transparent },
            onValueSelected = { entry ->
                val index = entry.x.toInt()
                if (index != lastScrubIndex) {
                    lastScrubIndex = index
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                }
                chartPoints.getOrNull(index)?.let(onScrub)
            },
            onNothingSelected = {
                lastScrubIndex = -1
                onScrubEnd()
            },
        )
    }
}

private fun formatIdr(value: Float): String {
    return String.format(Locale.US, "Rp %,.0f", value)
}

private fun formatYAxisLabel(value: Float): String {
    return if (value % 1f == 0f) {
        String.format(Locale.US, "%.0f", value)
    } else {
        String.format(Locale.US, "%.2f", value)
    }
}

private fun formatScrubPrice(price: Float): String {
    return if (price >= 1f) {
        String.format(Locale.US, "%,.2f", price)
    } else {
        String.format(Locale.US, "%,.8f", price).trimEnd('0').trimEnd('.')
    }
}
