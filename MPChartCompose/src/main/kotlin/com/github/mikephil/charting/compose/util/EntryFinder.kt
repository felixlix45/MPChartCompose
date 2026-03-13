package com.github.mikephil.charting.compose.util

import com.github.mikephil.charting.compose.data.Entry
import kotlin.math.abs

object EntryFinder {

    fun findNearest(entries: List<Entry>, targetX: Float): Entry? {
        val idx = findNearestIndex(entries, targetX)
        return if (idx >= 0) entries[idx] else null
    }

    fun findNearestIndex(entries: List<Entry>, targetX: Float): Int {
        return findNearestIndexByX(entries.map { it.x }, targetX)
    }

    fun findNearestIndexByX(xValues: List<Float>, targetX: Float): Int {
        if (xValues.isEmpty()) return -1
        if (xValues.size == 1) return 0

        var low = 0
        var high = xValues.size - 1
        while (low < high) {
            val mid = (low + high) / 2
            if (xValues[mid] < targetX) low = mid + 1
            else high = mid
        }
        return when {
            low == 0 -> 0
            low >= xValues.size -> xValues.size - 1
            else -> {
                val distLow = abs(xValues[low] - targetX)
                val distPrev = abs(xValues[low - 1] - targetX)
                if (distPrev <= distLow) low - 1 else low
            }
        }
    }
}
