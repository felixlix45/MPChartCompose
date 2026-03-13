package com.github.mikephil.charting.compose.marker

import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
internal fun MarkerPositioner(
    position: Offset,
    chartSize: IntSize,
    verticalOffset: Float = 16f,
    content: @Composable () -> Unit,
) {
    val animatedPosition by animateOffsetAsState(
        targetValue = position,
        animationSpec = spring(),
        label = "marker_position",
    )

    var markerSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    // Calculate position with edge avoidance
    val x = (animatedPosition.x - markerSize.width / 2f)
        .coerceIn(0f, (chartSize.width - markerSize.width).toFloat().coerceAtLeast(0f))

    // Default: above the point; flip below if clipping top
    val yAbove = animatedPosition.y - markerSize.height - verticalOffset
    val yBelow = animatedPosition.y + verticalOffset
    val y = if (yAbove >= 0f) yAbove else yBelow

    Box(
        modifier = Modifier
            .offset { IntOffset(x.toInt(), y.toInt()) }
            .onSizeChanged { markerSize = it }
    ) {
        content()
    }
}
