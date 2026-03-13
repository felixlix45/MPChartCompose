package com.github.mikephil.charting.compose.scope

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit

class ChartStyleScope {
    var backgroundColor: Color = Color.Unspecified
    var gridColor: Color = Color.Unspecified
    var gridLineWidth: Dp = Dp.Unspecified
    var axisLineColor: Color = Color.Unspecified
    var axisLabelColor: Color = Color.Unspecified
    var axisLabelSize: TextUnit = TextUnit.Unspecified
    var highlightLineColor: Color = Color.Unspecified
    var highlightLineWidth: Dp = Dp.Unspecified
}
