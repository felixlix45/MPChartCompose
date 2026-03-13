package com.github.mikephil.charting.compose.scope

import com.github.mikephil.charting.compose.data.LimitLine

class YAxisScope {
    var formatter: ((Float) -> String)? = null
    var gridEnabled: Boolean = true
    var drawLabels: Boolean = true
    var labelCount: Int = 5
    var axisLineEnabled: Boolean = true
    var limitLines: List<LimitLine> = emptyList()
}
