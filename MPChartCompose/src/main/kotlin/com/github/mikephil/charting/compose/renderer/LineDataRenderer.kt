package com.github.mikephil.charting.compose.renderer

import android.graphics.BlurMaskFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import com.github.mikephil.charting.compose.data.Entry
import com.github.mikephil.charting.compose.data.LineDataSet
import com.github.mikephil.charting.compose.data.LineGlow
import com.github.mikephil.charting.compose.data.LineMode
import com.github.mikephil.charting.compose.util.PathCache
import com.github.mikephil.charting.compose.util.Transformer

internal object LineDataRenderer {

    fun DrawScope.drawLineData(
        dataSets: List<LineDataSet>,
        transformer: Transformer,
        appearPhase: Float,
        pathCaches: List<PathCache>,
        defaultLineColor: Color,
    ) {
        for ((dsIndex, dataSet) in dataSets.withIndex()) {
            val entries = dataSet.entries
            if (entries.isEmpty()) continue

            val lineColor = if (dataSet.lineColor != Color.Unspecified) dataSet.lineColor else defaultLineColor
            val lineWidthPx = dataSet.lineWidth.toPx()
            val cache = pathCaches.getOrNull(dsIndex) ?: PathCache()

            // Build or update path
            val path = buildPath(entries, dataSet.mode, transformer, cache, appearPhase)

            // Draw glow effect (3-pass)
            dataSet.glow?.let { glow ->
                if (glow.enabled) {
                    drawGlow(path, glow, lineColor, lineWidthPx)
                }
            }

            // Draw the main line
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = lineWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )

            // Draw fill
            if (dataSet.fillEnabled) {
                drawFill(path, entries, transformer, dataSet, lineColor)
            }

            // Draw circles
            if (dataSet.drawCircles) {
                drawCircles(entries, transformer, lineColor, dataSet.circleRadius, appearPhase)
            }

            // Mark path cache valid
            cache.markValid(entryCount = entries.size)
        }
    }

    private fun DrawScope.buildPath(
        entries: List<Entry>,
        mode: LineMode,
        transformer: Transformer,
        cache: PathCache,
        appearPhase: Float,
    ): Path {
        val visibleCount = (entries.size * appearPhase).toInt().coerceIn(1, entries.size)

        if (cache.isAppendOnly(visibleCount)) {
            // Append-only: reuse cached path and add new segments
            val path = cache.path
            val startIdx = cache.appendStartIndex()
            for (i in startIdx until visibleCount) {
                val pixel = transformer.dataToPixel(entries[i].x, entries[i].y)
                path.lineTo(pixel.x, pixel.y)
            }
            return path
        }

        // Full rebuild
        val path = cache.path
        path.reset()

        if (visibleCount == 0) return path

        val first = transformer.dataToPixel(entries[0].x, entries[0].y)
        path.moveTo(first.x, first.y)

        when (mode) {
            LineMode.Linear -> {
                for (i in 1 until visibleCount) {
                    val p = transformer.dataToPixel(entries[i].x, entries[i].y)
                    path.lineTo(p.x, p.y)
                }
            }
            LineMode.CubicBezier -> {
                for (i in 1 until visibleCount) {
                    val prev = transformer.dataToPixel(entries[i - 1].x, entries[i - 1].y)
                    val curr = transformer.dataToPixel(entries[i].x, entries[i].y)
                    val cpx = (prev.x + curr.x) / 2f
                    path.cubicTo(cpx, prev.y, cpx, curr.y, curr.x, curr.y)
                }
            }
            LineMode.HorizontalBezier -> {
                for (i in 1 until visibleCount) {
                    val prev = transformer.dataToPixel(entries[i - 1].x, entries[i - 1].y)
                    val curr = transformer.dataToPixel(entries[i].x, entries[i].y)
                    val cpx = (prev.x + curr.x) / 2f
                    path.cubicTo(cpx, prev.y, cpx, curr.y, curr.x, curr.y)
                }
            }
            LineMode.Stepped -> {
                for (i in 1 until visibleCount) {
                    val prev = transformer.dataToPixel(entries[i - 1].x, entries[i - 1].y)
                    val curr = transformer.dataToPixel(entries[i].x, entries[i].y)
                    path.lineTo(curr.x, prev.y)
                    path.lineTo(curr.x, curr.y)
                }
            }
        }

        return path
    }

    private fun DrawScope.drawGlow(
        path: Path,
        glow: LineGlow,
        lineColor: Color,
        lineWidthPx: Float,
    ) {
        val glowColor = if (glow.color != Color.Unspecified) glow.color else lineColor
        val glowRadiusPx = glow.radius.toPx()
        if (glowRadiusPx <= 0f) return

        // Pad the bitmap so the blur isn't clipped at the edges
        val padding = (glowRadiusPx * 1.5f).toInt().coerceAtLeast(1)
        val bitmapW = (size.width.toInt() + padding * 2).coerceAtLeast(1)
        val bitmapH = (size.height.toInt() + padding * 2).coerceAtLeast(1)
        val bitmap = android.graphics.Bitmap.createBitmap(
            bitmapW, bitmapH, android.graphics.Bitmap.Config.ARGB_8888
        )
        val softwareCanvas = android.graphics.Canvas(bitmap)
        softwareCanvas.translate(padding.toFloat(), padding.toFloat())

        val androidPath = path.asAndroidPath()

        // Outer glow: wide blur, thicker stroke, boosted alpha to survive the spread
        val outerPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = glowColor.copy(alpha = (glow.alpha * 2f).coerceAtMost(1f)).toArgb()
            strokeWidth = lineWidthPx + glowRadiusPx * 0.5f
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
            maskFilter = BlurMaskFilter(glowRadiusPx, BlurMaskFilter.Blur.NORMAL)
        }
        softwareCanvas.drawPath(androidPath, outerPaint)

        // Inner glow: tighter blur, brighter — creates the bright halo near the line
        val innerPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = glowColor.copy(alpha = (glow.alpha * 3f).coerceAtMost(1f)).toArgb()
            strokeWidth = lineWidthPx
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
            maskFilter = BlurMaskFilter(glowRadiusPx * 0.4f, BlurMaskFilter.Blur.NORMAL)
        }
        softwareCanvas.drawPath(androidPath, innerPaint)

        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawBitmap(bitmap, -padding.toFloat(), -padding.toFloat(), null)
        }
        bitmap.recycle()
    }

    private fun DrawScope.drawFill(
        linePath: Path,
        entries: List<Entry>,
        transformer: Transformer,
        dataSet: LineDataSet,
        lineColor: Color,
    ) {
        if (entries.isEmpty()) return

        val fillPath = Path().apply {
            addPath(linePath)
            // Close to bottom
            val lastPixel = transformer.dataToPixel(entries.last().x, entries.last().y)
            val firstPixel = transformer.dataToPixel(entries.first().x, entries.first().y)
            lineTo(lastPixel.x, transformer.contentRect.bottom)
            lineTo(firstPixel.x, transformer.contentRect.bottom)
            close()
        }

        if (dataSet.fillBrush != null) {
            drawPath(path = fillPath, brush = dataSet.fillBrush)
        } else {
            val fillColor = if (dataSet.fillColor != Color.Unspecified) dataSet.fillColor else lineColor.copy(alpha = 0.3f)
            drawPath(path = fillPath, color = fillColor)
        }
    }

    private fun DrawScope.drawCircles(
        entries: List<Entry>,
        transformer: Transformer,
        color: Color,
        radius: Dp,
        appearPhase: Float,
    ) {
        val radiusPx = radius.toPx()
        val visibleCount = (entries.size * appearPhase).toInt().coerceIn(1, entries.size)
        for (i in 0 until visibleCount.coerceAtMost(entries.size)) {
            val pixel = transformer.dataToPixel(entries[i].x, entries[i].y)
            drawCircle(color = color, radius = radiusPx, center = pixel)
        }
    }
}
