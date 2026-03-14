package com.github.mikephil.charting.compose.scope

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class XAxisLabelPosition { Bottom, Top }

class HighlightScope {
    var circleRadius: Dp = 6.dp
    var circleColor: Color = Color.White
    var circleStrokeWidth: Dp = 2.dp
    var circleStrokeColor: Color = Color.Unspecified
    var crosshairEnabled: Boolean = true
    var crosshairColor: Color = Color(0xFF888888)
    var crosshairWidth: Dp = 1.dp
    var yAxisLabel: Boolean = false
    var yAxisLabelColor: Color = Color.White
    var yAxisLabelBackground: Color = Color.Unspecified
    var yAxisLabelFormatter: ((Float) -> String)? = null
    var xAxisLabel: Boolean = false
    var xAxisLabelFormatter: ((Float) -> String)? = null
    var xAxisLabelPosition: XAxisLabelPosition = XAxisLabelPosition.Bottom
}
