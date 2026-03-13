package com.github.mikephil.charting.compose.highlight

import androidx.compose.ui.geometry.Offset

data class Highlight(
    val dataSetIndex: Int,
    val entryIndex: Int,
    val x: Float,
    val y: Float,
    val pixelPosition: Offset,
)
