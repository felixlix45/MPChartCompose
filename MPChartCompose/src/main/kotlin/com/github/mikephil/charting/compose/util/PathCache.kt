package com.github.mikephil.charting.compose.util

import androidx.compose.ui.graphics.Path

internal class PathCache {
    var path: Path = Path()
        private set

    var isValid: Boolean = false
        private set

    private var lastEntryCount: Int = 0

    fun markValid(entryCount: Int) {
        isValid = true
        lastEntryCount = entryCount
    }

    fun invalidate() {
        isValid = false
        lastEntryCount = 0
        path.reset()
    }

    fun isAppendOnly(newEntryCount: Int): Boolean {
        return isValid && newEntryCount > lastEntryCount
    }

    fun appendStartIndex(): Int = lastEntryCount
}
