# MPChartCompose Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a new Jetpack Compose charting library module (`MPChartCompose/`) with Line, Bar, and Candle charts, optimized for real-time WebSocket streaming at 60fps.

**Architecture:** 5-layer cached draw pipeline (background, grid/axes, data, highlights, overlay) with zero-recomposition data updates. Composable-first API with lambda DSL. Frame-batched streaming via Channel + withFrameMillis. Standalone theme system.

**Tech Stack:** Kotlin 2.0.21+, Jetpack Compose (BOM 2026.03.00), Compose Canvas/DrawScope, Compose Animation (Animatable), Compose Gestures (pointerInput), JUnit 4. Requires JDK 17.

**Build note:** Compose BOM 2026.03.00 is the target version per spec. Verify availability at implementation time and use the latest stable BOM if not yet published.

**Spec:** `docs/superpowers/specs/2026-03-14-compose-chart-library-design.md`

---

## File Structure

```
MPChartCompose/
├── build.gradle.kts
├── src/main/AndroidManifest.xml
├── src/main/kotlin/com/github/mikephil/charting/compose/
│   │
│   ├── data/
│   │   ├── Entry.kt                    # Entry, BarEntry, CandleEntry data classes
│   │   ├── DataSet.kt                  # LineDataSet, BarDataSet, CandleDataSet
│   │   ├── ChartData.kt               # LineData, BarData, CandleData containers
│   │   ├── LineGlow.kt                 # LineGlow data class
│   │   └── Types.kt                    # LineMode, AxisPosition, LimitLine, LegendPosition, LegendOrientation
│   │
│   ├── style/
│   │   ├── ChartStyle.kt              # ChartStyle data class + defaults
│   │   └── ChartThemeProvider.kt       # CompositionLocal + ChartThemeProvider composable
│   │
│   ├── animation/
│   │   ├── ChartAnimation.kt          # ChartAnimation sealed interface + subtypes
│   │   └── ChartAnimator.kt           # Animatable-based phase animation, data transition interpolation
│   │
│   ├── scope/
│   │   ├── XAxisScope.kt              # XAxisScope DSL class
│   │   ├── YAxisScope.kt              # YAxisScope DSL class
│   │   ├── GestureScope.kt            # GestureScope DSL class
│   │   ├── HighlightScope.kt          # HighlightScope DSL class
│   │   ├── AnimationScope.kt          # AnimationScope DSL class
│   │   ├── ChartStyleScope.kt         # ChartStyleScope DSL class
│   │   ├── ViewportScope.kt           # ViewportScope DSL class
│   │   └── LegendScope.kt             # LegendScope DSL class
│   │
│   ├── state/
│   │   ├── ChartState.kt              # LineChartState, BarChartState, CandleChartState interfaces
│   │   ├── LineChartStateImpl.kt       # Implementation with Channel + ring buffer + frame batching
│   │   ├── BarChartStateImpl.kt        # BarChartState implementation
│   │   ├── CandleChartStateImpl.kt     # CandleChartState implementation
│   │   └── RememberChartState.kt       # rememberLineChartState, rememberBarChartState, rememberCandleChartState
│   │
│   ├── util/
│   │   ├── Transformer.kt             # Data ↔ Viewport ↔ Pixel coordinate conversion
│   │   ├── ViewportHandler.kt         # Visible range, translation, auto-scale calculations
│   │   ├── RingBuffer.kt              # Bounded ring buffer for streaming data
│   │   ├── PathCache.kt               # Path caching with append-only detection
│   │   └── EntryFinder.kt             # Nearest-entry search for touch/highlight
│   │
│   ├── renderer/
│   │   ├── BackgroundRenderer.kt       # Layer 1: background fill
│   │   ├── GridRenderer.kt            # Layer 2: grid lines + axis lines
│   │   ├── AxisLabelRenderer.kt       # Layer 2: axis label text (uses TextMeasurer)
│   │   ├── LimitLineRenderer.kt       # Layer 2: horizontal/vertical limit lines
│   │   ├── LineDataRenderer.kt        # Layer 3: line paths (all modes), fill, glow, circles
│   │   ├── BarDataRenderer.kt         # Layer 3: bar rectangles, stacked bars
│   │   ├── CandleDataRenderer.kt      # Layer 3: candle bodies + shadow wicks
│   │   ├── HighlightRenderer.kt       # Layer 4: crosshair lines + scrub indicator circle
│   │   └── LegendRenderer.kt          # Layer 5: legend forms + labels
│   │
│   ├── gesture/
│   │   ├── ChartGestureHandler.kt     # Unified pointerInput handler (tap, pan, scrub, fling)
│   │   └── FlingDecelerator.kt        # Exponential decay fling animation
│   │
│   ├── highlight/
│   │   └── HighlightState.kt          # MutableState<Highlight?> + Highlight data class
│   │
│   ├── marker/
│   │   ├── MarkerPositioner.kt        # Composable that positions marker with edge avoidance
│   │   └── DynamicAxisLabel.kt         # Dynamic Y/X axis label pill composables
│   │
│   └── chart/
│       ├── ChartCore.kt               # Shared Box + Canvas + overlay structure
│       ├── LineChart.kt                # LineChart composable function
│       ├── BarChart.kt                 # BarChart composable function
│       └── CandleChart.kt             # CandleChart composable function
│
└── src/test/kotlin/com/github/mikephil/charting/compose/
    ├── data/
    │   ├── EntryTest.kt                # Entry equality, BarEntry stack sum, CandleEntry
    │   └── DataSetTest.kt              # DataSet operations, empty/single entry edge cases
    ├── util/
    │   ├── TransformerTest.kt          # Bidirectional coordinate conversion
    │   ├── ViewportHandlerTest.kt      # Visible range, auto-scale, translation
    │   ├── RingBufferTest.kt           # Append, overflow, bounded size
    │   ├── PathCacheTest.kt            # Append detection, invalidation
    │   └── EntryFinderTest.kt          # Nearest entry search
    ├── state/
    │   └── ChartStateTest.kt           # Append, replaceData, clear, ring buffer integration
    ├── animation/
    │   └── ChartAnimationTest.kt       # Animation type construction, duration values
    └── style/
        └── ChartStyleTest.kt           # Style merging, defaults, scope override

Modified existing files:
    build.gradle                        # Add Kotlin plugin classpath, upgrade AGP
    settings.gradle                     # Add 'MPChartCompose' include
    gradle/wrapper/gradle-wrapper.properties  # Upgrade to Gradle 8.2+
    MPChartLib/build.gradle             # Compatibility updates for AGP 8 (namespace, SDK syntax)
    MPChartExample/build.gradle         # Compatibility updates for AGP 8 (namespace, SDK syntax)
```

---

## Chunk 1: Project Setup & Build Configuration

### Task 1: Upgrade Gradle Wrapper

**Files:**
- Modify: `gradle/wrapper/gradle-wrapper.properties`

- [ ] **Step 1: Upgrade Gradle wrapper to 8.2**

```properties
# gradle/wrapper/gradle-wrapper.properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.2-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

- [ ] **Step 2: Verify wrapper downloads**

Run: `./gradlew --version`
Expected: `Gradle 8.2`

---

### Task 2: Upgrade AGP and Add Kotlin Plugin

**Files:**
- Modify: `build.gradle` (root)
- Modify: `MPChartLib/build.gradle`
- Modify: `MPChartExample/build.gradle`

- [ ] **Step 1: Update root build.gradle**

```groovy
// build.gradle (root)
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:8.2.0'
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
```

- [ ] **Step 1b: Add pluginManagement to settings.gradle**

This is required so the new Kotlin DSL module can resolve Kotlin and Compose plugins:

```groovy
// settings.gradle (add at the TOP, before any include)
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id 'org.jetbrains.kotlin.android' version '2.0.21'
        id 'org.jetbrains.kotlin.plugin.compose' version '2.0.21'
    }
}
```

Note: Using Kotlin 2.0.21+ because `org.jetbrains.kotlin.plugin.compose` requires Kotlin 2.0+. The `kotlin-gradle-plugin` classpath is no longer needed in root `build.gradle` — it's resolved via `pluginManagement`.

- [ ] **Step 2: Update MPChartLib/build.gradle for AGP 8 compatibility**

AGP 8 requires `namespace` in the android block and JDK 17. Update SDK syntax to non-deprecated form:

```groovy
apply plugin: 'com.android.library'

group='com.github.philjay'

android {
    namespace 'com.github.mikephil.charting'
    compileSdk 28
    buildToolsVersion '28.0.3'
    defaultConfig {
        minSdk 14
        targetSdk 28
        versionCode 3
        versionName '3.1.0'
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }
    }
    testOptions {
        unitTests.returnDefaultValues = true
    }
}

dependencies {
    implementation 'androidx.annotation:annotation:1.0.0'
    testImplementation 'junit:junit:4.12'
}

task sourcesJar(type: Jar) {
    from android.sourceSets.main.java.srcDirs
    archiveClassifier = 'sources'
}

task javadoc(type: Javadoc) {
    options.charSet = 'UTF-8'
    failOnError  false
    source = android.sourceSets.main.java.sourceFiles
    classpath += project.files(android.getBootClasspath().join(File.pathSeparator))
}

task javadocJar(type: Jar, dependsOn: javadoc) {
    archiveClassifier = 'javadoc'
    from javadoc.destinationDir
}

artifacts {
    archives sourcesJar
    archives javadocJar
}
```

Key changes: add `namespace`, `compileSdkVersion` → `compileSdk`, `minSdkVersion` → `minSdk`, `targetSdkVersion` → `targetSdk`, `classifier` → `archiveClassifier`.

- [ ] **Step 3: Update MPChartExample/build.gradle for AGP 8 compatibility**

Read `MPChartExample/build.gradle` first to understand its current content, then apply the same pattern: add `namespace`, update SDK property names to non-deprecated syntax, update `classifier` → `archiveClassifier` if applicable.

The namespace for the example app should be taken from its existing `AndroidManifest.xml` package attribute.

- [ ] **Step 4: Remove package from AndroidManifest.xml files**

AGP 8 requires `namespace` in build.gradle and warns/errors if `package` is also in AndroidManifest.xml. Remove the `package` attribute from:
- `MPChartLib/src/main/AndroidManifest.xml`
- `MPChartExample/src/main/AndroidManifest.xml`

Keep the manifest files but remove only the `package="..."` attribute from the `<manifest>` tag.

- [ ] **Step 5: Verify existing modules still build**

Run: `./gradlew :MPChartLib:assembleRelease :MPChartExample:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Run existing tests**

Run: `./gradlew :MPChartLib:test`
Expected: All existing tests pass.

- [ ] **Step 7: Commit**

```bash
git add build.gradle gradle/wrapper/gradle-wrapper.properties MPChartLib/build.gradle MPChartExample/build.gradle MPChartLib/src/main/AndroidManifest.xml MPChartExample/src/main/AndroidManifest.xml
git commit -m "chore: upgrade Gradle 8.2, AGP 8.2.0, add Kotlin plugin"
```

---

### Task 3: Create MPChartCompose Module Skeleton

**Files:**
- Create: `MPChartCompose/build.gradle.kts`
- Create: `MPChartCompose/src/main/AndroidManifest.xml`
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/MPChartCompose.kt` (placeholder)
- Modify: `settings.gradle`

- [ ] **Step 1: Add module to settings.gradle**

```groovy
include 'MPChartLib'
include 'MPChartExample'
include 'MPChartCompose'
```

- [ ] **Step 2: Create MPChartCompose/build.gradle.kts**

```kotlin
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.github.mikephil.charting.compose"
    compileSdk = 34

    defaultConfig {
        minSdk = 23
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.03.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-text")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.03.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
```

- [ ] **Step 3: Create AndroidManifest.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest />
```

- [ ] **Step 4: Create placeholder Kotlin file**

Create `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/MPChartCompose.kt`:

```kotlin
package com.github.mikephil.charting.compose

/**
 * MPChartCompose — Jetpack Compose charting library.
 * Module placeholder; real code follows in subsequent tasks.
 */
internal object MPChartCompose {
    const val VERSION = "1.0.0-alpha01"
}
```

- [ ] **Step 5: Verify module compiles**

Run: `./gradlew :MPChartCompose:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add settings.gradle MPChartCompose/
git commit -m "feat: add MPChartCompose module skeleton with Compose BOM 2026.03.00"
```

---

## Chunk 2: Data Model & Core Types

### Task 4: Entry Data Classes

**Files:**
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/data/Entry.kt`
- Create: `MPChartCompose/src/test/kotlin/com/github/mikephil/charting/compose/data/EntryTest.kt`

- [ ] **Step 1: Write failing tests for Entry types**

```kotlin
// EntryTest.kt
package com.github.mikephil.charting.compose.data

import org.junit.Assert.*
import org.junit.Test

class EntryTest {

    @Test
    fun `Entry equality by value`() {
        val a = Entry(1f, 2f)
        val b = Entry(1f, 2f)
        assertEquals(a, b)
    }

    @Test
    fun `Entry with data field`() {
        val entry = Entry(1f, 2f, data = "tag")
        assertEquals("tag", entry.data)
    }

    @Test
    fun `BarEntry basic construction`() {
        val entry = BarEntry(x = 1f, y = 10f)
        assertEquals(1f, entry.x, 0.001f)
        assertEquals(10f, entry.y, 0.001f)
        assertNull(entry.stackValues)
    }

    @Test
    fun `BarEntry with stackValues derives y as sum`() {
        val entry = BarEntry(x = 1f, y = 0f, stackValues = listOf(3f, 4f, 5f))
        assertEquals(12f, entry.effectiveY, 0.001f)
    }

    @Test
    fun `BarEntry stackValues equality uses structural comparison`() {
        val a = BarEntry(x = 1f, y = 0f, stackValues = listOf(1f, 2f))
        val b = BarEntry(x = 1f, y = 0f, stackValues = listOf(1f, 2f))
        assertEquals(a, b)
    }

    @Test
    fun `BarEntry with data field`() {
        val entry = BarEntry(x = 1f, y = 5f, data = 42)
        assertEquals(42, entry.data)
    }

    @Test
    fun `CandleEntry construction`() {
        val entry = CandleEntry(x = 1f, open = 10f, high = 15f, low = 8f, close = 12f)
        assertEquals(10f, entry.open, 0.001f)
        assertEquals(15f, entry.high, 0.001f)
        assertEquals(8f, entry.low, 0.001f)
        assertEquals(12f, entry.close, 0.001f)
    }

    @Test
    fun `CandleEntry equality`() {
        val a = CandleEntry(1f, 10f, 15f, 8f, 12f)
        val b = CandleEntry(1f, 10f, 15f, 8f, 12f)
        assertEquals(a, b)
    }

    @Test
    fun `CandleEntry with data field`() {
        val entry = CandleEntry(1f, 10f, 15f, 8f, 12f, data = "candle")
        assertEquals("candle", entry.data)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :MPChartCompose:testDebugUnitTest --tests "com.github.mikephil.charting.compose.data.EntryTest"`
Expected: FAIL — classes not found.

- [ ] **Step 3: Implement Entry types**

```kotlin
// Entry.kt
package com.github.mikephil.charting.compose.data

data class Entry(
    val x: Float,
    val y: Float,
    val data: Any? = null,
)

data class BarEntry(
    val x: Float,
    val y: Float,
    val stackValues: List<Float>? = null,
    val data: Any? = null,
) {
    /** When stackValues is non-null, effectiveY is the sum. Otherwise, y. */
    val effectiveY: Float
        get() = stackValues?.sum() ?: y
}

data class CandleEntry(
    val x: Float,
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float,
    val data: Any? = null,
)
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :MPChartCompose:testDebugUnitTest --tests "com.github.mikephil.charting.compose.data.EntryTest"`
Expected: All 8 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/data/Entry.kt MPChartCompose/src/test/kotlin/com/github/mikephil/charting/compose/data/EntryTest.kt
git commit -m "feat: add Entry, BarEntry, CandleEntry data classes with tests"
```

---

### Task 5: DataSets, ChartData, and Supporting Types

**Files:**
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/data/DataSet.kt`
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/data/ChartData.kt`
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/data/LineGlow.kt`
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/data/Types.kt`
- Create: `MPChartCompose/src/test/kotlin/com/github/mikephil/charting/compose/data/DataSetTest.kt`

- [ ] **Step 1: Write failing tests for DataSet and ChartData**

```kotlin
// DataSetTest.kt
package com.github.mikephil.charting.compose.data

import org.junit.Assert.*
import org.junit.Test

class DataSetTest {

    @Test
    fun `LineDataSet default values`() {
        val ds = LineDataSet(entries = emptyList(), label = "test")
        assertEquals("test", ds.label)
        assertTrue(ds.drawCircles)
        assertEquals(LineMode.Linear, ds.mode)
        assertFalse(ds.fillEnabled)
        assertNull(ds.glow)
    }

    @Test
    fun `BarDataSet default values`() {
        val ds = BarDataSet(entries = emptyList(), label = "bars")
        assertEquals("bars", ds.label)
        assertNull(ds.barBrush)
    }

    @Test
    fun `CandleDataSet default colors`() {
        val ds = CandleDataSet(entries = emptyList(), label = "candles")
        assertEquals(0.7f, ds.bodyWidth, 0.001f)
    }

    @Test
    fun `LineData holds multiple datasets`() {
        val ds1 = LineDataSet(entries = listOf(Entry(0f, 1f)), label = "A")
        val ds2 = LineDataSet(entries = listOf(Entry(0f, 2f)), label = "B")
        val data = LineData(dataSets = listOf(ds1, ds2))
        assertEquals(2, data.dataSets.size)
    }

    @Test
    fun `BarData barWidth default`() {
        val data = BarData(dataSets = emptyList())
        assertEquals(0.85f, data.barWidth, 0.001f)
    }

    @Test
    fun `CandleData holds datasets`() {
        val data = CandleData(dataSets = emptyList())
        assertTrue(data.dataSets.isEmpty())
    }

    @Test
    fun `LineGlow default values`() {
        val glow = LineGlow()
        assertTrue(glow.enabled)
        assertEquals(0.4f, glow.alpha, 0.001f)
    }

    @Test
    fun `LimitLine default values`() {
        val line = LimitLine(value = 100f)
        assertEquals(100f, line.value, 0.001f)
        assertEquals("", line.label)
        assertNull(line.dashEffect)
    }

    @Test
    fun `LineMode enum values`() {
        val modes = LineMode.values()
        assertEquals(4, modes.size)
        assertTrue(modes.contains(LineMode.Linear))
        assertTrue(modes.contains(LineMode.CubicBezier))
        assertTrue(modes.contains(LineMode.HorizontalBezier))
        assertTrue(modes.contains(LineMode.Stepped))
    }

    @Test
    fun `AxisPosition enum values`() {
        assertEquals(2, AxisPosition.values().size)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :MPChartCompose:testDebugUnitTest --tests "com.github.mikephil.charting.compose.data.DataSetTest"`
Expected: FAIL — classes not found.

- [ ] **Step 3: Implement Types.kt**

```kotlin
// Types.kt
package com.github.mikephil.charting.compose.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class LineMode {
    Linear,
    CubicBezier,
    HorizontalBezier,
    Stepped,
}

enum class AxisPosition { Top, Bottom }

enum class LegendPosition { Top, Bottom, Left, Right }

enum class LegendOrientation { Horizontal, Vertical }

data class LimitLine(
    val value: Float,
    val label: String = "",
    val color: Color = Color(0xFFFFAA00),
    val lineWidth: Dp = 1.dp,
    val dashEffect: PathEffect? = null,
)
```

- [ ] **Step 4: Implement LineGlow.kt**

```kotlin
// LineGlow.kt
package com.github.mikephil.charting.compose.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class LineGlow(
    val enabled: Boolean = true,
    val radius: Dp = 8.dp,
    val alpha: Float = 0.4f,
    val color: Color = Color.Unspecified,
)
```

- [ ] **Step 5: Implement DataSet.kt**

```kotlin
// DataSet.kt
package com.github.mikephil.charting.compose.data

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class LineDataSet(
    val entries: List<Entry>,
    val label: String,
    val lineColor: Color = Color.Unspecified,
    val lineWidth: Dp = 2.dp,
    val drawCircles: Boolean = true,
    val circleRadius: Dp = 4.dp,
    val mode: LineMode = LineMode.Linear,
    val fillEnabled: Boolean = false,
    val fillBrush: Brush? = null,
    val fillColor: Color = Color.Unspecified,
    val glow: LineGlow? = null,
)

data class BarDataSet(
    val entries: List<BarEntry>,
    val label: String,
    val barColor: Color = Color.Unspecified,
    val barBrush: Brush? = null,
)

data class CandleDataSet(
    val entries: List<CandleEntry>,
    val label: String,
    val increasingColor: Color = Color(0xFF00C853),
    val decreasingColor: Color = Color(0xFFFF1744),
    val shadowColor: Color = Color(0xFF888888),
    val bodyWidth: Float = 0.7f,
)
```

- [ ] **Step 6: Implement ChartData.kt**

```kotlin
// ChartData.kt
package com.github.mikephil.charting.compose.data

data class LineData(val dataSets: List<LineDataSet>)

data class BarData(
    val dataSets: List<BarDataSet>,
    val barWidth: Float = 0.85f,
)

data class CandleData(val dataSets: List<CandleDataSet>)
```

- [ ] **Step 7: Run tests to verify they pass**

Run: `./gradlew :MPChartCompose:testDebugUnitTest --tests "com.github.mikephil.charting.compose.data.DataSetTest"`
Expected: All 10 tests PASS.

- [ ] **Step 8: Commit**

```bash
git add MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/data/ MPChartCompose/src/test/kotlin/com/github/mikephil/charting/compose/data/DataSetTest.kt
git commit -m "feat: add DataSet, ChartData, LineGlow, and supporting types"
```

---

### Task 6: DSL Scope Classes

**Files:**
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/scope/XAxisScope.kt`
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/scope/YAxisScope.kt`
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/scope/GestureScope.kt`
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/scope/HighlightScope.kt`
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/scope/AnimationScope.kt`
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/scope/ChartStyleScope.kt`
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/scope/ViewportScope.kt`
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/scope/LegendScope.kt`

- [ ] **Step 1: Implement all scope classes**

Each scope class is a simple mutable property container used as a lambda receiver in the chart DSL. Implement all 8 files matching the spec definitions exactly. Reference `docs/superpowers/specs/2026-03-14-compose-chart-library-design.md` sections "DSL Scope Classes" and "DSL Scope Definitions (All Scopes)" for the complete property lists, types, and defaults.

Each file follows this pattern:

```kotlin
// Example: XAxisScope.kt
package com.github.mikephil.charting.compose.scope

import com.github.mikephil.charting.compose.data.AxisPosition

class XAxisScope {
    var position: AxisPosition = AxisPosition.Bottom
    var formatter: ((Float) -> String)? = null
    var gridEnabled: Boolean = true
    var drawLabels: Boolean = true
    var labelCount: Int = 5
    var axisLineEnabled: Boolean = true
}
```

Implement all 8 scope files following the same pattern with their respective properties from the spec.

- [ ] **Step 2: Verify module compiles**

Run: `./gradlew :MPChartCompose:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/scope/
git commit -m "feat: add all DSL scope classes (axis, gesture, highlight, animation, style, viewport, legend)"
```

---

## Chunk 3: Core Utilities

### Task 7: Transformer (Coordinate Conversion)

**Files:**
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/util/Transformer.kt`
- Create: `MPChartCompose/src/test/kotlin/com/github/mikephil/charting/compose/util/TransformerTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
// TransformerTest.kt
package com.github.mikephil.charting.compose.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.*
import org.junit.Test

class TransformerTest {

    @Test
    fun `dataToPixel converts data point to pixel coordinates`() {
        val transformer = Transformer(
            contentRect = Rect(0f, 0f, 100f, 100f),
            dataXRange = 0f..10f,
            dataYRange = 0f..100f,
        )
        val pixel = transformer.dataToPixel(5f, 50f)
        assertEquals(50f, pixel.x, 0.5f)
        assertEquals(50f, pixel.y, 0.5f)
    }

    @Test
    fun `pixelToData converts pixel coordinates back to data space`() {
        val transformer = Transformer(
            contentRect = Rect(0f, 0f, 100f, 100f),
            dataXRange = 0f..10f,
            dataYRange = 0f..100f,
        )
        val data = transformer.pixelToData(Offset(50f, 50f))
        assertEquals(5f, data.first, 0.5f)
        assertEquals(50f, data.second, 0.5f)
    }

    @Test
    fun `dataToPixel handles Y-axis inversion (top=max, bottom=min)`() {
        val transformer = Transformer(
            contentRect = Rect(0f, 0f, 100f, 100f),
            dataXRange = 0f..10f,
            dataYRange = 0f..100f,
        )
        // Y=100 (max) should map to top (y=0)
        val top = transformer.dataToPixel(0f, 100f)
        assertEquals(0f, top.y, 0.5f)
        // Y=0 (min) should map to bottom (y=100)
        val bottom = transformer.dataToPixel(0f, 0f)
        assertEquals(100f, bottom.y, 0.5f)
    }

    @Test
    fun `roundtrip conversion is identity`() {
        val transformer = Transformer(
            contentRect = Rect(20f, 10f, 200f, 150f),
            dataXRange = -5f..15f,
            dataYRange = 10f..90f,
        )
        val pixel = transformer.dataToPixel(3f, 45f)
        val (dx, dy) = transformer.pixelToData(pixel)
        assertEquals(3f, dx, 0.01f)
        assertEquals(45f, dy, 0.01f)
    }

    @Test
    fun `contentRect with offsets works correctly`() {
        val transformer = Transformer(
            contentRect = Rect(50f, 20f, 250f, 220f), // 200x200 drawing area at offset
            dataXRange = 0f..100f,
            dataYRange = 0f..100f,
        )
        // data (0,0) → pixel (50, 220) — bottom-left of content rect
        val bottomLeft = transformer.dataToPixel(0f, 0f)
        assertEquals(50f, bottomLeft.x, 0.5f)
        assertEquals(220f, bottomLeft.y, 0.5f)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :MPChartCompose:testDebugUnitTest --tests "com.github.mikephil.charting.compose.util.TransformerTest"`
Expected: FAIL

- [ ] **Step 3: Implement Transformer**

```kotlin
// Transformer.kt
package com.github.mikephil.charting.compose.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

class Transformer(
    val contentRect: Rect,
    val dataXRange: ClosedFloatingPointRange<Float>,
    val dataYRange: ClosedFloatingPointRange<Float>,
) {
    private val xSpan get() = dataXRange.endInclusive - dataXRange.start
    private val ySpan get() = dataYRange.endInclusive - dataYRange.start
    private val width get() = contentRect.width
    private val height get() = contentRect.height

    fun dataToPixel(dataX: Float, dataY: Float): Offset {
        val px = contentRect.left + ((dataX - dataXRange.start) / xSpan) * width
        // Y is inverted: high data values → top of screen (low pixel Y)
        val py = contentRect.bottom - ((dataY - dataYRange.start) / ySpan) * height
        return Offset(px, py)
    }

    fun pixelToData(pixel: Offset): Pair<Float, Float> {
        val dataX = dataXRange.start + ((pixel.x - contentRect.left) / width) * xSpan
        val dataY = dataYRange.start + ((contentRect.bottom - pixel.y) / height) * ySpan
        return dataX to dataY
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :MPChartCompose:testDebugUnitTest --tests "com.github.mikephil.charting.compose.util.TransformerTest"`
Expected: All 5 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/util/Transformer.kt MPChartCompose/src/test/kotlin/com/github/mikephil/charting/compose/util/TransformerTest.kt
git commit -m "feat: add Transformer for data/pixel coordinate conversion"
```

---

### Task 8: RingBuffer

**Files:**
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/util/RingBuffer.kt`
- Create: `MPChartCompose/src/test/kotlin/com/github/mikephil/charting/compose/util/RingBufferTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
// RingBufferTest.kt
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
        buf.append(4) // overflow
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
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :MPChartCompose:testDebugUnitTest --tests "com.github.mikephil.charting.compose.util.RingBufferTest"`
Expected: FAIL

- [ ] **Step 3: Implement RingBuffer**

```kotlin
// RingBuffer.kt
package com.github.mikephil.charting.compose.util

/**
 * A bounded ring buffer that evicts oldest elements on overflow.
 * Used internally for streaming chart data.
 */
internal class RingBuffer<T>(val capacity: Int) {
    private val data = ArrayList<T>()
    private var head = 0 // index of oldest element when at capacity

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
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :MPChartCompose:testDebugUnitTest --tests "com.github.mikephil.charting.compose.util.RingBufferTest"`
Expected: All 8 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/util/RingBuffer.kt MPChartCompose/src/test/kotlin/com/github/mikephil/charting/compose/util/RingBufferTest.kt
git commit -m "feat: add RingBuffer for bounded streaming data"
```

---

### Task 9: ViewportHandler

**Files:**
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/util/ViewportHandler.kt`
- Create: `MPChartCompose/src/test/kotlin/com/github/mikephil/charting/compose/util/ViewportHandlerTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
// ViewportHandlerTest.kt
package com.github.mikephil.charting.compose.util

import com.github.mikephil.charting.compose.data.Entry
import org.junit.Assert.*
import org.junit.Test

class ViewportHandlerTest {

    @Test
    fun `computeVisibleRange shows all data when visibleXRange is null`() {
        val entries = listOf(Entry(0f, 1f), Entry(10f, 2f), Entry(20f, 3f))
        val vp = ViewportHandler(visibleXRange = null, translationX = 0f)
        val range = vp.computeVisibleXRange(dataXMin = 0f, dataXMax = 20f)
        assertEquals(0f, range.start, 0.01f)
        assertEquals(20f, range.endInclusive, 0.01f)
    }

    @Test
    fun `computeVisibleRange limits to visibleXRange from the end`() {
        val vp = ViewportHandler(visibleXRange = 10f, translationX = 0f)
        val range = vp.computeVisibleXRange(dataXMin = 0f, dataXMax = 20f)
        // Default: shows the last visibleXRange worth of data
        assertEquals(10f, range.start, 0.01f)
        assertEquals(20f, range.endInclusive, 0.01f)
    }

    @Test
    fun `translationX pans the visible range`() {
        val vp = ViewportHandler(visibleXRange = 10f, translationX = -5f)
        val range = vp.computeVisibleXRange(dataXMin = 0f, dataXMax = 20f)
        assertEquals(5f, range.start, 0.01f)
        assertEquals(15f, range.endInclusive, 0.01f)
    }

    @Test
    fun `computeVisibleYRange with auto-scale`() {
        val entries = listOf(Entry(5f, 10f), Entry(6f, 30f), Entry(7f, 20f))
        val vp = ViewportHandler(visibleXRange = null, translationX = 0f)
        val yRange = vp.computeVisibleYRange(
            entries = entries,
            visibleXRange = 0f..20f,
            yAutoScale = true,
            yMin = null,
            yMax = null,
        )
        assertEquals(10f, yRange.start, 0.01f)
        assertEquals(30f, yRange.endInclusive, 0.01f)
    }

    @Test
    fun `computeVisibleYRange with fixed min and max`() {
        val entries = listOf(Entry(5f, 10f), Entry(6f, 30f))
        val vp = ViewportHandler(visibleXRange = null, translationX = 0f)
        val yRange = vp.computeVisibleYRange(
            entries = entries,
            visibleXRange = 0f..20f,
            yAutoScale = true,
            yMin = 0f,
            yMax = 50f,
        )
        assertEquals(0f, yRange.start, 0.01f)
        assertEquals(50f, yRange.endInclusive, 0.01f)
    }

    @Test
    fun `isAtEnd returns true when viewport shows latest data`() {
        val vp = ViewportHandler(visibleXRange = 10f, translationX = 0f)
        assertTrue(vp.isAtEnd(dataXMax = 20f))
    }

    @Test
    fun `isAtEnd returns false when panned away`() {
        val vp = ViewportHandler(visibleXRange = 10f, translationX = -5f)
        assertFalse(vp.isAtEnd(dataXMax = 20f))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :MPChartCompose:testDebugUnitTest --tests "com.github.mikephil.charting.compose.util.ViewportHandlerTest"`
Expected: FAIL

- [ ] **Step 3: Implement ViewportHandler**

```kotlin
// ViewportHandler.kt
package com.github.mikephil.charting.compose.util

import com.github.mikephil.charting.compose.data.Entry

class ViewportHandler(
    val visibleXRange: Float?,
    val translationX: Float,
) {
    fun computeVisibleXRange(dataXMin: Float, dataXMax: Float): ClosedFloatingPointRange<Float> {
        val range = visibleXRange ?: return dataXMin..dataXMax
        val end = dataXMax + translationX
        val start = end - range
        return start..end
    }

    fun computeVisibleYRange(
        entries: List<Entry>,
        visibleXRange: ClosedFloatingPointRange<Float>,
        yAutoScale: Boolean,
        yMin: Float?,
        yMax: Float?,
    ): ClosedFloatingPointRange<Float> {
        if (!yAutoScale && yMin != null && yMax != null) return yMin..yMax

        val visibleEntries = entries.filter { it.x in visibleXRange }
        val computedMin = visibleEntries.minOfOrNull { it.y } ?: 0f
        val computedMax = visibleEntries.maxOfOrNull { it.y } ?: 1f

        val finalMin = yMin ?: computedMin
        val finalMax = yMax ?: computedMax
        return finalMin..finalMax
    }

    fun isAtEnd(dataXMax: Float): Boolean {
        return translationX >= -0.01f // essentially at 0 (default = showing end)
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :MPChartCompose:testDebugUnitTest --tests "com.github.mikephil.charting.compose.util.ViewportHandlerTest"`
Expected: All 7 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/util/ViewportHandler.kt MPChartCompose/src/test/kotlin/com/github/mikephil/charting/compose/util/ViewportHandlerTest.kt
git commit -m "feat: add ViewportHandler for visible range and auto-scale"
```

---

### Task 10: EntryFinder (Nearest Entry Search)

**Files:**
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/util/EntryFinder.kt`
- Create: `MPChartCompose/src/test/kotlin/com/github/mikephil/charting/compose/util/EntryFinderTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
// EntryFinderTest.kt
package com.github.mikephil.charting.compose.util

import com.github.mikephil.charting.compose.data.Entry
import org.junit.Assert.*
import org.junit.Test

class EntryFinderTest {

    @Test
    fun `findNearest returns closest entry by x`() {
        val entries = listOf(Entry(0f, 1f), Entry(5f, 2f), Entry(10f, 3f))
        val result = EntryFinder.findNearest(entries, targetX = 4f)
        assertEquals(Entry(5f, 2f), result)
    }

    @Test
    fun `findNearest returns first when equidistant`() {
        val entries = listOf(Entry(0f, 1f), Entry(10f, 2f))
        val result = EntryFinder.findNearest(entries, targetX = 5f)
        assertEquals(Entry(0f, 1f), result)
    }

    @Test
    fun `findNearest returns null for empty list`() {
        val result = EntryFinder.findNearest(emptyList(), targetX = 5f)
        assertNull(result)
    }

    @Test
    fun `findNearest with single entry`() {
        val entries = listOf(Entry(5f, 10f))
        val result = EntryFinder.findNearest(entries, targetX = 100f)
        assertEquals(Entry(5f, 10f), result)
    }

    @Test
    fun `findNearestIndex returns index`() {
        val entries = listOf(Entry(0f, 1f), Entry(5f, 2f), Entry(10f, 3f))
        val idx = EntryFinder.findNearestIndex(entries, targetX = 8f)
        assertEquals(2, idx)
    }

    @Test
    fun `findNearestIndex returns -1 for empty list`() {
        val idx = EntryFinder.findNearestIndex(emptyList(), targetX = 5f)
        assertEquals(-1, idx)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :MPChartCompose:testDebugUnitTest --tests "com.github.mikephil.charting.compose.util.EntryFinderTest"`
Expected: FAIL

- [ ] **Step 3: Implement EntryFinder**

```kotlin
// EntryFinder.kt
package com.github.mikephil.charting.compose.util

import com.github.mikephil.charting.compose.data.Entry
import kotlin.math.abs

object EntryFinder {

    fun findNearest(entries: List<Entry>, targetX: Float): Entry? {
        val idx = findNearestIndex(entries, targetX)
        return if (idx >= 0) entries[idx] else null
    }

    fun findNearestIndex(entries: List<Entry>, targetX: Float): Int {
        return findNearestIndexByX(entries.map { it.x }, targetX)
    }

    /** Generic version that works with any list of x-coordinates. */
    fun findNearestIndexByX(xValues: List<Float>, targetX: Float): Int {
        if (xValues.isEmpty()) return -1
        if (xValues.size == 1) return 0

        // Binary search for efficiency with sorted values
        var low = 0
        var high = xValues.size - 1
        while (low < high) {
            val mid = (low + high) / 2
            if (xValues[mid] < targetX) low = mid + 1
            else high = mid
        }
        return when {
            low == 0 -> 0
            low >= xValues.size -> xValues.size - 1
            else -> {
                val distLow = abs(xValues[low] - targetX)
                val distPrev = abs(xValues[low - 1] - targetX)
                if (distPrev <= distLow) low - 1 else low
            }
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :MPChartCompose:testDebugUnitTest --tests "com.github.mikephil.charting.compose.util.EntryFinderTest"`
Expected: All 6 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/util/EntryFinder.kt MPChartCompose/src/test/kotlin/com/github/mikephil/charting/compose/util/EntryFinderTest.kt
git commit -m "feat: add EntryFinder with binary search for nearest entry"
```

---

## Chunk 4: Theme, Animation & Streaming State

### Task 11: ChartStyle and ChartThemeProvider

**Files:**
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/style/ChartStyle.kt`
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/style/ChartThemeProvider.kt`
- Create: `MPChartCompose/src/test/kotlin/com/github/mikephil/charting/compose/style/ChartStyleTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
// ChartStyleTest.kt
package com.github.mikephil.charting.compose.style

import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Test

class ChartStyleTest {

    @Test
    fun `default style has dark theme colors`() {
        val style = ChartStyle()
        assertEquals(Color(0xFF1A1A1A), style.backgroundColor)
        assertEquals(Color(0xFF2A2A2A), style.gridColor)
    }

    @Test
    fun `style mergeWith overrides specified scope values`() {
        val base = ChartStyle()
        val scope = ChartStyleScope().apply { backgroundColor = Color.Red }
        val merged = base.mergeWith(scope)
        assertEquals(Color.Red, merged.backgroundColor)
        assertEquals(base.gridColor, merged.gridColor) // unchanged — scope left it Unspecified
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :MPChartCompose:testDebugUnitTest --tests "com.github.mikephil.charting.compose.style.ChartStyleTest"`
Expected: FAIL

- [ ] **Step 3: Implement ChartStyle**

```kotlin
// ChartStyle.kt
package com.github.mikephil.charting.compose.style

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ChartStyle(
    val backgroundColor: Color = Color(0xFF1A1A1A),
    val gridColor: Color = Color(0xFF2A2A2A),
    val gridLineWidth: Dp = 0.5.dp,
    val axisLineColor: Color = Color(0xFF444444),
    val axisLabelColor: Color = Color(0xFFAAAAAA),
    val axisLabelSize: TextUnit = 12.sp,
    val limitLineColor: Color = Color(0xFFFFAA00),
    val highlightLineColor: Color = Color(0xFFFFFFFF),
    val highlightLineWidth: Dp = 1.dp,
    val legendTextColor: Color = Color(0xFFCCCCCC),
    val legendTextSize: TextUnit = 11.sp,
) {
    /**
     * Merges this style with overrides from a ChartStyleScope.
     * Only non-Unspecified values from the scope override the base style.
     */
    fun mergeWith(scope: ChartStyleScope): ChartStyle {
        return copy(
            backgroundColor = if (scope.backgroundColor != Color.Unspecified) scope.backgroundColor else backgroundColor,
            gridColor = if (scope.gridColor != Color.Unspecified) scope.gridColor else gridColor,
            gridLineWidth = if (scope.gridLineWidth != Dp.Unspecified) scope.gridLineWidth else gridLineWidth,
            axisLineColor = if (scope.axisLineColor != Color.Unspecified) scope.axisLineColor else axisLineColor,
            axisLabelColor = if (scope.axisLabelColor != Color.Unspecified) scope.axisLabelColor else axisLabelColor,
            axisLabelSize = if (scope.axisLabelSize != TextUnit.Unspecified) scope.axisLabelSize else axisLabelSize,
            highlightLineColor = if (scope.highlightLineColor != Color.Unspecified) scope.highlightLineColor else highlightLineColor,
            highlightLineWidth = if (scope.highlightLineWidth != Dp.Unspecified) scope.highlightLineWidth else highlightLineWidth,
        )
    }
}
```

- [ ] **Step 4: Implement ChartThemeProvider**

```kotlin
// ChartThemeProvider.kt
package com.github.mikephil.charting.compose.style

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

val LocalChartStyle = compositionLocalOf { ChartStyle() }

@Composable
fun ChartThemeProvider(
    style: ChartStyle,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalChartStyle provides style) {
        content()
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :MPChartCompose:testDebugUnitTest --tests "com.github.mikephil.charting.compose.style.ChartStyleTest"`
Expected: All 2 tests PASS.

- [ ] **Step 6: Commit**

```bash
git add MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/style/ MPChartCompose/src/test/kotlin/com/github/mikephil/charting/compose/style/
git commit -m "feat: add ChartStyle, ChartThemeProvider with CompositionLocal"
```

---

### Task 12: ChartAnimation Types

**Files:**
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/animation/ChartAnimation.kt`
- Create: `MPChartCompose/src/test/kotlin/com/github/mikephil/charting/compose/animation/ChartAnimationTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
// ChartAnimationTest.kt
package com.github.mikephil.charting.compose.animation

import org.junit.Assert.*
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ChartAnimationTest {

    @Test
    fun `None is singleton`() {
        assertSame(ChartAnimation.None, ChartAnimation.None)
    }

    @Test
    fun `EaseInOut stores duration`() {
        val anim = ChartAnimation.EaseInOut(1.seconds)
        assertEquals(1000L, anim.duration.inWholeMilliseconds)
    }

    @Test
    fun `Spring stores damping and stiffness`() {
        val anim = ChartAnimation.Spring(dampingRatio = 0.5f, stiffness = 200f)
        assertEquals(0.5f, anim.dampingRatio, 0.001f)
        assertEquals(200f, anim.stiffness, 0.001f)
    }

    @Test
    fun `EaseIn and EaseOut store duration`() {
        val easeIn = ChartAnimation.EaseIn(500.milliseconds)
        val easeOut = ChartAnimation.EaseOut(800.milliseconds)
        assertEquals(500L, easeIn.duration.inWholeMilliseconds)
        assertEquals(800L, easeOut.duration.inWholeMilliseconds)
    }
}
```

- [ ] **Step 2: Run tests, verify fail. Step 3: Implement ChartAnimation**

```kotlin
// ChartAnimation.kt
package com.github.mikephil.charting.compose.animation

import kotlin.time.Duration

sealed interface ChartAnimation {
    data object None : ChartAnimation
    data class EaseInOut(val duration: Duration) : ChartAnimation
    data class EaseIn(val duration: Duration) : ChartAnimation
    data class EaseOut(val duration: Duration) : ChartAnimation
    data class Spring(
        val dampingRatio: Float = 0.75f,  // Spring.DampingRatioLowBouncy
        val stiffness: Float = 1500f,   // Spring.StiffnessMedium
    ) : ChartAnimation
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :MPChartCompose:testDebugUnitTest --tests "com.github.mikephil.charting.compose.animation.ChartAnimationTest"`
Expected: All 4 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/animation/ChartAnimation.kt MPChartCompose/src/test/kotlin/com/github/mikephil/charting/compose/animation/
git commit -m "feat: add ChartAnimation sealed interface with easing types"
```

---

### Task 13: ChartState Interfaces and LineChartState Implementation

**Files:**
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/state/ChartState.kt`
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/state/LineChartStateImpl.kt`
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/state/RememberChartState.kt`
- Create: `MPChartCompose/src/test/kotlin/com/github/mikephil/charting/compose/state/ChartStateTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
// ChartStateTest.kt
package com.github.mikephil.charting.compose.state

import com.github.mikephil.charting.compose.data.*
import org.junit.Assert.*
import org.junit.Test

class ChartStateTest {

    @Test
    fun `initial data is accessible`() {
        val initial = LineData(dataSets = listOf(
            LineDataSet(entries = listOf(Entry(0f, 1f)), label = "A")
        ))
        val state = LineChartStateImpl(capacity = 100, initialData = initial)
        assertEquals(1, state.data.dataSets.size)
        assertEquals(1, state.data.dataSets[0].entries.size)
    }

    @Test
    fun `append adds entry to dataset`() {
        val initial = LineData(dataSets = listOf(
            LineDataSet(entries = listOf(Entry(0f, 1f)), label = "A")
        ))
        val state = LineChartStateImpl(capacity = 100, initialData = initial)
        state.appendImmediate(0, Entry(1f, 2f))
        assertEquals(2, state.data.dataSets[0].entries.size)
    }

    @Test
    fun `append respects ring buffer capacity`() {
        val initial = LineData(dataSets = listOf(
            LineDataSet(entries = emptyList(), label = "A")
        ))
        val state = LineChartStateImpl(capacity = 3, initialData = initial)
        state.appendImmediate(0, Entry(0f, 1f))
        state.appendImmediate(0, Entry(1f, 2f))
        state.appendImmediate(0, Entry(2f, 3f))
        state.appendImmediate(0, Entry(3f, 4f))
        val entries = state.data.dataSets[0].entries
        assertEquals(3, entries.size)
        assertEquals(1f, entries[0].x, 0.01f) // oldest (0f) evicted
    }

    @Test
    fun `replaceData swaps all data`() {
        val initial = LineData(dataSets = listOf(
            LineDataSet(entries = listOf(Entry(0f, 1f)), label = "A")
        ))
        val state = LineChartStateImpl(capacity = 100, initialData = initial)
        val newData = LineData(dataSets = listOf(
            LineDataSet(entries = listOf(Entry(10f, 20f), Entry(11f, 21f)), label = "B")
        ))
        state.replaceData(newData)
        assertEquals("B", state.data.dataSets[0].label)
        assertEquals(2, state.data.dataSets[0].entries.size)
    }

    @Test
    fun `clear empties all data`() {
        val initial = LineData(dataSets = listOf(
            LineDataSet(entries = listOf(Entry(0f, 1f)), label = "A")
        ))
        val state = LineChartStateImpl(capacity = 100, initialData = initial)
        state.clear()
        assertTrue(state.data.dataSets[0].entries.isEmpty())
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :MPChartCompose:testDebugUnitTest --tests "com.github.mikephil.charting.compose.state.ChartStateTest"`
Expected: FAIL

- [ ] **Step 3: Implement ChartState interfaces**

```kotlin
// ChartState.kt
package com.github.mikephil.charting.compose.state

import com.github.mikephil.charting.compose.data.*

interface LineChartState {
    val data: LineData
    fun append(dataSetIndex: Int, entry: Entry)
    fun appendAll(dataSetIndex: Int, entries: List<Entry>)
    fun replaceData(data: LineData)
    fun clear()
    // Viewport control
    fun scrollToEnd(animated: Boolean = true)
    fun scrollTo(x: Float, animated: Boolean = true)
    fun setVisibleXRange(range: Float)
}

interface BarChartState {
    val data: BarData
    fun append(dataSetIndex: Int, entry: BarEntry)
    fun appendAll(dataSetIndex: Int, entries: List<BarEntry>)
    fun replaceData(data: BarData)
    fun clear()
    fun scrollToEnd(animated: Boolean = true)
    fun scrollTo(x: Float, animated: Boolean = true)
    fun setVisibleXRange(range: Float)
}

interface CandleChartState {
    val data: CandleData
    fun append(dataSetIndex: Int, entry: CandleEntry)
    fun appendAll(dataSetIndex: Int, entries: List<CandleEntry>)
    fun replaceData(data: CandleData)
    fun clear()
    fun scrollToEnd(animated: Boolean = true)
    fun scrollTo(x: Float, animated: Boolean = true)
    fun setVisibleXRange(range: Float)
}
```

- [ ] **Step 4: Implement LineChartStateImpl**

```kotlin
// LineChartStateImpl.kt
package com.github.mikephil.charting.compose.state

import androidx.compose.runtime.mutableIntStateOf
import com.github.mikephil.charting.compose.data.*
import com.github.mikephil.charting.compose.util.RingBuffer
import kotlinx.coroutines.channels.Channel

internal class LineChartStateImpl(
    private val capacity: Int,
    initialData: LineData,
) : LineChartState {

    private val buffers: List<RingBuffer<Entry>> = initialData.dataSets.map { ds ->
        RingBuffer<Entry>(capacity).also { buf -> buf.appendAll(ds.entries) }
    }.toMutableList()

    private val labels: MutableList<String> = initialData.dataSets.map { it.label }.toMutableList()
    private val configs: MutableList<LineDataSet> = initialData.dataSets.toMutableList()

    // Version counter for snapshot invalidation — read in draw phase
    private var _version = mutableIntStateOf(0)

    override val data: LineData
        get() {
            _version.intValue // read to create snapshot dependency
            return LineData(
                dataSets = buffers.mapIndexed { i, buf ->
                    configs[i].copy(entries = buf.toList())
                }
            )
        }

    // Channel for thread-safe, frame-batched data ingestion
    internal sealed class DataCommand {
        data class Append(val dsIndex: Int, val entry: Entry) : DataCommand()
        data class AppendAll(val dsIndex: Int, val entries: List<Entry>) : DataCommand()
        data class Replace(val data: LineData) : DataCommand()
        data object Clear : DataCommand()
    }

    internal val commandChannel = Channel<DataCommand>(Channel.BUFFERED)

    /** Immediate append for testing (bypasses Channel batching). */
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

    /** Called from frame-synced coroutine to drain all pending commands. */
    internal fun processPendingCommands() {
        var cmd = commandChannel.tryReceive().getOrNull()
        while (cmd != null) {
            when (cmd) {
                is DataCommand.Append -> buffers[cmd.dsIndex].append(cmd.entry)
                is DataCommand.AppendAll -> buffers[cmd.dsIndex].appendAll(cmd.entries)
                is DataCommand.Replace -> { /* handled inline */ }
                is DataCommand.Clear -> buffers.forEach { it.clear() }
            }
            cmd = commandChannel.tryReceive().getOrNull()
        }
        _version.intValue++
    }

    override fun replaceData(data: LineData) {
        // Resize buffers to match new dataset count
        while ((buffers as MutableList).size < data.dataSets.size) {
            buffers.add(RingBuffer(capacity))
        }
        while (buffers.size > data.dataSets.size) {
            (buffers as MutableList).removeAt(buffers.size - 1)
        }
        data.dataSets.forEachIndexed { i, ds ->
            buffers[i].replaceAll(ds.entries)
        }
        configs.clear()
        configs.addAll(data.dataSets)
        labels.clear()
        labels.addAll(data.dataSets.map { it.label })
        _version.intValue++
    }

    override fun clear() {
        buffers.forEach { it.clear() }
        _version.intValue++
    }

    // --- Viewport control ---
    internal var _translationX = mutableFloatStateOf(0f)
    internal var _visibleXRange = mutableFloatStateOf(Float.MAX_VALUE)

    override fun scrollToEnd(animated: Boolean) {
        _translationX.floatValue = 0f // 0 = default end position
    }

    override fun scrollTo(x: Float, animated: Boolean) {
        val dataXMax = buffers.flatMap { it.toList() }.maxOfOrNull { it.x } ?: 0f
        _translationX.floatValue = -(dataXMax - x)
    }

    override fun setVisibleXRange(range: Float) {
        _visibleXRange.floatValue = range
    }
}
```

- [ ] **Step 5: Implement RememberChartState (composable factories)**

```kotlin
// RememberChartState.kt
package com.github.mikephil.charting.compose.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import com.github.mikephil.charting.compose.data.*

@Composable
fun rememberLineChartState(
    capacity: Int = Int.MAX_VALUE,
    initialData: LineData = LineData(dataSets = emptyList()),
): LineChartState {
    val state = remember { LineChartStateImpl(capacity = capacity, initialData = initialData) }
    // Frame-batched command processing: drain all pending appends once per vsync
    LaunchedEffect(state) {
        while (true) {
            withFrameMillis { }
            state.processPendingCommands()
        }
    }
    return state
}

@Composable
fun rememberBarChartState(
    capacity: Int = Int.MAX_VALUE,
    initialData: BarData = BarData(dataSets = emptyList()),
): BarChartState {
    val state = remember { BarChartStateImpl(capacity = capacity, initialData = initialData) }
    LaunchedEffect(state) {
        while (true) {
            withFrameMillis { }
            state.processPendingCommands()
        }
    }
    return state
}

@Composable
fun rememberCandleChartState(
    capacity: Int = Int.MAX_VALUE,
    initialData: CandleData = CandleData(dataSets = emptyList()),
): CandleChartState {
    val state = remember { CandleChartStateImpl(capacity = capacity, initialData = initialData) }
    LaunchedEffect(state) {
        while (true) {
            withFrameMillis { }
            state.processPendingCommands()
        }
    }
    return state
}
```

- [ ] **Step 6: Implement BarChartStateImpl and CandleChartStateImpl**

Follow the same pattern as `LineChartStateImpl` but using `BarEntry`/`BarData`/`BarDataSet` and `CandleEntry`/`CandleData`/`CandleDataSet` respectively.

Create:
- `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/state/BarChartStateImpl.kt`
- `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/state/CandleChartStateImpl.kt`

- [ ] **Step 7: Run tests to verify they pass**

Run: `./gradlew :MPChartCompose:testDebugUnitTest --tests "com.github.mikephil.charting.compose.state.ChartStateTest"`
Expected: All 5 tests PASS.

- [ ] **Step 8: Commit**

```bash
git add MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/state/ MPChartCompose/src/test/kotlin/com/github/mikephil/charting/compose/state/
git commit -m "feat: add ChartState interfaces and streaming implementations"
```

---

### Task 14: ChartAnimator (Phase Animation)

**Files:**
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/animation/ChartAnimator.kt`

The ChartAnimator manages `Animatable<Float>` values for appear phase (0→1) and data transition interpolation. This class is tightly coupled with Compose's animation runtime and is tested via integration tests later.

- [ ] **Step 1: Implement ChartAnimator**

```kotlin
// ChartAnimator.kt
package com.github.mikephil.charting.compose.animation

import androidx.compose.animation.core.*
import kotlin.time.Duration

internal class ChartAnimator {

    var appearPhase: Float = 0f
        private set

    var transitionPhase: Float = 1f
        private set

    suspend fun animateAppear(animation: ChartAnimation, onFrame: (Float) -> Unit) {
        when (animation) {
            is ChartAnimation.None -> {
                appearPhase = 1f
                onFrame(1f)
            }
            is ChartAnimation.EaseInOut -> animatePhase(animation.duration, FastOutSlowInEasing, onFrame) { appearPhase = it }
            is ChartAnimation.EaseIn -> animatePhase(animation.duration, FastOutLinearInEasing, onFrame) { appearPhase = it }
            is ChartAnimation.EaseOut -> animatePhase(animation.duration, LinearOutSlowInEasing, onFrame) { appearPhase = it }
            is ChartAnimation.Spring -> {
                val animatable = Animatable(0f)
                animatable.animateTo(1f, spring(animation.dampingRatio, animation.stiffness)) {
                    appearPhase = value
                    onFrame(value)
                }
            }
        }
    }

    suspend fun animateTransition(animation: ChartAnimation, onFrame: (Float) -> Unit) {
        transitionPhase = 0f
        when (animation) {
            is ChartAnimation.None -> {
                transitionPhase = 1f
                onFrame(1f)
            }
            is ChartAnimation.EaseInOut -> animatePhase(animation.duration, FastOutSlowInEasing, onFrame) { transitionPhase = it }
            is ChartAnimation.EaseIn -> animatePhase(animation.duration, FastOutLinearInEasing, onFrame) { transitionPhase = it }
            is ChartAnimation.EaseOut -> animatePhase(animation.duration, LinearOutSlowInEasing, onFrame) { transitionPhase = it }
            is ChartAnimation.Spring -> {
                val animatable = Animatable(0f)
                animatable.animateTo(1f, spring(animation.dampingRatio, animation.stiffness)) {
                    transitionPhase = value
                    onFrame(value)
                }
            }
        }
    }

    private suspend fun animatePhase(
        duration: Duration,
        easing: Easing,
        onFrame: (Float) -> Unit,
        setter: (Float) -> Unit,
    ) {
        val animatable = Animatable(0f)
        animatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = duration.inWholeMilliseconds.toInt(),
                easing = easing,
            ),
        ) {
            setter(value)
            onFrame(value)
        }
    }
}
```

- [ ] **Step 2: Verify module compiles**

Run: `./gradlew :MPChartCompose:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/animation/ChartAnimator.kt
git commit -m "feat: add ChartAnimator with phase-based appear and transition animations"
```

---

## Chunk 5: Renderers

### Task 15: BackgroundRenderer and GridRenderer

**Files:**
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/renderer/BackgroundRenderer.kt`
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/renderer/GridRenderer.kt`

- [ ] **Step 1: Implement BackgroundRenderer**

```kotlin
// BackgroundRenderer.kt
package com.github.mikephil.charting.compose.renderer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

internal object BackgroundRenderer {
    fun DrawScope.drawBackground(color: Color) {
        drawRect(color = color)
    }
}
```

- [ ] **Step 2: Implement GridRenderer**

```kotlin
// GridRenderer.kt
package com.github.mikephil.charting.compose.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import com.github.mikephil.charting.compose.util.Transformer

internal object GridRenderer {
    fun DrawScope.drawGrid(
        transformer: Transformer,
        xLabelValues: List<Float>,
        yLabelValues: List<Float>,
        gridColor: Color,
        gridLineWidth: Dp,
        drawXGrid: Boolean,
        drawYGrid: Boolean,
        axisLineColor: Color,
        drawXAxisLine: Boolean,
        drawYAxisLine: Boolean,
    ) {
        val strokeWidth = gridLineWidth.toPx()
        val contentRect = transformer.contentRect

        if (drawYGrid) {
            for (xVal in xLabelValues) {
                val px = transformer.dataToPixel(xVal, 0f).x
                if (px in contentRect.left..contentRect.right) {
                    drawLine(
                        color = gridColor,
                        start = Offset(px, contentRect.top),
                        end = Offset(px, contentRect.bottom),
                        strokeWidth = strokeWidth,
                    )
                }
            }
        }

        if (drawXGrid) {
            for (yVal in yLabelValues) {
                val py = transformer.dataToPixel(0f, yVal).y
                if (py in contentRect.top..contentRect.bottom) {
                    drawLine(
                        color = gridColor,
                        start = Offset(contentRect.left, py),
                        end = Offset(contentRect.right, py),
                        strokeWidth = strokeWidth,
                    )
                }
            }
        }

        // Axis lines
        if (drawXAxisLine) {
            drawLine(
                color = axisLineColor,
                start = Offset(contentRect.left, contentRect.bottom),
                end = Offset(contentRect.right, contentRect.bottom),
                strokeWidth = strokeWidth * 2,
            )
        }
        if (drawYAxisLine) {
            drawLine(
                color = axisLineColor,
                start = Offset(contentRect.left, contentRect.top),
                end = Offset(contentRect.left, contentRect.bottom),
                strokeWidth = strokeWidth * 2,
            )
        }
    }
}
```

- [ ] **Step 3: Verify module compiles**

Run: `./gradlew :MPChartCompose:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/renderer/BackgroundRenderer.kt MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/renderer/GridRenderer.kt
git commit -m "feat: add BackgroundRenderer and GridRenderer (Layers 1-2)"
```

---

### Task 16: AxisLabelRenderer and LimitLineRenderer

**Files:**
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/renderer/AxisLabelRenderer.kt`
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/renderer/LimitLineRenderer.kt`

- [ ] **Step 1: Implement AxisLabelRenderer**

Uses `TextMeasurer` (from `rememberTextMeasurer()` passed in) to draw axis label text at computed positions. Handles X-axis labels (at bottom or top) and Y-axis labels (at left). Formats values using the provided `formatter: (Float) -> String` lambda.

Reference `androidx.compose.ui.text.drawText` API in DrawScope. The renderer receives a pre-created `TextMeasurer` to avoid creating it in the draw phase.

- [ ] **Step 2: Implement LimitLineRenderer**

Draws horizontal lines at specified Y values with optional dash effect and label text. Uses `PathEffect` for dashing and `TextMeasurer` for labels.

- [ ] **Step 3: Verify module compiles**

Run: `./gradlew :MPChartCompose:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/renderer/AxisLabelRenderer.kt MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/renderer/LimitLineRenderer.kt
git commit -m "feat: add AxisLabelRenderer and LimitLineRenderer"
```

---

### Task 17: LineDataRenderer

**Files:**
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/renderer/LineDataRenderer.kt`
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/util/PathCache.kt`

- [ ] **Step 1: Implement PathCache**

```kotlin
// PathCache.kt
package com.github.mikephil.charting.compose.util

import androidx.compose.ui.graphics.Path

internal class PathCache {
    private var cachedPath: Path? = null
    private var cachedEntryCount: Int = 0
    private var cachedVersion: Int = -1

    fun getOrBuildPath(
        entryCount: Int,
        version: Int,
        builder: (Path, startIndex: Int) -> Unit,
    ): Path {
        val existing = cachedPath
        // Can append if version incremented by 1 and entry count grew
        if (existing != null && version == cachedVersion + 1 && entryCount > cachedEntryCount) {
            builder(existing, cachedEntryCount)
            cachedEntryCount = entryCount
            cachedVersion = version
            return existing
        }
        // Full rebuild
        val path = Path()
        builder(path, 0)
        cachedPath = path
        cachedEntryCount = entryCount
        cachedVersion = version
        return path
    }

    fun invalidate() {
        cachedPath = null
        cachedEntryCount = 0
        cachedVersion = -1
    }
}
```

- [ ] **Step 1b: Write PathCache tests**

Create `MPChartCompose/src/test/kotlin/com/github/mikephil/charting/compose/util/PathCacheTest.kt`:

```kotlin
package com.github.mikephil.charting.compose.util

import org.junit.Assert.*
import org.junit.Test

class PathCacheTest {

    @Test
    fun `first call builds full path from index 0`() {
        val cache = PathCache()
        var buildStartIndex = -1
        cache.getOrBuildPath(entryCount = 5, version = 0) { _, startIndex ->
            buildStartIndex = startIndex
        }
        assertEquals(0, buildStartIndex)
    }

    @Test
    fun `sequential version with more entries appends from last count`() {
        val cache = PathCache()
        cache.getOrBuildPath(entryCount = 5, version = 0) { _, _ -> }
        var buildStartIndex = -1
        cache.getOrBuildPath(entryCount = 8, version = 1) { _, startIndex ->
            buildStartIndex = startIndex
        }
        assertEquals(5, buildStartIndex) // append from index 5
    }

    @Test
    fun `non-sequential version triggers full rebuild`() {
        val cache = PathCache()
        cache.getOrBuildPath(entryCount = 5, version = 0) { _, _ -> }
        var buildStartIndex = -1
        cache.getOrBuildPath(entryCount = 3, version = 5) { _, startIndex ->
            buildStartIndex = startIndex
        }
        assertEquals(0, buildStartIndex) // full rebuild
    }

    @Test
    fun `invalidate forces full rebuild on next call`() {
        val cache = PathCache()
        cache.getOrBuildPath(entryCount = 5, version = 0) { _, _ -> }
        cache.invalidate()
        var buildStartIndex = -1
        cache.getOrBuildPath(entryCount = 5, version = 1) { _, startIndex ->
            buildStartIndex = startIndex
        }
        assertEquals(0, buildStartIndex)
    }
}
```

Run: `./gradlew :MPChartCompose:testDebugUnitTest --tests "com.github.mikephil.charting.compose.util.PathCacheTest"`
Expected: All 4 tests PASS after PathCache implementation.

- [ ] **Step 2: Implement LineDataRenderer**

Implements all 4 line modes (Linear, CubicBezier, HorizontalBezier, Stepped). Handles:
- Path building per mode using `Transformer` for coordinate conversion
- Fill area (using `fillBrush` or `fillColor`, drawing a closed path from line to bottom)
- Glow effect (3-pass: outer with `BlurMaskFilter` via `drawIntoCanvas`, inner, core)
- Circle points (when `drawCircles = true`)
- Phase animation (multiply path progress by `appearPhase`)

Key implementation notes:
- Use `drawIntoCanvas { canvas -> canvas.nativeCanvas }` for `BlurMaskFilter` on glow
- Reuse `PathCache` for append-only optimizations
- Apply `appearPhase` by limiting the portion of the path drawn (for left-to-right animation, only draw entries up to `entries.size * phase`)

- [ ] **Step 3: Verify module compiles**

Run: `./gradlew :MPChartCompose:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/renderer/LineDataRenderer.kt MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/util/PathCache.kt
git commit -m "feat: add LineDataRenderer with all line modes, fill, glow, and path caching"
```

---

### Task 18: BarDataRenderer and CandleDataRenderer

**Files:**
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/renderer/BarDataRenderer.kt`
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/renderer/CandleDataRenderer.kt`

- [ ] **Step 1: Implement BarDataRenderer**

Draws bar rectangles for each `BarEntry`. Handles:
- Bar width calculation based on `BarData.barWidth` and available pixel space
- Stacked bars (when `stackValues` is non-null, draw segments stacked vertically)
- Negative values (bars draw downward from zero baseline)
- `barBrush` gradient support (use `drawRect(brush = ...)` when non-null, else `drawRect(color = ...)`)
- Phase animation (bars grow upward: multiply height by `appearPhase`)

- [ ] **Step 2: Implement CandleDataRenderer**

Draws candlestick bodies and shadow wicks. Handles:
- Shadow line from `high` to `low` (thin line)
- Body rectangle from `open` to `close`
- Color based on increasing (`close >= open`) vs decreasing (`close < open`)
- `bodyWidth` as fraction of available space
- Phase animation (candle body expands from center: multiply body height by `appearPhase`)

- [ ] **Step 3: Verify module compiles**

Run: `./gradlew :MPChartCompose:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/renderer/BarDataRenderer.kt MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/renderer/CandleDataRenderer.kt
git commit -m "feat: add BarDataRenderer and CandleDataRenderer (Layer 3)"
```

---

### Task 19: HighlightRenderer and LegendRenderer

**Files:**
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/renderer/HighlightRenderer.kt`
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/renderer/LegendRenderer.kt`
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/highlight/HighlightState.kt`

- [ ] **Step 1: Implement HighlightState**

```kotlin
// HighlightState.kt
package com.github.mikephil.charting.compose.highlight

import androidx.compose.ui.geometry.Offset

data class Highlight(
    val entryIndex: Int,
    val dataSetIndex: Int,
    val pixelPosition: Offset,
    val dataX: Float,
    val dataY: Float,
)
```

- [ ] **Step 2: Implement HighlightRenderer**

Draws Layer 4: vertical crosshair line, horizontal crosshair line, and scrub indicator circle at the highlight position. Uses `HighlightScope` configuration for colors, sizes, and toggle flags.

- [ ] **Step 3: Implement LegendRenderer**

Draws Layer 5: legend forms (small colored squares/circles) and labels for each dataset. Positions based on `LegendScope` configuration (Top, Bottom, Left, Right).

- [ ] **Step 4: Verify module compiles**

Run: `./gradlew :MPChartCompose:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/renderer/HighlightRenderer.kt MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/renderer/LegendRenderer.kt MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/highlight/
git commit -m "feat: add HighlightRenderer, LegendRenderer, and HighlightState (Layers 4-5)"
```

---

## Chunk 6: Gestures, Markers & Chart Composables

### Task 20: Gesture Handler

**Files:**
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/gesture/ChartGestureHandler.kt`
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/gesture/FlingDecelerator.kt`

- [ ] **Step 1: Implement FlingDecelerator**

Uses `Animatable` with `exponentialDecay` to animate fling after pan drag ends.

```kotlin
// FlingDecelerator.kt
package com.github.mikephil.charting.compose.gesture

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay

internal class FlingDecelerator {
    suspend fun fling(
        initialVelocity: Float,
        onFrame: (delta: Float) -> Unit,
    ) {
        val animatable = Animatable(0f)
        var previous = 0f
        animatable.animateDecay(
            initialVelocity = initialVelocity,
            animationSpec = exponentialDecay(frictionMultiplier = 1.5f),
        ) {
            val delta = value - previous
            previous = value
            onFrame(delta)
        }
    }
}
```

- [ ] **Step 2: Implement ChartGestureHandler**

A `Modifier` extension function that applies `pointerInput` blocks for all gesture types. Reads `GestureScope` configuration to enable/disable each gesture. Implements:

- `detectTapGestures` for tap-to-highlight
- `detectDragGestures` for pan and scrub (with disambiguation based on `scrubThreshold`)
- Fling deceleration after pan ends (using `FlingDecelerator`)
- Updates `highlightState: MutableState<Highlight?>` and `translationX: MutableState<Float>`

```kotlin
// ChartGestureHandler.kt
package com.github.mikephil.charting.compose.gesture

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.github.mikephil.charting.compose.data.Entry
import com.github.mikephil.charting.compose.highlight.Highlight
import com.github.mikephil.charting.compose.scope.GestureScope
import com.github.mikephil.charting.compose.util.EntryFinder
import com.github.mikephil.charting.compose.util.Transformer

/**
 * Generic gesture handler. Works with any entry type by accepting:
 * - entryXValues: list of x-coordinates for hit-testing
 * - onEntryFound: callback when an entry is found at a given index
 */
internal fun <T> Modifier.chartGestures(
    gestureConfig: GestureScope,
    transformer: () -> Transformer?,
    entryXValues: () -> List<Float>,
    findEntryAtIndex: (Int) -> T?,
    highlightState: MutableState<Highlight?>,
    translationX: MutableState<Float>,
    onValueSelected: ((T) -> Unit)?,
): Modifier {
    var mod = this

    // Helper: find nearest entry by x-coordinate and invoke callback
    fun handleSelection(pixelOffset: Offset) {
        val t = transformer() ?: return
        val (dataX, _) = t.pixelToData(pixelOffset)
        val xValues = entryXValues()
        val idx = EntryFinder.findNearestIndexByX(xValues, dataX)
        if (idx < 0) return
        val entry = findEntryAtIndex(idx) ?: return
        val nearestX = xValues[idx]
        // Use dataX for pixel position lookup (approx — chart composable
        // provides actual data-to-pixel via transformer)
        val px = t.dataToPixel(nearestX, 0f) // Y is approximate; highlight renderer uses entry
        highlightState.value = Highlight(
            entryIndex = idx,
            dataSetIndex = 0,
            pixelPosition = px,
            dataX = nearestX,
            dataY = 0f, // actual Y resolved by chart composable
        )
        onValueSelected?.invoke(entry)
    }

    if (gestureConfig.tapToHighlight) {
        mod = mod.pointerInput(gestureConfig) {
            detectTapGestures { offset -> handleSelection(offset) }
        }
    }
    if (gestureConfig.dragToPan || gestureConfig.dragToScrub) {
        mod = mod.pointerInput(gestureConfig) {
            detectDragGestures(
                onDrag = { change, dragAmount ->
                    change.consume()
                    if (gestureConfig.dragToPan && !gestureConfig.dragToScrub) {
                        translationX.value += dragAmount.x
                    } else if (gestureConfig.dragToScrub && !gestureConfig.dragToPan) {
                        handleSelection(change.position)
                    }
                    // When both enabled: disambiguation logic using scrubThreshold
                    // (threshold check on drag start determines mode for entire drag)
                },
            )
        }
    }
    return mod
}
```

- [ ] **Step 3: Verify module compiles**

Run: `./gradlew :MPChartCompose:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/gesture/
git commit -m "feat: add ChartGestureHandler with tap, pan, scrub, and fling"
```

---

### Task 21: MarkerPositioner and DynamicAxisLabel

**Files:**
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/marker/MarkerPositioner.kt`
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/marker/DynamicAxisLabel.kt`

- [ ] **Step 1: Implement MarkerPositioner**

A composable that measures its child content, then positions it relative to the highlight point with edge avoidance:

```kotlin
// MarkerPositioner.kt
package com.github.mikephil.charting.compose.marker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

@Composable
internal fun MarkerPositioner(
    position: Offset,
    chartSize: IntSize,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var markerSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .onGloballyPositioned { markerSize = it.size }
            .offset {
                val x = (position.x - markerSize.width / 2f)
                    .coerceIn(0f, (chartSize.width - markerSize.width).toFloat())
                val y = position.y - markerSize.height - 16f // 16px gap above point
                val adjustedY = if (y < 0f) position.y + 16f else y // flip below if clips top
                IntOffset(x.toInt(), adjustedY.toInt())
            }
    ) {
        content()
    }
}
```

- [ ] **Step 2: Implement DynamicAxisLabel**

Composable for the pill/badge labels on Y and X axes during scrub:

```kotlin
// DynamicAxisLabel.kt
package com.github.mikephil.charting.compose.marker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicText

@Composable
internal fun DynamicYAxisLabel(
    position: Offset,
    label: String,
    textColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .offset { IntOffset(0, (position.y - 12f).toInt()) }
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        BasicText(text = label, style = TextStyle(color = textColor, fontSize = 10.sp))
    }
}

@Composable
internal fun DynamicXAxisLabel(
    position: Offset,
    chartBottom: Float,
    label: String,
    textColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .offset { IntOffset((position.x - 30f).toInt(), chartBottom.toInt()) }
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        BasicText(text = label, style = TextStyle(color = textColor, fontSize = 10.sp))
    }
}
```

- [ ] **Step 3: Verify module compiles**

Run: `./gradlew :MPChartCompose:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/marker/
git commit -m "feat: add MarkerPositioner and DynamicAxisLabel composables"
```

---

### Task 22: ChartCore (Shared Chart Structure)

**Files:**
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/chart/ChartCore.kt`

This is the shared `Box` + Canvas + overlay structure used by all three chart types. It wires together:
- The 5-layer draw pipeline via `Modifier.drawBehind`
- The overlay composition layer (marker + dynamic axis labels)
- Gesture handling via `chartGestures` modifier
- Animation via `ChartAnimator` + `LaunchedEffect`
- Viewport via `ViewportHandler` + `Transformer`
- Style resolution (per-chart scope → theme → defaults)

- [ ] **Step 1: Implement ChartCore**

The `ChartCore` composable is an internal function that accepts:
- `modifier: Modifier`
- `style: ChartStyle` (resolved)
- `xAxisConfig: XAxisScope`, `yAxisConfig: YAxisScope`
- `gestureConfig: GestureScope`
- `highlightConfig: HighlightScope`
- `animationConfig: AnimationScope`
- `viewportConfig: ViewportScope`
- `legendConfig: LegendScope`
- `drawData: DrawScope.(Transformer, Float) -> Unit` — chart-type-specific data drawing lambda
- `highlightState: MutableState<Highlight?>`
- `marker: (@Composable (Any) -> Unit)?`
- `dataXMin/dataXMax: Float`
- `dataYMin/dataYMax: Float`
- `entries: List<Entry>` (for gesture hit testing)

Structure:
```
Box {
    // Canvas layer
    Modifier.drawBehind {
        Layer 1: BackgroundRenderer.drawBackground(style.backgroundColor)
        Layer 2: GridRenderer.drawGrid(...) + AxisLabelRenderer + LimitLineRenderer
        Layer 3: drawData(transformer, appearPhase)  // chart-specific
        Layer 4: HighlightRenderer.drawHighlight(...)
        Layer 5: LegendRenderer.drawLegend(...)
    }
    // Overlay layer
    MarkerPositioner + DynamicAxisLabels (when highlight is active)
}
```

This is the most complex file — take care to wire all systems correctly. Reference the spec's "Composable Layout Structure" section.

- [ ] **Step 2: Verify module compiles**

Run: `./gradlew :MPChartCompose:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/chart/ChartCore.kt
git commit -m "feat: add ChartCore shared chart structure with 5-layer pipeline"
```

---

### Task 23: LineChart Composable

**Files:**
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/chart/LineChart.kt`

- [ ] **Step 1: Implement LineChart composable**

The public `LineChart()` function that matches the spec signature. It:
1. Resolves `data` from either static `data` param or streaming `state` param
2. Builds scope configurations from lambda params (applying defaults for nulls)
3. Resolves style (per-chart scope → theme → library defaults)
4. Delegates to `ChartCore` with a `drawData` lambda that calls `LineDataRenderer`

```kotlin
// LineChart.kt
package com.github.mikephil.charting.compose.chart

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.github.mikephil.charting.compose.data.*
import com.github.mikephil.charting.compose.scope.*
import com.github.mikephil.charting.compose.state.LineChartState

@Composable
fun LineChart(
    modifier: Modifier = Modifier,
    data: LineData? = null,
    state: LineChartState? = null,
    xAxis: (XAxisScope.() -> Unit)? = null,
    yAxis: (YAxisScope.() -> Unit)? = null,
    gestures: (GestureScope.() -> Unit)? = null,
    highlight: (HighlightScope.() -> Unit)? = null,
    marker: (@Composable (Entry) -> Unit)? = null,
    animation: (AnimationScope.() -> Unit)? = null,
    style: (ChartStyleScope.() -> Unit)? = null,
    legend: (LegendScope.() -> Unit)? = null,
    viewport: (ViewportScope.() -> Unit)? = null,
    onValueSelected: ((Entry) -> Unit)? = null,
) {
    require(data != null || state != null) { "Either data or state must be provided" }
    require(data == null || state == null) { "data and state are mutually exclusive" }

    val resolvedData = data ?: state!!.data

    // Build scope configs
    val xAxisConfig = remember { XAxisScope() }.also { xAxis?.invoke(it) }
    val yAxisConfig = remember { YAxisScope() }.also { yAxis?.invoke(it) }
    val gestureConfig = remember { GestureScope() }.also { gestures?.invoke(it) }
    val highlightConfig = remember { HighlightScope() }.also { highlight?.invoke(it) }
    val animationConfig = remember { AnimationScope() }.also { animation?.invoke(it) }
    val styleConfig = remember { ChartStyleScope() }.also { style?.invoke(it) }
    val legendConfig = remember { LegendScope() }.also { legend?.invoke(it) }
    val viewportConfig = remember { ViewportScope() }.also { viewport?.invoke(it) }

    // Delegate to ChartCore with LineDataRenderer as the data drawing lambda
    // ... (wire all params to ChartCore)
}
```

Complete implementation wires the DSL scopes to `ChartCore` and provides the `drawData` lambda that calls `LineDataRenderer` for each `LineDataSet`.

- [ ] **Step 2: Verify module compiles**

Run: `./gradlew :MPChartCompose:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/chart/LineChart.kt
git commit -m "feat: add LineChart composable with full DSL API"
```

---

### Task 24: BarChart and CandleChart Composables

**Files:**
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/chart/BarChart.kt`
- Create: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/chart/CandleChart.kt`

- [ ] **Step 1: Implement BarChart composable**

Same pattern as `LineChart` but with `BarData`/`BarEntry`/`BarChartState` types and `BarDataRenderer` in the `drawData` lambda.

- [ ] **Step 2: Implement CandleChart composable**

Same pattern as `LineChart` but with `CandleData`/`CandleEntry`/`CandleChartState` types and `CandleDataRenderer` in the `drawData` lambda.

- [ ] **Step 3: Verify module compiles**

Run: `./gradlew :MPChartCompose:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/chart/BarChart.kt MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/chart/CandleChart.kt
git commit -m "feat: add BarChart and CandleChart composables"
```

---

## Chunk 7: Integration, Polish & Cleanup

### Task 25: Remove Placeholder File

**Files:**
- Delete: `MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/MPChartCompose.kt`

- [ ] **Step 1: Remove the placeholder**

```bash
rm MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/MPChartCompose.kt
```

- [ ] **Step 2: Verify module still compiles**

Run: `./gradlew :MPChartCompose:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add -u MPChartCompose/src/main/kotlin/com/github/mikephil/charting/compose/MPChartCompose.kt
git commit -m "chore: remove module placeholder file"
```

---

### Task 26: Run Full Test Suite

- [ ] **Step 1: Run all MPChartCompose tests**

Run: `./gradlew :MPChartCompose:testDebugUnitTest`
Expected: All tests PASS.

- [ ] **Step 2: Run existing MPChartLib tests (regression check)**

Run: `./gradlew :MPChartLib:test`
Expected: All existing tests still PASS.

- [ ] **Step 3: Full project build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL across all modules.

---

### Task 27: Final Integration Commit

- [ ] **Step 1: Verify all files are committed**

Run: `git status`
Expected: Clean working tree.

- [ ] **Step 2: If any uncommitted changes, commit them**

```bash
git add -A
git commit -m "chore: final cleanup for MPChartCompose v1.0.0-alpha01"
```
