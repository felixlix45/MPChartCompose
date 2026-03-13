package com.github.mikephil.charting.compose.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import com.github.mikephil.charting.compose.data.*

@Composable
fun rememberLineChartState(
    capacity: Int = Int.MAX_VALUE,
    initialData: LineData = LineData(dataSets = emptyList()),
): LineChartState {
    val state = remember { LineChartStateImpl(capacity = capacity, initialData = initialData) }
    LaunchedEffect(state) {
        while (true) {
            withFrameMillis { }
            state.processPendingCommands()
        }
    }
    return state
}

@Composable
fun rememberBarChartState(
    capacity: Int = Int.MAX_VALUE,
    initialData: BarData = BarData(dataSets = emptyList()),
): BarChartState {
    val state = remember { BarChartStateImpl(capacity = capacity, initialData = initialData) }
    LaunchedEffect(state) {
        while (true) {
            withFrameMillis { }
            state.processPendingCommands()
        }
    }
    return state
}

@Composable
fun rememberCandleChartState(
    capacity: Int = Int.MAX_VALUE,
    initialData: CandleData = CandleData(dataSets = emptyList()),
): CandleChartState {
    val state = remember { CandleChartStateImpl(capacity = capacity, initialData = initialData) }
    LaunchedEffect(state) {
        while (true) {
            withFrameMillis { }
            state.processPendingCommands()
        }
    }
    return state
}
