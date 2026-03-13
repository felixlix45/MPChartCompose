package com.github.mikephil.charting.compose.style

import androidx.compose.ui.graphics.Color
import com.github.mikephil.charting.compose.scope.ChartStyleScope
import org.junit.Assert.*
import org.junit.Test

class ChartStyleTest {

    @Test
    fun `default style has dark theme colors`() {
        val style = ChartStyle()
        assertEquals(Color(0xFF1A1A1A), style.backgroundColor)
        assertEquals(Color(0xFF2A2A2A), style.gridColor)
    }

    @Test
    fun `style mergeWith overrides specified scope values`() {
        val base = ChartStyle()
        val scope = ChartStyleScope().apply { backgroundColor = Color.Red }
        val merged = base.mergeWith(scope)
        assertEquals(Color.Red, merged.backgroundColor)
        assertEquals(base.gridColor, merged.gridColor)
    }
}
