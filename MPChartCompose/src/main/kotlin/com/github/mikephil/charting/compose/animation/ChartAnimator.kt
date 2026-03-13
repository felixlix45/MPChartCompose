package com.github.mikephil.charting.compose.animation

import androidx.compose.animation.core.*
import kotlin.time.Duration

internal class ChartAnimator {

    var appearPhase: Float = 0f
        private set

    var transitionPhase: Float = 1f
        private set

    suspend fun animateAppear(animation: ChartAnimation, onFrame: (Float) -> Unit) {
        when (animation) {
            is ChartAnimation.None -> {
                appearPhase = 1f
                onFrame(1f)
            }
            is ChartAnimation.EaseInOut -> animatePhase(animation.duration, FastOutSlowInEasing, onFrame) { appearPhase = it }
            is ChartAnimation.EaseIn -> animatePhase(animation.duration, FastOutLinearInEasing, onFrame) { appearPhase = it }
            is ChartAnimation.EaseOut -> animatePhase(animation.duration, LinearOutSlowInEasing, onFrame) { appearPhase = it }
            is ChartAnimation.Spring -> {
                val animatable = Animatable(0f)
                animatable.animateTo(1f, spring(animation.dampingRatio, animation.stiffness)) {
                    appearPhase = value
                    onFrame(value)
                }
            }
        }
    }

    suspend fun animateTransition(animation: ChartAnimation, onFrame: (Float) -> Unit) {
        transitionPhase = 0f
        when (animation) {
            is ChartAnimation.None -> {
                transitionPhase = 1f
                onFrame(1f)
            }
            is ChartAnimation.EaseInOut -> animatePhase(animation.duration, FastOutSlowInEasing, onFrame) { transitionPhase = it }
            is ChartAnimation.EaseIn -> animatePhase(animation.duration, FastOutLinearInEasing, onFrame) { transitionPhase = it }
            is ChartAnimation.EaseOut -> animatePhase(animation.duration, LinearOutSlowInEasing, onFrame) { transitionPhase = it }
            is ChartAnimation.Spring -> {
                val animatable = Animatable(0f)
                animatable.animateTo(1f, spring(animation.dampingRatio, animation.stiffness)) {
                    transitionPhase = value
                    onFrame(value)
                }
            }
        }
    }

    private suspend fun animatePhase(
        duration: Duration,
        easing: Easing,
        onFrame: (Float) -> Unit,
        setter: (Float) -> Unit,
    ) {
        val animatable = Animatable(0f)
        animatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = duration.inWholeMilliseconds.toInt(),
                easing = easing,
            ),
        ) {
            setter(value)
            onFrame(value)
        }
    }
}
