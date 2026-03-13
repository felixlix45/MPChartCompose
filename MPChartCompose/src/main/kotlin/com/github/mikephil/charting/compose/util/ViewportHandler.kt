package com.github.mikephil.charting.compose.util

import com.github.mikephil.charting.compose.data.Entry

class ViewportHandler(
    val visibleXRange: Float?,
    val translationX: Float,
) {
    fun computeVisibleXRange(dataXMin: Float, dataXMax: Float): ClosedFloatingPointRange<Float> {
        val range = visibleXRange ?: return dataXMin..dataXMax
        val end = dataXMax + translationX
        val start = end - range
        return start..end
    }

    fun computeVisibleYRange(
        entries: List<Entry>,
        visibleXRange: ClosedFloatingPointRange<Float>,
        yAutoScale: Boolean,
        yMin: Float?,
        yMax: Float?,
    ): ClosedFloatingPointRange<Float> {
        if (!yAutoScale && yMin != null && yMax != null) return yMin..yMax

        val visibleEntries = entries.filter { it.x in visibleXRange }
        val computedMin = visibleEntries.minOfOrNull { it.y } ?: 0f
        val computedMax = visibleEntries.maxOfOrNull { it.y } ?: 1f

        val finalMin = yMin ?: computedMin
        val finalMax = yMax ?: computedMax
        return finalMin..finalMax
    }

    fun isAtEnd(dataXMax: Float): Boolean {
        return translationX >= -0.01f
    }
}
