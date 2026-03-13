package com.github.mikephil.charting.compose.state

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import com.github.mikephil.charting.compose.data.*
import com.github.mikephil.charting.compose.util.RingBuffer
import kotlinx.coroutines.channels.Channel

internal class LineChartStateImpl(
    private val capacity: Int,
    initialData: LineData,
) : LineChartState {

    private val buffers: MutableList<RingBuffer<Entry>> = initialData.dataSets.map { ds ->
        RingBuffer<Entry>(capacity).also { buf -> buf.appendAll(ds.entries) }
    }.toMutableList()

    private val configs: MutableList<LineDataSet> = initialData.dataSets.toMutableList()

    private var _version = mutableIntStateOf(0)

    override val data: LineData
        get() {
            _version.intValue
            return LineData(
                dataSets = buffers.mapIndexed { i, buf ->
                    configs[i].copy(entries = buf.toList())
                }
            )
        }

    internal sealed class DataCommand {
        data class Append(val dsIndex: Int, val entry: Entry) : DataCommand()
        data class AppendAll(val dsIndex: Int, val entries: List<Entry>) : DataCommand()
        data class Replace(val data: LineData) : DataCommand()
        data object Clear : DataCommand()
    }

    internal val commandChannel = Channel<DataCommand>(Channel.BUFFERED)

    internal fun appendImmediate(dataSetIndex: Int, entry: Entry) {
        buffers[dataSetIndex].append(entry)
        _version.intValue++
    }

    override fun append(dataSetIndex: Int, entry: Entry) {
        commandChannel.trySend(DataCommand.Append(dataSetIndex, entry))
    }

    override fun appendAll(dataSetIndex: Int, entries: List<Entry>) {
        commandChannel.trySend(DataCommand.AppendAll(dataSetIndex, entries))
    }

    internal fun processPendingCommands() {
        var cmd = commandChannel.tryReceive().getOrNull()
        while (cmd != null) {
            when (cmd) {
                is DataCommand.Append -> buffers[cmd.dsIndex].append(cmd.entry)
                is DataCommand.AppendAll -> buffers[cmd.dsIndex].appendAll(cmd.entries)
                is DataCommand.Replace -> handleReplace(cmd.data)
                is DataCommand.Clear -> buffers.forEach { it.clear() }
            }
            cmd = commandChannel.tryReceive().getOrNull()
        }
        _version.intValue++
    }

    private fun handleReplace(newData: LineData) {
        while (buffers.size < newData.dataSets.size) {
            buffers.add(RingBuffer(capacity))
        }
        while (buffers.size > newData.dataSets.size) {
            buffers.removeAt(buffers.size - 1)
        }
        newData.dataSets.forEachIndexed { i, ds ->
            buffers[i].replaceAll(ds.entries)
        }
        configs.clear()
        configs.addAll(newData.dataSets)
    }

    override fun replaceData(data: LineData) {
        handleReplace(data)
        _version.intValue++
    }

    override fun clear() {
        buffers.forEach { it.clear() }
        _version.intValue++
    }

    internal var _translationX = mutableFloatStateOf(0f)
    internal var _visibleXRange = mutableFloatStateOf(Float.MAX_VALUE)

    override fun scrollToEnd(animated: Boolean) {
        _translationX.floatValue = 0f
    }

    override fun scrollTo(x: Float, animated: Boolean) {
        val dataXMax = buffers.flatMap { it.toList() }.maxOfOrNull { it.x } ?: 0f
        _translationX.floatValue = -(dataXMax - x)
    }

    override fun setVisibleXRange(range: Float) {
        _visibleXRange.floatValue = range
    }
}
