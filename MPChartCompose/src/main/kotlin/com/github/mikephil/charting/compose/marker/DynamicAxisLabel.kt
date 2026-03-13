package com.github.mikephil.charting.compose.marker

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicText

@Composable
internal fun DynamicYAxisLabel(
    pixelY: Float,
    value: Float,
    formatter: ((Float) -> String)?,
    labelColor: Color,
    backgroundColor: Color,
    chartLeft: Float,
) {
    val animatedY by animateFloatAsState(
        targetValue = pixelY,
        animationSpec = spring(),
        label = "y_axis_label",
    )

    val text = formatter?.invoke(value) ?: value.toString()
    val bgColor = if (backgroundColor != Color.Unspecified) backgroundColor else Color(0xFF333333)

    Box(
        modifier = Modifier
            .offset { IntOffset(0, (animatedY - 12).toInt()) }
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        BasicText(
            text = text,
            style = TextStyle(color = labelColor, fontSize = 10.sp),
        )
    }
}

@Composable
internal fun DynamicXAxisLabel(
    pixelX: Float,
    value: Float,
    formatter: ((Float) -> String)?,
    labelColor: Color,
    backgroundColor: Color,
    chartBottom: Float,
) {
    val animatedX by animateFloatAsState(
        targetValue = pixelX,
        animationSpec = spring(),
        label = "x_axis_label",
    )

    val text = formatter?.invoke(value) ?: value.toString()
    val bgColor = if (backgroundColor != Color.Unspecified) backgroundColor else Color(0xFF333333)

    Box(
        modifier = Modifier
            .offset { IntOffset((animatedX - 30).toInt(), chartBottom.toInt()) }
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        BasicText(
            text = text,
            style = TextStyle(color = labelColor, fontSize = 10.sp),
        )
    }
}
