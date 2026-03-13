package com.github.mikephil.charting.compose.style

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.mikephil.charting.compose.scope.ChartStyleScope

data class ChartStyle(
    val backgroundColor: Color = Color(0xFF1A1A1A),
    val gridColor: Color = Color(0xFF2A2A2A),
    val gridLineWidth: Dp = 0.5.dp,
    val axisLineColor: Color = Color(0xFF444444),
    val axisLabelColor: Color = Color(0xFFAAAAAA),
    val axisLabelSize: TextUnit = 12.sp,
    val limitLineColor: Color = Color(0xFFFFAA00),
    val highlightLineColor: Color = Color(0xFFFFFFFF),
    val highlightLineWidth: Dp = 1.dp,
    val legendTextColor: Color = Color(0xFFCCCCCC),
    val legendTextSize: TextUnit = 11.sp,
) {
    fun mergeWith(scope: ChartStyleScope): ChartStyle {
        return copy(
            backgroundColor = if (scope.backgroundColor != Color.Unspecified) scope.backgroundColor else backgroundColor,
            gridColor = if (scope.gridColor != Color.Unspecified) scope.gridColor else gridColor,
            gridLineWidth = if (scope.gridLineWidth != Dp.Unspecified) scope.gridLineWidth else gridLineWidth,
            axisLineColor = if (scope.axisLineColor != Color.Unspecified) scope.axisLineColor else axisLineColor,
            axisLabelColor = if (scope.axisLabelColor != Color.Unspecified) scope.axisLabelColor else axisLabelColor,
            axisLabelSize = if (scope.axisLabelSize != TextUnit.Unspecified) scope.axisLabelSize else axisLabelSize,
            highlightLineColor = if (scope.highlightLineColor != Color.Unspecified) scope.highlightLineColor else highlightLineColor,
            highlightLineWidth = if (scope.highlightLineWidth != Dp.Unspecified) scope.highlightLineWidth else highlightLineWidth,
        )
    }
}
