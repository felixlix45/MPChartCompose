package com.github.mikephil.charting.compose.gesture

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay

internal class FlingDecelerator {
    private val animatable = Animatable(0f)

    suspend fun fling(
        initialVelocity: Float,
        onFrame: (delta: Float) -> Unit,
    ) {
        var previousValue = 0f
        animatable.snapTo(0f)
        animatable.animateDecay(
            initialVelocity = initialVelocity,
            animationSpec = exponentialDecay(frictionMultiplier = 1.5f),
        ) {
            val delta = value - previousValue
            previousValue = value
            onFrame(delta)
        }
    }

    fun cancel() {
        // No-op; animation cancellation handled by coroutine scope
    }
}
