package com.github.mikephil.charting.compose.animation

import org.junit.Assert.*
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ChartAnimationTest {

    @Test
    fun `None is singleton`() {
        assertSame(ChartAnimation.None, ChartAnimation.None)
    }

    @Test
    fun `EaseInOut stores duration`() {
        val anim = ChartAnimation.EaseInOut(1.seconds)
        assertEquals(1000L, anim.duration.inWholeMilliseconds)
    }

    @Test
    fun `Spring stores damping and stiffness`() {
        val anim = ChartAnimation.Spring(dampingRatio = 0.5f, stiffness = 200f)
        assertEquals(0.5f, anim.dampingRatio, 0.001f)
        assertEquals(200f, anim.stiffness, 0.001f)
    }

    @Test
    fun `EaseIn and EaseOut store duration`() {
        val easeIn = ChartAnimation.EaseIn(500.milliseconds)
        val easeOut = ChartAnimation.EaseOut(800.milliseconds)
        assertEquals(500L, easeIn.duration.inWholeMilliseconds)
        assertEquals(800L, easeOut.duration.inWholeMilliseconds)
    }
}
