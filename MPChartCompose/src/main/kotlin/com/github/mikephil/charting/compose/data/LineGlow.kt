package com.github.mikephil.charting.compose.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class LineGlow(
    val enabled: Boolean = true,
    val radius: Dp = 8.dp,
    val alpha: Float = 0.4f,
    val color: Color = Color.Unspecified,
)
