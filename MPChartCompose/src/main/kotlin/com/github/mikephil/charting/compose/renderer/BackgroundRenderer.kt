package com.github.mikephil.charting.compose.renderer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

internal object BackgroundRenderer {
    fun DrawScope.drawBackground(color: Color) {
        drawRect(color = color)
    }
}
