package com.github.mikephil.charting.compose.scope

import com.github.mikephil.charting.compose.data.AxisPosition

class XAxisScope {
    var position: AxisPosition = AxisPosition.Bottom
    var formatter: ((Float) -> String)? = null
    var gridEnabled: Boolean = true
    var drawLabels: Boolean = true
    var labelCount: Int = 6
    var axisLineEnabled: Boolean = true
    var spaceMin: Float = 0f
    var spaceMax: Float = 0f
    var axisMinimum: Float? = null
    var axisMaximum: Float? = null
    var granularity: Float? = null
    var forceLabels: Boolean = false
}
