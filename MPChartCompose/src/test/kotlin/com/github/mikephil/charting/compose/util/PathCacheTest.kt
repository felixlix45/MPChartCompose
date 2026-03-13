package com.github.mikephil.charting.compose.util

import org.junit.Assert.*
import org.junit.Test

class PathCacheTest {

    @Test
    fun `new cache has no cached path`() {
        val cache = PathCache()
        assertFalse(cache.isValid)
    }

    @Test
    fun `markValid sets cache as valid`() {
        val cache = PathCache()
        cache.markValid(entryCount = 10)
        assertTrue(cache.isValid)
    }

    @Test
    fun `invalidate clears the cache`() {
        val cache = PathCache()
        cache.markValid(entryCount = 10)
        cache.invalidate()
        assertFalse(cache.isValid)
    }

    @Test
    fun `isAppendOnly detects append-only updates`() {
        val cache = PathCache()
        cache.markValid(entryCount = 10)
        assertTrue(cache.isAppendOnly(newEntryCount = 12))
    }

    @Test
    fun `isAppendOnly returns false when entries shrink`() {
        val cache = PathCache()
        cache.markValid(entryCount = 10)
        assertFalse(cache.isAppendOnly(newEntryCount = 8))
    }

    @Test
    fun `isAppendOnly returns false when entry count unchanged`() {
        val cache = PathCache()
        cache.markValid(entryCount = 10)
        assertFalse(cache.isAppendOnly(newEntryCount = 10))
    }
}
