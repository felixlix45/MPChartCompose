package com.github.mikephil.charting.compose.scope

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

class GestureScope {
    var tapToHighlight: Boolean = true
    var dragToPan: Boolean = true
    var dragToScrub: Boolean = true
    var longPressToDrag: Boolean = false
    var flingDeceleration: Boolean = true
    var scrubThreshold: Dp = 48.dp
}
