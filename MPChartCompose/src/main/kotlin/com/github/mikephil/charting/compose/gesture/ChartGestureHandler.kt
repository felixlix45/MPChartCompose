package com.github.mikephil.charting.compose.gesture

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.Dp
import com.github.mikephil.charting.compose.highlight.Highlight
import com.github.mikephil.charting.compose.util.Transformer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

internal fun Modifier.chartGestureHandler(
    tapToHighlight: Boolean,
    dragToPan: Boolean,
    dragToScrub: Boolean,
    longPressToDrag: Boolean,
    flingEnabled: Boolean,
    scrubThreshold: Dp,
    highlightState: MutableState<Highlight?>,
    transformerProvider: () -> Transformer?,
    findNearestIndexByX: (targetX: Float) -> Int,
    getEntryX: (index: Int) -> Float,
    getEntryY: (index: Int) -> Float,
    onTranslationChange: (Float) -> Unit,
    onValueSelected: ((Int) -> Unit)?,
    onNothingSelected: (() -> Unit)?,
    onScrubTouchDown: (() -> Unit)? = null,
    onScrubStarted: (() -> Unit)? = null,
): Modifier = this.pointerInput(
    tapToHighlight, dragToPan, dragToScrub, longPressToDrag, flingEnabled
) {
    val flingDecelerator = FlingDecelerator()
    val scrubThresholdPx = scrubThreshold.toPx()
    val longPressTimeoutMs = viewConfiguration.longPressTimeoutMillis
    val touchSlopPx = viewConfiguration.touchSlop

    fun highlightAtIndex(idx: Int, transformer: Transformer, notifySelection: Boolean = true) {
        if (idx < 0) return
        val entryX = getEntryX(idx)
        val entryY = getEntryY(idx)
        val pixel = transformer.dataToPixel(entryX, entryY)
        highlightState.value = Highlight(
            dataSetIndex = 0,
            entryIndex = idx,
            x = entryX,
            y = entryY,
            pixelPosition = pixel,
        )
        if (notifySelection) {
            onValueSelected?.invoke(idx)
        }
    }

    fun scrubAtPosition(position: Offset, transformer: Transformer) {
        val (scrubDataX, _) = transformer.pixelToData(position)
        highlightAtIndex(findNearestIndexByX(scrubDataX), transformer)
    }

    fun clearScrubHighlight() {
        highlightState.value = null
        onNothingSelected?.invoke()
    }

    coroutineScope {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val transformer = transformerProvider() ?: return@awaitEachGesture

            // Determine if this is a scrub or pan
            val (dataX, _) = transformer.pixelToData(down.position)
            val nearestIdx = findNearestIndexByX(dataX)
            val isScrub = if (!dragToScrub) {
                false
            } else if (!dragToPan) {
                true
            } else if (nearestIdx >= 0) {
                val nearestPixel =
                    transformer.dataToPixel(getEntryX(nearestIdx), getEntryY(nearestIdx))
                val dist = (down.position - nearestPixel).getDistance()
                dist <= scrubThresholdPx
            } else {
                false
            }

            // Instant scrub: wait for direction — horizontal starts scrub, vertical goes to parent scroll.
            // Do not lock the AppBar or show crosshair until horizontal drag is confirmed.
            if (isScrub && dragToScrub && !longPressToDrag) {
                var scrubPosition = down.position
                var horizontalScrub = false
                var pastSlop = false

                while (!pastSlop) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id }
                    if (change == null || !change.pressed) {
                        return@awaitEachGesture
                    }
                    val delta = change.position - down.position
                    if (delta.getDistance() > touchSlopPx) {
                        pastSlop = true
                        horizontalScrub = abs(delta.x) > abs(delta.y)
                        scrubPosition = change.position
                    }
                }

                if (!horizontalScrub) {
                    return@awaitEachGesture
                }

                onScrubStarted?.invoke()
                scrubAtPosition(scrubPosition, transformer)

                drag(down.id) { change ->
                    val currentTransformer = transformerProvider() ?: return@drag
                    scrubAtPosition(change.position, currentTransformer)
                    change.consume()
                }

                clearScrubHighlight()
                return@awaitEachGesture
            }

            var totalDragX = 0f
            var scrubStarted = false

            // Long-press gate: when required for scrub, wait before entering scrub mode.
            // During the wait, pointer events are NOT consumed so parent scrollable
            // containers can intercept the gesture for scrolling.
            if (isScrub && longPressToDrag) {
                var fingerLifted = false
                val wasCancelled = withTimeoutOrNull(longPressTimeoutMs) {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) {
                            fingerLifted = true
                            break
                        }
                        if ((change.position - down.position).getDistance() > touchSlopPx) {
                            break
                        }
                    }
                }

                if (wasCancelled != null) {
                    // Long-press NOT confirmed — handle as tap if finger was lifted
                    if (fingerLifted && tapToHighlight && nearestIdx >= 0) {
                        val currentHighlight = highlightState.value
                        if (currentHighlight != null && currentHighlight.entryIndex == nearestIdx) {
                            clearScrubHighlight()
                        } else {
                            highlightAtIndex(nearestIdx, transformer)
                        }
                    }
                    return@awaitEachGesture
                }

                // Long-press confirmed → set initial highlight and begin scrub
                highlightAtIndex(nearestIdx, transformer)
                onScrubStarted?.invoke()
                scrubStarted = true
            } else {
                // Handle tap (original behavior, no long-press gate)
                if (tapToHighlight && nearestIdx >= 0) {
                    val currentHighlight = highlightState.value
                    if (currentHighlight != null && currentHighlight.entryIndex == nearestIdx) {
                        clearScrubHighlight()
                    } else {
                        highlightAtIndex(nearestIdx, transformer)
                    }
                }
            }

            drag(down.id) { change ->
                val dragAmount = change.positionChange()

                if (isScrub && dragToScrub) {
                    if (!scrubStarted) {
                        scrubStarted = true
                        onScrubStarted?.invoke()
                    }
                    val currentTransformer = transformerProvider() ?: return@drag
                    scrubAtPosition(change.position, currentTransformer)
                } else if (dragToPan) {
                    totalDragX += dragAmount.x
                    val currentTransformer = transformerProvider() ?: return@drag
                    val xSpan =
                        currentTransformer.dataXRange.endInclusive - currentTransformer.dataXRange.start
                    val dataPerPixel = xSpan / currentTransformer.contentRect.width
                    onTranslationChange(-dragAmount.x * dataPerPixel)
                }

                change.consume()
            }

            // Clear highlight after scrub ends
            if (isScrub) {
                clearScrubHighlight()
            }

            // Fling after pan
            if (!isScrub && dragToPan && flingEnabled && totalDragX != 0f) {
                val currentTransformer = transformerProvider() ?: return@awaitEachGesture
                val velocity = totalDragX * 2f // Simple velocity estimate
                launch {
                    flingDecelerator.fling(velocity) { delta ->
                        val xSpan =
                            currentTransformer.dataXRange.endInclusive - currentTransformer.dataXRange.start
                        val dataPerPixel = xSpan / currentTransformer.contentRect.width
                        onTranslationChange(-delta * dataPerPixel)
                    }
                }
            }
        }
    }
}
