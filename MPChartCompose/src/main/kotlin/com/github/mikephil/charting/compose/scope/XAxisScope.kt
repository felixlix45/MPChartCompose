package com.github.mikephil.charting.compose.scope

import com.github.mikephil.charting.compose.data.AxisPosition

class XAxisScope {
    var position: AxisPosition = AxisPosition.Bottom
    var formatter: ((Float) -> String)? = null
    var gridEnabled: Boolean = true
    var drawLabels: Boolean = true
    var labelCount: Int = 5
    var axisLineEnabled: Boolean = true
}
