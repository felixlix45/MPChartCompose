package com.github.mikephil.charting.compose.util

import org.junit.Assert.*
import org.junit.Test

class RingBufferTest {

    @Test
    fun `append adds elements`() {
        val buf = RingBuffer<Int>(capacity = 5)
        buf.append(1)
        buf.append(2)
        assertEquals(listOf(1, 2), buf.toList())
    }

    @Test
    fun `overflow evicts oldest elements`() {
        val buf = RingBuffer<Int>(capacity = 3)
        buf.append(1)
        buf.append(2)
        buf.append(3)
        buf.append(4)
        assertEquals(listOf(2, 3, 4), buf.toList())
        assertEquals(3, buf.size)
    }

    @Test
    fun `appendAll adds multiple elements`() {
        val buf = RingBuffer<Int>(capacity = 5)
        buf.appendAll(listOf(1, 2, 3))
        assertEquals(listOf(1, 2, 3), buf.toList())
    }

    @Test
    fun `appendAll with overflow`() {
        val buf = RingBuffer<Int>(capacity = 3)
        buf.appendAll(listOf(1, 2, 3, 4, 5))
        assertEquals(listOf(3, 4, 5), buf.toList())
    }

    @Test
    fun `clear empties buffer`() {
        val buf = RingBuffer<Int>(capacity = 5)
        buf.appendAll(listOf(1, 2, 3))
        buf.clear()
        assertEquals(0, buf.size)
        assertTrue(buf.toList().isEmpty())
    }

    @Test
    fun `size tracks element count`() {
        val buf = RingBuffer<Int>(capacity = 3)
        assertEquals(0, buf.size)
        buf.append(1)
        assertEquals(1, buf.size)
        buf.append(2)
        buf.append(3)
        assertEquals(3, buf.size)
        buf.append(4)
        assertEquals(3, buf.size)
    }

    @Test
    fun `unbounded capacity`() {
        val buf = RingBuffer<Int>(capacity = Int.MAX_VALUE)
        repeat(100) { buf.append(it) }
        assertEquals(100, buf.size)
    }

    @Test
    fun `replaceAll replaces all content`() {
        val buf = RingBuffer<Int>(capacity = 5)
        buf.appendAll(listOf(1, 2, 3))
        buf.replaceAll(listOf(10, 20))
        assertEquals(listOf(10, 20), buf.toList())
    }
}
