package com.github.mikephil.charting.compose.data

data class LineData(val dataSets: List<LineDataSet>)

data class BarData(
    val dataSets: List<BarDataSet>,
    val barWidth: Float = 0.85f,
)

data class CandleData(val dataSets: List<CandleDataSet>)
