package com.github.mikephil.charting.compose.style

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

val LocalChartStyle = compositionLocalOf { ChartStyle() }

@Composable
fun ChartThemeProvider(
    style: ChartStyle,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalChartStyle provides style) {
        content()
    }
}
