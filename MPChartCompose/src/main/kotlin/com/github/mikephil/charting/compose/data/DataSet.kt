package com.github.mikephil.charting.compose.data

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class LineDataSet(
    val entries: List<Entry>,
    val label: String,
    val lineColor: Color = Color.Unspecified,
    val lineWidth: Dp = 2.dp,
    val drawCircles: Boolean = true,
    val circleRadius: Dp = 4.dp,
    val mode: LineMode = LineMode.Linear,
    val fillEnabled: Boolean = false,
    val fillBrush: Brush? = null,
    val fillColor: Color = Color.Unspecified,
    val glow: LineGlow? = null,
)

data class BarDataSet(
    val entries: List<BarEntry>,
    val label: String,
    val barColor: Color = Color.Unspecified,
    val barBrush: Brush? = null,
)

data class CandleDataSet(
    val entries: List<CandleEntry>,
    val label: String,
    val increasingColor: Color = Color(0xFF00C853),
    val decreasingColor: Color = Color(0xFFFF1744),
    val shadowColor: Color = Color(0xFF888888),
    val bodyWidth: Float = 0.7f,
)
