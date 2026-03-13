package com.github.mikephil.charting.compose.scope

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.github.mikephil.charting.compose.data.LegendPosition
import com.github.mikephil.charting.compose.data.LegendOrientation

class LegendScope {
    var enabled: Boolean = true
    var position: LegendPosition = LegendPosition.Top
    var orientation: LegendOrientation = LegendOrientation.Horizontal
    var formSize: Dp = 10.dp
    var textColor: Color = Color.Unspecified
    var textSize: TextUnit = TextUnit.Unspecified
}
