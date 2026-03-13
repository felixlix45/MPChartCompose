package com.github.mikephil.charting.compose.util

internal class RingBuffer<T>(val capacity: Int) {
    private val data = ArrayList<T>()
    private var head = 0

    val size: Int get() = data.size

    fun append(element: T) {
        if (capacity == Int.MAX_VALUE || data.size < capacity) {
            data.add(element)
        } else {
            data[head] = element
            head = (head + 1) % capacity
        }
    }

    fun appendAll(elements: List<T>) {
        for (element in elements) {
            append(element)
        }
    }

    fun replaceAll(elements: List<T>) {
        clear()
        appendAll(elements)
    }

    fun clear() {
        data.clear()
        head = 0
    }

    fun toList(): List<T> {
        if (data.size < capacity || capacity == Int.MAX_VALUE) return ArrayList(data)
        val result = ArrayList<T>(data.size)
        for (i in 0 until data.size) {
            result.add(data[(head + i) % data.size])
        }
        return result
    }
}
