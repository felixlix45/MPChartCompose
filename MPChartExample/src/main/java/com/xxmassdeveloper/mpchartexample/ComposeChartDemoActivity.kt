package com.xxmassdeveloper.mpchartexample

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xxmassdeveloper.mpchartexample.cryptodetaildemo.CryptoDetailScrollScrubDemoActivity
import com.github.mikephil.charting.compose.animation.ChartAnimation
import com.github.mikephil.charting.compose.chart.BarChart
import com.github.mikephil.charting.compose.chart.CandleChart
import com.github.mikephil.charting.compose.chart.LineChart
import com.github.mikephil.charting.compose.scope.MinMaxLabelScope
import com.github.mikephil.charting.compose.data.BarData
import com.github.mikephil.charting.compose.data.BarDataSet
import com.github.mikephil.charting.compose.data.BarEntry
import com.github.mikephil.charting.compose.data.CandleData
import com.github.mikephil.charting.compose.data.CandleDataSet
import com.github.mikephil.charting.compose.data.CandleEntry
import com.github.mikephil.charting.compose.data.Entry
import com.github.mikephil.charting.compose.data.LimitLine
import com.github.mikephil.charting.compose.data.LineData
import com.github.mikephil.charting.compose.data.LineDataSet
import com.github.mikephil.charting.compose.data.LineGlow
import com.github.mikephil.charting.compose.data.LineMode
import com.github.mikephil.charting.compose.scope.CrosshairMode
import com.github.mikephil.charting.compose.scope.XAxisLabelPosition
import com.github.mikephil.charting.compose.style.ChartStyle
import com.github.mikephil.charting.compose.style.ChartThemeProvider
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ComposeChartDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeChartDemoScreen()
        }
    }
}

@Composable
private fun ComposeChartDemoScreen() {
    val darkBackground = Color(0xFF121212)
    val context = LocalContext.current

    ChartThemeProvider(
        style = ChartStyle(
            backgroundColor = Color(0xFF1A1A1A),
            gridColor = Color(0xFF2A2A2A),
            axisLineColor = Color(0xFF444444),
            axisLabelColor = Color(0xFFAAAAAA),
            highlightLineColor = Color.White,
            legendTextColor = Color(0xFFCCCCCC),
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(darkBackground)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Title
            Text(
                text = "MPChartCompose Demo",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Native Jetpack Compose Charts",
                color = Color(0xFF888888),
                fontSize = 14.sp,
            )

            Button(
                onClick = {
                    context.startActivity(
                        Intent(context, CryptoDetailScrollScrubDemoActivity::class.java),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Open Crypto Detail Scroll/Scrub Demo")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 1. Simple Line Chart
            SectionTitle("1. Line Chart — Stock Price")
            LineChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                data = sampleStockLineData(),
                xAxis = {
                    gridEnabled = true
                    formatter = { value -> "${value.toInt()}:00" }
                    labelCount = 6
                },
                yAxis = {
                    gridEnabled = true
                    formatter = { value -> "$${String.format("%.0f", value)}" }
                    labelCount = 5
                    limitLines = listOf(
                        LimitLine(value = 170f, label = "Open", color = Color(0xFFFFAA00))
                    )
                },
                highlight = {
                    crosshairEnabled = true
                    crosshairMode = CrosshairMode.VerticalOnly
                    circleColor = Color.White
                    circleRadius = 6.dp
                    yAxisLabel = true
                    yAxisLabelFormatter = { "$${String.format("%.2f", it)}" }
                    xAxisLabel = true
                    xAxisLabelFormatter = { "${it.toInt()}:00" }
                    xAxisLabelPosition = XAxisLabelPosition.Top
                },
                gestures = {
                    tapToHighlight = true
                    dragToScrub = true
                    dragToPan = false
                },
                animation = {
                    appear = ChartAnimation.EaseInOut(1.seconds)
                },
                style = {
                    backgroundColor = Color(0xFF0D1117)
                    gridColor = Color(0xFF21262D)
                },
                legend = { enabled = true },
                onValueSelected = { },
                minMaxLabel = {
                    maxTextColor = Color(0xFF4CAF50)
                    minTextColor = Color(0xFFFF5252)
                    textSize = 11.sp
                    fontWeight = FontWeight.Bold
                    formatter = { value -> "$${String.format("%.0f", value)}" }
                },
            )

            // 2. Line Chart with Glow Effect
            SectionTitle("2. Line Chart — Stock Price with Glow")
            LineChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                data = sampleGlowLineData(),
                xAxis = {
                    gridEnabled = true
                    drawLabels = true
                    labelCount = 6
                    formatter = { "${it.toInt()}:00" }
                },
                yAxis = {
                    gridEnabled = true
                    formatter = { "$${String.format("%.0f", it)}" }
                    labelCount = 5
                },
                highlight = {
                    crosshairEnabled = true
                    circleColor = Color.White
                    circleRadius = 6.dp
                    yAxisLabel = true
                    yAxisLabelFormatter = { "$${String.format("%.2f", it)}" }
                },
                gestures = {
                    tapToHighlight = true
                    dragToScrub = true
                    dragToPan = false
                },
                animation = {
                    appear = ChartAnimation.EaseInOut(1200.milliseconds)
                },
                style = {
                    backgroundColor = Color(0xFF0D1117)
                    gridColor = Color(0xFF21262D)
                },
                legend = { enabled = true },
                minMaxLabel = {
                    maxTextColor = Color(0xFF4CAF50)
                    minTextColor = Color(0xFFFF5252)
                    textSize = 11.sp
                    fontWeight = FontWeight.Bold
                    formatter = { value -> "$${String.format("%.0f", value)}" }
                },
            )

            // 3. Line Chart with Fill
            SectionTitle("3. Line Chart — Gradient Fill")
            LineChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                data = sampleFilledLineData(),
                xAxis = {
                    labelCount = 6
                    formatter = { "Day ${it.toInt()}" }
                },
                yAxis = {
                    formatter = { "${it.toInt()}%" }
                    labelCount = 5
                },
                animation = {
                    appear = ChartAnimation.EaseOut(800.milliseconds)
                },
                legend = { enabled = true },
            )

            // 4. Multi-Line Chart
            SectionTitle("4. Multi-Line Chart")
            LineChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                data = sampleMultiLineData(),
                xAxis = {
                    labelCount = 6
                    formatter = { "W${it.toInt()}" }
                },
                yAxis = {
                    formatter = { "${it.toInt()}" }
                },
                animation = {
                    appear = ChartAnimation.EaseInOut(1.seconds)
                },
                legend = { enabled = true },
            )

            // 5. Cubic Bezier Line Chart
            SectionTitle("5. Line Chart — Cubic Bezier")
            LineChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                data = sampleCubicLineData(),
                xAxis = {
                    labelCount = 6
                },
                yAxis = {
                    formatter = { String.format("%.1f", it) }
                },
                animation = {
                    appear = ChartAnimation.Spring(dampingRatio = 0.6f, stiffness = 300f)
                },
                legend = { enabled = false },
            )

            // 6. Stepped Line Chart
            SectionTitle("6. Line Chart — Stepped")
            LineChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                data = sampleSteppedLineData(),
                xAxis = {
                    labelCount = 5
                    formatter = { "${it.toInt()}h" }
                },
                yAxis = {
                    formatter = { "${it.toInt()}" }
                },
                animation = {
                    appear = ChartAnimation.EaseIn(600.milliseconds)
                },
                style = {
                    backgroundColor = Color(0xFF1A1A2E)
                    gridColor = Color(0xFF16213E)
                },
                legend = { enabled = true },
            )

            // 7. Bar Chart — Volume
            SectionTitle("7. Bar Chart — Volume")
            BarChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                data = sampleBarData(),
                xAxis = {
                    labelCount = 6
                    formatter = { "${it.toInt()}:00" }
                },
                yAxis = {
                    formatter = { "${(it / 1000).toInt()}K" }
                },
                animation = {
                    appear = ChartAnimation.EaseOut(800.milliseconds)
                },
                legend = { enabled = true },
                onValueSelected = { },
            )

            // 8. Stacked Bar Chart
            SectionTitle("8. Bar Chart — Stacked")
            BarChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                data = sampleStackedBarData(),
                xAxis = {
                    labelCount = 5
                    formatter = { "Q${it.toInt() + 1}" }
                },
                yAxis = {
                    formatter = { "${it.toInt()}" }
                },
                animation = {
                    appear = ChartAnimation.EaseInOut(1.seconds)
                },
                legend = { enabled = true },
            )

            // 9. Candlestick Chart — OHLC
            SectionTitle("9. Candlestick Chart — OHLC")
            CandleChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                data = sampleCandleData(),
                xAxis = {
                    labelCount = 6
                    formatter = { "Day ${it.toInt()}" }
                },
                yAxis = {
                    formatter = { "$${String.format("%.0f", it)}" }
                    limitLines = listOf(
                        LimitLine(value = 155f, label = "Open", color = Color(0xFFFFAA00))
                    )
                },
                highlight = {
                    crosshairEnabled = true
                    yAxisLabel = true
                    yAxisLabelFormatter = { "$${String.format("%.2f", it)}" }
                    xAxisLabel = true
                    xAxisLabelFormatter = { "Day ${it.toInt()}" }
                },
                gestures = {
                    tapToHighlight = true
                    dragToScrub = true
                    dragToPan = true
                },
                animation = {
                    appear = ChartAnimation.EaseInOut(1.seconds)
                },
                style = {
                    backgroundColor = Color(0xFF0D1117)
                    gridColor = Color(0xFF21262D)
                },
                marker = { entry ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E1E2E),
                        shadowElevation = 4.dp,
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("O: $${String.format("%.2f", entry.open)}", color = Color.White, fontSize = 11.sp)
                            Text("H: $${String.format("%.2f", entry.high)}", color = Color(0xFF00C853), fontSize = 11.sp)
                            Text("L: $${String.format("%.2f", entry.low)}", color = Color(0xFFFF1744), fontSize = 11.sp)
                            Text("C: $${String.format("%.2f", entry.close)}", color = Color.White, fontSize = 11.sp)
                        }
                    }
                },
                legend = { enabled = false },
                onValueSelected = { },
            )

            // 10. Line + Bar combo demonstration
            SectionTitle("10. Price + Volume Layout")
            Text(
                text = "Line chart (price) above bar chart (volume) — typical stock layout",
                color = Color(0xFF666666),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            LineChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                data = sampleStockLineData(),
                xAxis = {
                    drawLabels = false
                    gridEnabled = true
                },
                yAxis = {
                    formatter = { "$${it.toInt()}" }
                    labelCount = 4
                },
                animation = {
                    appear = ChartAnimation.EaseInOut(800.milliseconds)
                },
                legend = { enabled = false },
            )
            BarChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                data = sampleBarData(),
                xAxis = {
                    labelCount = 6
                    formatter = { "${it.toInt()}:00" }
                },
                yAxis = {
                    drawLabels = false
                    gridEnabled = false
                },
                animation = {
                    appear = ChartAnimation.EaseOut(600.milliseconds)
                },
                style = {
                    backgroundColor = Color(0xFF0D1117)
                },
                legend = { enabled = false },
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = Color(0xFFE0E0E0),
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

// --- Sample Data Generators ---

private fun sampleStockLineData(): LineData {
    val entries = (0..23).map { hour ->
        val price = 165f + sin(hour * 0.5) * 8f + hour * 0.5f
        Entry(x = hour.toFloat(), y = price.toFloat())
    }
    return LineData(
        dataSets = listOf(
            LineDataSet(
                entries = entries,
                label = "AAPL",
                lineColor = Color(0xFF00C853),
                lineWidth = 2.dp,
                drawCircles = false,
                mode = LineMode.Linear,
            )
        )
    )
}

private fun sampleGlowLineData(): LineData {
    val entries = (0..23).map { hour ->
        val price = 165f + sin(hour * 0.5) * 8f + hour * 0.5f
        Entry(x = hour.toFloat(), y = price.toFloat())
    }
    return LineData(
        dataSets = listOf(
            LineDataSet(
                entries = entries,
                label = "AAPL",
                lineColor = Color(0xFF4CAF50),
                lineWidth = 2.5.dp,
                drawCircles = false,
                mode = LineMode.Linear,
                glow = LineGlow(
                    enabled = true,
                    radius = 24.dp,
                    alpha = 0.2f,
                    color = Color(0xFF4CAF50),
                ),
            )
        )
    )
}

private fun sampleFilledLineData(): LineData {
    val entries = (0..14).map { day ->
        val value = 20f + sin(day * 0.4) * 15f + day * 2f
        Entry(x = day.toFloat(), y = value.toFloat())
    }
    return LineData(
        dataSets = listOf(
            LineDataSet(
                entries = entries,
                label = "Portfolio",
                lineColor = Color(0xFF7C4DFF),
                lineWidth = 2.dp,
                drawCircles = true,
                circleRadius = 3.dp,
                mode = LineMode.CubicBezier,
                fillEnabled = true,
                fillBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF7C4DFF).copy(alpha = 0.4f),
                        Color(0xFF7C4DFF).copy(alpha = 0.0f),
                    )
                ),
            )
        )
    )
}

private fun sampleMultiLineData(): LineData {
    val seriesA = (0..12).map { w ->
        Entry(x = w.toFloat(), y = (30 + sin(w * 0.5) * 20 + w * 1.5).toFloat())
    }
    val seriesB = (0..12).map { w ->
        Entry(x = w.toFloat(), y = (50 + sin(w * 0.3 + 1) * 15 + w * 0.8).toFloat())
    }
    val seriesC = (0..12).map { w ->
        Entry(x = w.toFloat(), y = (40 + sin(w * 0.7 + 2) * 10 + w).toFloat())
    }
    return LineData(
        dataSets = listOf(
            LineDataSet(entries = seriesA, label = "Product A", lineColor = Color(0xFF2196F3), lineWidth = 2.dp, drawCircles = false),
            LineDataSet(entries = seriesB, label = "Product B", lineColor = Color(0xFFFF9800), lineWidth = 2.dp, drawCircles = false),
            LineDataSet(entries = seriesC, label = "Product C", lineColor = Color(0xFF4CAF50), lineWidth = 2.dp, drawCircles = false),
        )
    )
}

private fun sampleCubicLineData(): LineData {
    val entries = (0..20).map { i ->
        Entry(x = i.toFloat(), y = (sin(i * 0.4) * 3 + 5).toFloat())
    }
    return LineData(
        dataSets = listOf(
            LineDataSet(
                entries = entries,
                label = "Smooth",
                lineColor = Color(0xFFE91E63),
                lineWidth = 2.5.dp,
                drawCircles = false,
                mode = LineMode.CubicBezier,
                fillEnabled = true,
                fillColor = Color(0xFFE91E63).copy(alpha = 0.15f),
            )
        )
    )
}

private fun sampleSteppedLineData(): LineData {
    val entries = listOf(
        Entry(0f, 100f), Entry(2f, 100f), Entry(4f, 120f),
        Entry(6f, 120f), Entry(8f, 80f), Entry(10f, 80f),
        Entry(12f, 150f), Entry(14f, 150f), Entry(16f, 110f),
    )
    return LineData(
        dataSets = listOf(
            LineDataSet(
                entries = entries,
                label = "Server Load",
                lineColor = Color(0xFFFFAB00),
                lineWidth = 2.dp,
                drawCircles = true,
                circleRadius = 3.dp,
                mode = LineMode.Stepped,
            )
        )
    )
}

private fun sampleBarData(): BarData {
    val entries = (0..23).map { hour ->
        val volume = (2000 + sin(hour * 0.7) * 1500 + hour * 100).toFloat()
        BarEntry(x = hour.toFloat(), y = volume.coerceAtLeast(500f))
    }
    return BarData(
        dataSets = listOf(
            BarDataSet(
                entries = entries,
                label = "Volume",
                barColor = Color(0xFF42A5F5).copy(alpha = 0.7f),
            )
        ),
        barWidth = 0.8f,
    )
}

private fun sampleStackedBarData(): BarData {
    val entries = (0..3).map { q ->
        BarEntry(
            x = q.toFloat(),
            y = 0f,
            stackValues = listOf(
                (30 + q * 5).toFloat(),
                (20 + q * 3).toFloat(),
                (15 + q * 2).toFloat(),
            ),
        )
    }
    return BarData(
        dataSets = listOf(
            BarDataSet(
                entries = entries,
                label = "Revenue",
                barColor = Color(0xFF66BB6A),
            )
        ),
        barWidth = 0.6f,
    )
}

private fun sampleCandleData(): CandleData {
    val entries = listOf(
        CandleEntry(0f, open = 150f, high = 158f, low = 148f, close = 155f),
        CandleEntry(1f, open = 155f, high = 162f, low = 153f, close = 160f),
        CandleEntry(2f, open = 160f, high = 165f, low = 157f, close = 158f),
        CandleEntry(3f, open = 158f, high = 163f, low = 155f, close = 162f),
        CandleEntry(4f, open = 162f, high = 168f, low = 160f, close = 165f),
        CandleEntry(5f, open = 165f, high = 170f, low = 162f, close = 163f),
        CandleEntry(6f, open = 163f, high = 167f, low = 159f, close = 166f),
        CandleEntry(7f, open = 166f, high = 172f, low = 164f, close = 170f),
        CandleEntry(8f, open = 170f, high = 175f, low = 168f, close = 169f),
        CandleEntry(9f, open = 169f, high = 174f, low = 166f, close = 173f),
        CandleEntry(10f, open = 173f, high = 178f, low = 171f, close = 175f),
        CandleEntry(11f, open = 175f, high = 180f, low = 172f, close = 174f),
        CandleEntry(12f, open = 174f, high = 179f, low = 170f, close = 177f),
        CandleEntry(13f, open = 177f, high = 182f, low = 175f, close = 180f),
        CandleEntry(14f, open = 180f, high = 185f, low = 178f, close = 176f),
    )
    return CandleData(
        dataSets = listOf(
            CandleDataSet(
                entries = entries,
                label = "AAPL",
                increasingColor = Color(0xFF00C853),
                decreasingColor = Color(0xFFFF1744),
                shadowColor = Color(0xFF888888),
                bodyWidth = 0.7f,
            )
        )
    )
}
