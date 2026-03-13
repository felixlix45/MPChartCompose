package com.github.mikephil.charting.compose.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class LineMode {
    Linear,
    CubicBezier,
    HorizontalBezier,
    Stepped,
}

enum class AxisPosition { Top, Bottom }

enum class LegendPosition { Top, Bottom, Left, Right }

enum class LegendOrientation { Horizontal, Vertical }

data class LimitLine(
    val value: Float,
    val label: String = "",
    val color: Color = Color(0xFFFFAA00),
    val lineWidth: Dp = 1.dp,
    val dashEffect: PathEffect? = null,
)
