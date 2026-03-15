package com.github.mikephil.charting.compose.scope

import com.github.mikephil.charting.compose.data.LimitLine
import com.github.mikephil.charting.compose.data.YAxisPosition

class YAxisScope {
    var position: YAxisPosition = YAxisPosition.Left
    var formatter: ((Float) -> String)? = null
    var gridEnabled: Boolean = true
    var drawLabels: Boolean = true
    var labelCount: Int = 6
    var axisLineEnabled: Boolean = true
    var limitLines: List<LimitLine> = emptyList()
    var spaceTop: Float = 10f
    var spaceBottom: Float = 10f
    var axisMinimum: Float? = null
    var axisMaximum: Float? = null
    var granularity: Float? = null
    var forceLabels: Boolean = false
    var inverted: Boolean = false
}
