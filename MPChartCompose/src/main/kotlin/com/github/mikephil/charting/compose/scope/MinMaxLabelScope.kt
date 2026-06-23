package com.github.mikephil.charting.compose.scope

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit

/** Controls labels drawn at the highest and lowest y-values of the chart. */
class MinMaxLabelScope {
    /** Set to false to hide both labels entirely. */
    var enabled: Boolean = true
    /** Custom number formatter; defaults to the raw float string if null. */
    var formatter: ((Float) -> String)? = null
    /** Color of the maximum-value label. Falls back to the chart's axis label color. */
    var maxTextColor: Color = Color.Unspecified
    /** Color of the minimum-value label. Falls back to the chart's axis label color. */
    var minTextColor: Color = Color.Unspecified
    /** Font size of both labels. Falls back to the chart's axis label size. */
    var textSize: TextUnit = TextUnit.Unspecified
    /** Font weight of both labels (e.g. FontWeight.Bold). */
    var fontWeight: FontWeight = FontWeight.Normal
}
