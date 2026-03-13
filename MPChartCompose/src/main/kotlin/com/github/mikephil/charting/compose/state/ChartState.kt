package com.github.mikephil.charting.compose.state

import com.github.mikephil.charting.compose.data.*

interface LineChartState {
    val data: LineData
    fun append(dataSetIndex: Int, entry: Entry)
    fun appendAll(dataSetIndex: Int, entries: List<Entry>)
    fun replaceData(data: LineData)
    fun clear()
    fun scrollToEnd(animated: Boolean = true)
    fun scrollTo(x: Float, animated: Boolean = true)
    fun setVisibleXRange(range: Float)
}

interface BarChartState {
    val data: BarData
    fun append(dataSetIndex: Int, entry: BarEntry)
    fun appendAll(dataSetIndex: Int, entries: List<BarEntry>)
    fun replaceData(data: BarData)
    fun clear()
    fun scrollToEnd(animated: Boolean = true)
    fun scrollTo(x: Float, animated: Boolean = true)
    fun setVisibleXRange(range: Float)
}

interface CandleChartState {
    val data: CandleData
    fun append(dataSetIndex: Int, entry: CandleEntry)
    fun appendAll(dataSetIndex: Int, entries: List<CandleEntry>)
    fun replaceData(data: CandleData)
    fun clear()
    fun scrollToEnd(animated: Boolean = true)
    fun scrollTo(x: Float, animated: Boolean = true)
    fun setVisibleXRange(range: Float)
}
