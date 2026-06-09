package com.github.mikephil.charting.compose.gesture

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.Dp
import com.github.mikephil.charting.compose.highlight.Highlight
import com.github.mikephil.charting.compose.util.Transformer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

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
): Modifier = this.pointerInput(
    tapToHighlight, dragToPan, dragToScrub, longPressToDrag, flingEnabled
) {
    val flingDecelerator = FlingDecelerator()
    val scrubThresholdPx = scrubThreshold.toPx()
    val longPressTimeoutMs = viewConfiguration.longPressTimeoutMillis
    val touchSlopPx = viewConfiguration.touchSlop

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
                            highlightState.value = null
                            onNothingSelected?.invoke()
                        } else {
                            val entryX = getEntryX(nearestIdx)
                            val entryY = getEntryY(nearestIdx)
                            val pixel = transformer.dataToPixel(entryX, entryY)
                            highlightState.value = Highlight(
                                dataSetIndex = 0,
                                entryIndex = nearestIdx,
                                x = entryX,
                                y = entryY,
                                pixelPosition = pixel,
                            )
                            onValueSelected?.invoke(nearestIdx)
                        }
                    }
                    return@awaitEachGesture
                }

                // Long-press confirmed → set initial highlight
                if (nearestIdx >= 0) {
                    val entryX = getEntryX(nearestIdx)
                    val entryY = getEntryY(nearestIdx)
                    val pixel = transformer.dataToPixel(entryX, entryY)
                    highlightState.value = Highlight(
                        dataSetIndex = 0,
                        entryIndex = nearestIdx,
                        x = entryX,
                        y = entryY,
                        pixelPosition = pixel,
                    )
                    onValueSelected?.invoke(nearestIdx)
                }
            } else {
                // Handle tap (original behavior, no long-press gate)
                if (tapToHighlight && nearestIdx >= 0) {
                    val currentHighlight = highlightState.value
                    if (currentHighlight != null && currentHighlight.entryIndex == nearestIdx) {
                        // Tapping the already-highlighted point → deselect
                        highlightState.value = null
                        onNothingSelected?.invoke()
                    } else {
                        val entryX = getEntryX(nearestIdx)
                        val entryY = getEntryY(nearestIdx)
                        val pixel = transformer.dataToPixel(entryX, entryY)
                        highlightState.value = Highlight(
                            dataSetIndex = 0,
                            entryIndex = nearestIdx,
                            x = entryX,
                            y = entryY,
                            pixelPosition = pixel,
                        )
                        onValueSelected?.invoke(nearestIdx)
                    }
                }
            }

            var totalDragX = 0f

            drag(down.id) { change ->
                val dragAmount = change.positionChange()

                if (isScrub && dragToScrub) {
                    // Scrub mode: find nearest entry to current position
                    val currentTransformer = transformerProvider() ?: return@drag
                    val (scrubDataX, _) = currentTransformer.pixelToData(change.position)
                    val idx = findNearestIndexByX(scrubDataX)
                    if (idx >= 0) {
                        val entryX = getEntryX(idx)
                        val entryY = getEntryY(idx)
                        val pixel = currentTransformer.dataToPixel(entryX, entryY)
                        highlightState.value = Highlight(
                            dataSetIndex = 0,
                            entryIndex = idx,
                            x = entryX,
                            y = entryY,
                            pixelPosition = pixel,
                        )
                        onValueSelected?.invoke(idx)
                    }
                } else if (dragToPan) {
                    // Pan mode
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
                highlightState.value = null
                onNothingSelected?.invoke()
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
