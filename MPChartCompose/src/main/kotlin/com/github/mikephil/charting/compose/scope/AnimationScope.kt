package com.github.mikephil.charting.compose.scope

import com.github.mikephil.charting.compose.animation.ChartAnimation
import kotlin.time.Duration.Companion.seconds

class AnimationScope {
    var appear: ChartAnimation = ChartAnimation.EaseInOut(1.seconds)
    var dataTransition: ChartAnimation = ChartAnimation.None
}
