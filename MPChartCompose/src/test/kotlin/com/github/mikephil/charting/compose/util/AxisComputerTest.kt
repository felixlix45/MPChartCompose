package com.github.mikephil.charting.compose.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AxisComputerTest {

    // --- roundToNextSignificant tests ---

    @Test
    fun `roundToNextSignificant - 38 rounds to 40`() {
        assertEquals(40f, AxisComputer.roundToNextSignificant(38.0))
    }

    @Test
    fun `roundToNextSignificant - 3_17 rounds to 3`() {
        assertEquals(3f, AxisComputer.roundToNextSignificant(3.17))
    }

    @Test
    fun `roundToNextSignificant - 0_45 rounds to 0_5`() {
        assertEquals(0.5f, AxisComputer.roundToNextSignificant(0.45))
    }

    @Test
    fun `roundToNextSignificant - 156 rounds to 200`() {
        assertEquals(200f, AxisComputer.roundToNextSignificant(156.0))
    }

    @Test
    fun `roundToNextSignificant - zero returns zero`() {
        assertEquals(0f, AxisComputer.roundToNextSignificant(0.0))
    }

    @Test
    fun `roundToNextSignificant - NaN returns zero`() {
        assertEquals(0f, AxisComputer.roundToNextSignificant(Double.NaN))
    }

    @Test
    fun `roundToNextSignificant - Infinity returns zero`() {
        assertEquals(0f, AxisComputer.roundToNextSignificant(Double.POSITIVE_INFINITY))
    }

    // --- computeAxisValues tests ---
    // Test inputs use padded min/max (after spaceTop/spaceBottom applied by chart)

    @Test
    fun `data 48-238 produces labels 40, 80, 120, 160, 200, 240`() {
        // After 10% padding: min=29, max=257
        val result = AxisComputer.computeAxisValues(29f, 257f, 6, null, false)
        assertEquals(listOf(40f, 80f, 120f, 160f, 200f, 240f), result.labels)
        assertEquals(40f, result.interval)
    }

    @Test
    fun `data 0_5-3_2 produces labels 0_5, 1_0, 1_5, 2_0, 2_5, 3_0`() {
        // After 10% padding: min=0.23, max=3.47
        val result = AxisComputer.computeAxisValues(0.23f, 3.47f, 6, null, false)
        assertEquals(6, result.labels.size)
        assertEquals(0.5f, result.labels[0], 0.01f)
        assertEquals(1.0f, result.labels[1], 0.01f)
        assertEquals(1.5f, result.labels[2], 0.01f)
        assertEquals(2.0f, result.labels[3], 0.01f)
        assertEquals(2.5f, result.labels[4], 0.01f)
        assertEquals(3.0f, result.labels[5], 0.01f)
    }

    @Test
    fun `data 0-10000 produces labels 0, 2000, 4000, 6000, 8000, 10000`() {
        // After 10% padding: min=-1000, max=11000
        val result = AxisComputer.computeAxisValues(-1000f, 11000f, 6, null, false)
        assertEquals(listOf(0f, 2000f, 4000f, 6000f, 8000f, 10000f), result.labels)
        assertEquals(2000f, result.interval)
    }

    @Test
    fun `data -150 to -20 produces labels -150, -120, -90, -60, -30`() {
        // After 10% padding: min=-163, max=-7
        val result = AxisComputer.computeAxisValues(-163f, -7f, 6, null, false)
        assertEquals(5, result.labels.size)
        assertEquals(-150f, result.labels[0], 0.01f)
        assertEquals(-120f, result.labels[1], 0.01f)
        assertEquals(-90f, result.labels[2], 0.01f)
        assertEquals(-60f, result.labels[3], 0.01f)
        assertEquals(-30f, result.labels[4], 0.01f)
    }

    @Test
    fun `data -50 to 200 produces labels -50, 0, 50, 100, 150, 200`() {
        // After 10% padding: min=-75, max=225
        val result = AxisComputer.computeAxisValues(-75f, 225f, 6, null, false)
        assertEquals(6, result.labels.size)
        assertEquals(-50f, result.labels[0], 0.01f)
        assertEquals(0f, result.labels[1], 0.01f)
        assertEquals(50f, result.labels[2], 0.01f)
        assertEquals(100f, result.labels[3], 0.01f)
        assertEquals(150f, result.labels[4], 0.01f)
        assertEquals(200f, result.labels[5], 0.01f)
    }

    @Test
    fun `all data equal to 100 produces labels around 98_8 to 101_2`() {
        // range=0 → adjMin=99, adjMax=101, adjRange=2
        // After 10% padding: min=98.8, max=101.2
        val result = AxisComputer.computeAxisValues(98.8f, 101.2f, 6, null, false)
        assertTrue("Should have labels", result.labels.isNotEmpty())
        assertTrue("First label >= 98.8", result.labels.first() >= 98.7f)
        assertTrue("Last label <= 101.3", result.labels.last() <= 101.3f)
    }

    @Test
    fun `forceLabels uses linear interpolation`() {
        val result = AxisComputer.computeAxisValues(0f, 100f, 6, null, true)
        assertEquals(6, result.labels.size)
        assertEquals(0f, result.labels[0], 0.01f)
        assertEquals(20f, result.labels[1], 0.01f)
        assertEquals(40f, result.labels[2], 0.01f)
        assertEquals(60f, result.labels[3], 0.01f)
        assertEquals(80f, result.labels[4], 0.01f)
        assertEquals(100f, result.labels[5], 0.01f)
    }

    @Test
    fun `zero range returns empty`() {
        val result = AxisComputer.computeAxisValues(5f, 5f, 6, null, false)
        assertTrue(result.labels.isEmpty())
        assertEquals(0f, result.interval)
    }

    @Test
    fun `zero labelCount returns empty`() {
        val result = AxisComputer.computeAxisValues(0f, 100f, 0, null, false)
        assertTrue(result.labels.isEmpty())
    }

    @Test
    fun `granularity enforces minimum interval`() {
        // Without granularity, interval would be smaller
        val result = AxisComputer.computeAxisValues(0f, 10f, 6, 5f, false)
        assertTrue("Interval should be >= granularity", result.interval >= 5f)
    }

    @Test
    fun `decimals computed correctly for small intervals`() {
        val result = AxisComputer.computeAxisValues(0f, 1f, 6, null, false)
        assertTrue("Decimals should be > 0 for intervals < 1", result.decimals > 0)
    }

    @Test
    fun `decimals is zero for large intervals`() {
        val result = AxisComputer.computeAxisValues(0f, 1000f, 6, null, false)
        assertEquals(0, result.decimals)
    }

    @Test
    fun `interval normalization bumps sigDigit over 5 to next power of 10`() {
        // range=600, labelCount=6 → rawInterval=100 → sigDigit=1 (no bump)
        val result1 = AxisComputer.computeAxisValues(0f, 600f, 6, null, false)
        assertEquals(100f, result1.interval)

        // range=4200, labelCount=6 → rawInterval=700 → rounded=700
        // intervalMagnitude=100, sigDigit=7 → bumped to 1000
        val result2 = AxisComputer.computeAxisValues(0f, 4200f, 6, null, false)
        assertEquals(1000f, result2.interval)
    }
}
