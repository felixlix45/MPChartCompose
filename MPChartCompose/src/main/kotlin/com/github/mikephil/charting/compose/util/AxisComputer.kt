package com.github.mikephil.charting.compose.util

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Port of the original MPAndroidChart axis computation algorithms from
 * [Utils.roundToNextSignificant] and [AxisRenderer.computeAxisValues].
 */
object AxisComputer {

    data class AxisResult(
        val labels: List<Float>,
        val interval: Float,
        val decimals: Int,
    )

    /**
     * Rounds the given number to the next significant number.
     * Ported from Utils.java:355-366.
     */
    fun roundToNextSignificant(number: Double): Float {
        if (number.isInfinite() || number.isNaN() || number == 0.0) return 0f

        val d = ceil(log10(if (number < 0) -number else number).toFloat().toDouble()).toFloat()
        val pw = 1 - d.toInt()
        val magnitude = 10.0.pow(pw)
        val shifted = (number * magnitude).roundToLong()
        return (shifted / magnitude).toFloat()
    }

    /**
     * Computes axis label values using the "nice number" algorithm.
     * Ported from AxisRenderer.java:149-264.
     *
     * @param min       The minimum value of the axis range.
     * @param max       The maximum value of the axis range.
     * @param labelCount Desired number of labels.
     * @param granularity Minimum interval between labels, or null to disable.
     * @param forceLabels If true, uses simple linear interpolation (original behavior).
     */
    fun computeAxisValues(
        min: Float,
        max: Float,
        labelCount: Int,
        granularity: Float?,
        forceLabels: Boolean,
    ): AxisResult {
        val range = abs(max - min).toDouble()

        if (labelCount == 0 || range <= 0 || range.isInfinite()) {
            return AxisResult(emptyList(), 0f, 0)
        }

        // Find out how much spacing (in value space) between axis values
        val rawInterval = range / labelCount
        var interval = roundToNextSignificant(rawInterval).toDouble()

        // Enforce granularity
        if (granularity != null && interval < granularity) {
            interval = granularity.toDouble()
        }

        // Normalize interval: if leading significant digit > 5, bump to next power of 10
        val intervalMagnitude =
            roundToNextSignificant(10.0.pow(log10(interval).toInt().toDouble())).toDouble()
        val intervalSigDigit = (interval / intervalMagnitude).toInt()
        if (intervalSigDigit > 5) {
            val floored = floor(10.0 * intervalMagnitude)
            if (floored != 0.0) {
                interval = floored
            }
        }

        // Force label count: simple linear interpolation
        if (forceLabels) {
            val forcedInterval = range / (labelCount - 1).coerceAtLeast(1)
            val labels = (0 until labelCount).map { i ->
                (min + i * forcedInterval).toFloat()
            }
            val decimals = if (forcedInterval < 1) ceil(-log10(forcedInterval)).toInt() else 0
            return AxisResult(labels, forcedInterval.toFloat(), decimals)
        }

        // Snap first label to ceil(min/interval) * interval
        val first = if (interval == 0.0) 0.0 else ceil(min / interval) * interval
        val last = if (interval == 0.0) 0.0 else Math.nextUp(floor(max / interval) * interval)

        // Count and collect labels
        val labels = mutableListOf<Float>()
        if (interval != 0.0 && last != first) {
            var f = first
            while (f <= last) {
                // Fix for negative zero case
                labels.add(if (f == 0.0) 0f else f.toFloat())
                f += interval
            }
        } else if (last == first) {
            labels.add(first.toFloat())
        }

        val decimals = if (interval < 1) ceil(-log10(interval)).toInt() else 0
        return AxisResult(labels, interval.toFloat(), decimals)
    }
}
