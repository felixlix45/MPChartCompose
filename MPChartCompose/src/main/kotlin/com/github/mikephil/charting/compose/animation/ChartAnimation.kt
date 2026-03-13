package com.github.mikephil.charting.compose.animation

import kotlin.time.Duration

sealed interface ChartAnimation {
    data object None : ChartAnimation
    data class EaseInOut(val duration: Duration) : ChartAnimation
    data class EaseIn(val duration: Duration) : ChartAnimation
    data class EaseOut(val duration: Duration) : ChartAnimation
    data class Spring(val dampingRatio: Float = 0.75f, val stiffness: Float = 1500f) : ChartAnimation
}
