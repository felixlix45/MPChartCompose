package com.github.mikephil.charting.compose.data

data class Entry(
    val x: Float,
    val y: Float,
    val data: Any? = null,
)

data class BarEntry(
    val x: Float,
    val y: Float,
    val stackValues: List<Float>? = null,
    val data: Any? = null,
) {
    val effectiveY: Float
        get() = stackValues?.sum() ?: y
}

data class CandleEntry(
    val x: Float,
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float,
    val data: Any? = null,
)
