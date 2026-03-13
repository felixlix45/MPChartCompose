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

internal fun Modifier.chartGestureHandler(
    tapToHighlight: Boolean,
    dragToPan: Boolean,
    dragToScrub: Boolean,
    flingEnabled: Boolean,
    scrubThreshold: Dp,
    highlightState: MutableState<Highlight?>,
    transformerProvider: () -> Transformer?,
    findNearestIndexByX: (targetX: Float) -> Int,
    getEntryX: (index: Int) -> Float,
    getEntryY: (index: Int) -> Float,
    onTranslationChange: (Float) -> Unit,
    onValueSelected: ((Int) -> Unit)?,
): Modifier = this.pointerInput(
    tapToHighlight, dragToPan, dragToScrub, flingEnabled
) {
    val flingDecelerator = FlingDecelerator()
    val scrubThresholdPx = scrubThreshold.toPx()

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

            // Handle tap
            if (tapToHighlight && nearestIdx >= 0) {
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
