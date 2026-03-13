# MPChartCompose — Jetpack Compose Charting Library Design

## Overview

A native Jetpack Compose charting library built as a new module (`MPChartCompose/`) alongside the existing MPAndroidChart Java library. Designed for real-time data streaming (WebSocket stock tickers) with 60fps rendering performance.

**Initial chart types:** Line, Bar, Candlestick

**Target:** Android-only Compose, min SDK 23.

## Constraints & Requirements

- Real-time WebSocket data at high tick rates without frame drops
- Zero recomposition on data changes — draw-phase-only updates
- Frame-batched data ingestion (1 update per vsync regardless of tick frequency)
- Immutable public API with smart internals (ring buffer, path caching)
- Composable-first API with lambda DSL configuration
- Standalone theme system — no Material3 dependency
- All gestures individually toggleable

## Build Configuration

The new module requires a Gradle/Kotlin/Compose toolchain upgrade from the current project baseline.

### Required Project-Level Changes

- **Gradle:** 8.2+ (from current 7.2)
- **AGP:** 8.2+ (from current 7.0.4)
- **Kotlin:** 1.9.22+ with Compose compiler Gradle plugin
- **Compose BOM:** 2024.02.00+ (or latest stable)

The existing `MPChartLib/` module (Java, compileSdk 28, minSdk 14) remains untouched. The new `MPChartCompose/` module diverges:

### MPChartCompose build.gradle.kts

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
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui-graphics")
    // No Material dependency — standalone theme system
    // No dependency on :MPChartLib — clean-room implementation

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
```

**Note:** `MPChartCompose` has zero dependency on the existing `MPChartLib`. It is a clean-room implementation using the original as a reference only.

## Module Structure

```
MPChartCompose/
├── src/main/kotlin/com/github/mikephil/charting/compose/
│   ├── data/           # Entry, DataSet, ChartData (immutable data classes)
│   ├── chart/          # LineChart, BarChart, CandleChart composables
│   ├── renderer/       # Drawing logic per chart type
│   ├── axis/           # Axis configuration & rendering
│   ├── style/          # ChartStyle, ChartThemeProvider
│   ├── animation/      # ChartAnimation, phase-based + transitions
│   ├── gesture/        # Tap, pan, scrub gesture handling
│   ├── highlight/      # Highlight state, scrub indicator, dynamic axis labels
│   ├── marker/         # Composable marker positioning
│   └── util/           # Transformer, viewport, path caching, ring buffer
└── src/test/           # Unit tests
```

## Data Model

Immutable public API with smart internals. Consumers work with immutable data classes. Internally, bounded datasets use a ring buffer for WebSocket streaming to avoid list copying on every tick. Frame-batching via `Channel` + `withFrameMillis` ensures 1 state update per vsync.

### Entries

```kotlin
data class Entry(val x: Float, val y: Float, val data: Any? = null)

data class BarEntry(
    val x: Float,
    val y: Float,             // Total value; ignored when stackValues is non-null
    val stackValues: List<Float>? = null,  // When non-null, y = stackValues.sum()
    val data: Any? = null,
)

data class CandleEntry(
    val x: Float,
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float,
    val data: Any? = null,
)
```

**Note on `BarEntry`:** When `stackValues` is non-null, `y` is derived as `stackValues.sum()` and any provided `y` value is ignored. This follows standard OHLC ordering (diverges from the original library's `(x, shadowHigh, shadowLow, open, close)` order for clarity). `stackValues` uses `List<Float>` (not `FloatArray`) to ensure correct `equals`/`hashCode` behavior for data class comparison, Compose snapshot diffing, and animation transitions.

### DataSets

```kotlin
data class LineDataSet(
    val entries: List<Entry>,
    val label: String,
    val lineColor: Color = Color.Unspecified,
    val lineWidth: Dp = 2.dp,
    val drawCircles: Boolean = true,
    val circleRadius: Dp = 4.dp,
    val mode: LineMode = LineMode.Linear,
    val fillEnabled: Boolean = false,
    val fillBrush: Brush? = null,       // Gradient or solid fill under line
    val fillColor: Color = Color.Unspecified,  // Shorthand; fillBrush takes precedence
)

data class BarDataSet(
    val entries: List<BarEntry>,
    val label: String,
    val barColor: Color = Color.Unspecified,
    val barBrush: Brush? = null,         // Gradient fill for bars; takes precedence over barColor
)

data class CandleDataSet(
    val entries: List<CandleEntry>,
    val label: String,
    val increasingColor: Color = Color(0xFF00C853),
    val decreasingColor: Color = Color(0xFFFF1744),
    val shadowColor: Color = Color(0xFF888888),
    val bodyWidth: Float = 0.7f,          // Fraction of available space (0.0–1.0)
)
```

### Enums and Supporting Types

```kotlin
enum class LineMode {
    Linear,          // Straight lines between points
    CubicBezier,     // Smooth cubic bezier curves
    HorizontalBezier,// Horizontal bezier (flat entry/exit tangents)
    Stepped,         // Step-line (horizontal then vertical)
}

enum class AxisPosition { Top, Bottom }

data class LimitLine(
    val value: Float,
    val label: String = "",
    val color: Color = Color(0xFFFFAA00),
    val lineWidth: Dp = 1.dp,
    val dashEffect: PathEffect? = null,
)
```

### ChartData Containers

```kotlin
data class LineData(val dataSets: List<LineDataSet>)
data class BarData(val dataSets: List<BarDataSet>, val barWidth: Float = 0.85f)
data class CandleData(val dataSets: List<CandleDataSet>)
```

**Note:** `barWidth` lives on `BarData` only (not on `BarDataSet`) — it controls the fraction of available horizontal space used by bars and applies uniformly across all datasets.

### Invalid Data Handling

- **Empty `dataSets` list:** Chart renders background, axes, and grid only. No crash.
- **DataSet with 0 entries:** Skipped during rendering. Legend still shows label.
- **DataSet with 1 entry:** Rendered as a single point (LineChart) or single bar/candle.
- **Unsorted entries by x-value:** Library does not auto-sort. Entries are rendered in order. For correct results, consumers must provide entries sorted by x.
- **`CandleEntry` with `high < low`:** Rendered as-is (degenerate candle). No validation error.
- **Negative `BarEntry.y`:** Supported. Bars draw downward from the zero baseline.

## Streaming Data API

Two modes of operation: static data (immutable snapshots) and streaming data (real-time WebSocket updates).

### Static Mode

Pass immutable `LineData`/`BarData`/`CandleData` directly. When you assign new data, the chart animates the transition (if `dataTransition` is configured).

```kotlin
var data by remember { mutableStateOf(LineData(dataSets = listOf(...))) }
LineChart(data = data, ...)

// Later: swap data → triggers transition animation
data = LineData(dataSets = listOf(newDataSet))
```

### Streaming Mode

Use `rememberChartState()` for real-time WebSocket data. This provides a frame-batched, ring-buffer-backed state holder.

```kotlin
// Create streaming state with bounded capacity
val chartState = rememberLineChartState(
    capacity = 500,             // Ring buffer size — oldest entries evicted on overflow
    initialData = LineData(dataSets = listOf(...)),  // Optional initial snapshot
)

// Collect WebSocket ticks — can be called from any thread/dispatcher
LaunchedEffect(Unit) {
    webSocket.tickFlow.collect { tick ->
        chartState.append(
            dataSetIndex = 0,
            entry = Entry(x = tick.timestamp, y = tick.price),
        )
    }
}

// Use streaming state in chart
LineChart(state = chartState, ...)
```

### ChartState API

```kotlin
interface LineChartState {
    val data: LineData                // Current snapshot (read in draw phase)
    fun append(dataSetIndex: Int, entry: Entry)
    fun appendAll(dataSetIndex: Int, entries: List<Entry>)
    fun replaceData(data: LineData)    // Full swap (e.g., timeframe change)
    fun clear()
}

// Analogous: BarChartState, CandleChartState with their respective entry types

@Composable
fun rememberLineChartState(
    capacity: Int = Int.MAX_VALUE,     // Unbounded by default
    initialData: LineData = LineData(dataSets = emptyList()),
): LineChartState
```

### Internal Batching Mechanism

`ChartState` implementations use a `Channel(Channel.BUFFERED)` internally. A frame-synced coroutine collects all pending entries per vsync frame (via `withFrameMillis`) and applies them as a single atomic state swap. This ensures:

- Multiple `append()` calls within one frame → 1 draw invalidation
- Data state is backed by `mutableStateOf()` with a custom `SnapshotMutationPolicy` that always reports changed (since the internal ring buffer mutates in place, wrapped in a version counter)
- The `data` property is read inside draw lambdas → draw-phase-only invalidation, zero recomposition

### Threading Guarantees

- `append()` and `appendAll()` are thread-safe — can be called from any dispatcher (IO, Default, Main)
- The internal `Channel` provides the thread-safety bridge
- State mutation (ring buffer update + version bump) always happens on the Main dispatcher within `withFrameMillis`
- Draw-phase reads are inherently main-thread

### Chart Composable Signatures

Each chart accepts either static `data` or streaming `state` (mutually exclusive):

```kotlin
@Composable
fun LineChart(
    modifier: Modifier = Modifier,
    data: LineData? = null,              // Static mode
    state: LineChartState? = null,       // Streaming mode (mutually exclusive with data)
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
)

@Composable
fun BarChart(
    modifier: Modifier = Modifier,
    data: BarData? = null,
    state: BarChartState? = null,
    xAxis: (XAxisScope.() -> Unit)? = null,
    yAxis: (YAxisScope.() -> Unit)? = null,
    gestures: (GestureScope.() -> Unit)? = null,
    highlight: (HighlightScope.() -> Unit)? = null,
    marker: (@Composable (BarEntry) -> Unit)? = null,
    animation: (AnimationScope.() -> Unit)? = null,
    style: (ChartStyleScope.() -> Unit)? = null,
    legend: (LegendScope.() -> Unit)? = null,
    viewport: (ViewportScope.() -> Unit)? = null,
    onValueSelected: ((BarEntry) -> Unit)? = null,
)

@Composable
fun CandleChart(
    modifier: Modifier = Modifier,
    data: CandleData? = null,
    state: CandleChartState? = null,
    xAxis: (XAxisScope.() -> Unit)? = null,
    yAxis: (YAxisScope.() -> Unit)? = null,
    gestures: (GestureScope.() -> Unit)? = null,
    highlight: (HighlightScope.() -> Unit)? = null,
    marker: (@Composable (CandleEntry) -> Unit)? = null,
    animation: (AnimationScope.() -> Unit)? = null,
    style: (ChartStyleScope.() -> Unit)? = null,
    legend: (LegendScope.() -> Unit)? = null,
    viewport: (ViewportScope.() -> Unit)? = null,
    onValueSelected: ((CandleEntry) -> Unit)? = null,
)
```

**Note:** `marker` and `onValueSelected` are typed to their respective entry types (`Entry`, `BarEntry`, `CandleEntry`) for type safety.

## Rendering Architecture

Hybrid approach: Canvas-direct rendering with selective layer caching. All rendering uses Compose's `DrawScope` API — no Android graphics escape hatches.

### Composable Layout Structure

Each chart composable is a `Box` with two layers:

1. **Canvas layer** (`Modifier.drawBehind` on the Box) — draws Layers 1–3 and 5 (background, grid, data, overlay). Pure draw-phase, no composable content.
2. **Overlay composition layer** — contains the **marker composable** and **dynamic axis labels**. These are real composables positioned via `Modifier.offset` based on the current highlight pixel coordinates (communicated from draw phase via `MutableState<Offset?>`).

This separation is necessary because marker content (`@Composable` lambda) cannot be emitted inside a draw phase. The highlight pixel position is written to a `mutableStateOf<Offset?>` during the draw phase; the overlay composable reads it to position the marker. This causes a single recomposition of only the marker overlay when highlight changes — the chart canvas itself does not recompose.

```
Box(modifier) {
    // Layer: Canvas (draw phase only — no recomposition on data change)
    Modifier.drawBehind {
        drawBackground(...)       // Layer 1 — cached
        drawGridAndAxes(...)      // Layer 2 — cached
        drawData(...)             // Layer 3 — hot path
        drawHighlightLines(...)   // Layer 4 (crosshairs only)
        drawLegend(...)           // Layer 5 — cached
    }

    // Layer: Marker overlay (recomposes only on highlight change)
    highlightPosition?.let { pos ->
        MarkerPositioner(position = pos, edgeAvoidance = true) {
            marker?.invoke(highlightedEntry)
        }
        DynamicYAxisLabel(position = pos, ...)
        DynamicXAxisLabel(position = pos, ...)
    }
}
```

### Marker Positioning (Edge Avoidance)

The `MarkerPositioner` composable measures the marker content, then positions it:
- Default: centered above the highlighted point with a small vertical offset
- If the marker would clip the top edge: positions below the point instead
- If the marker would clip left/right edge: shifts horizontally to stay within bounds
- Position animates smoothly during scrubbing

### 5-Layer Draw Pipeline

Each layer has its own caching strategy to minimize redraw cost:

| Layer | Content | Cache Strategy | Redraws When |
|-------|---------|---------------|-------------|
| 1. Background | Fill color | `drawWithCache` | Size or theme change |
| 2. Grid & Axes | Grid lines, axis lines, tick marks, axis labels, limit lines | `drawWithCache` | Axis config or viewport change |
| 3. Data (HOT PATH) | Line paths, fill areas, bar rectangles, candle bodies/wicks | Per-frame with path caching | Every frame during streaming |
| 4. Highlights | Crosshair lines, scrub indicator circle | Draw on selection change | Highlight state change |
| 5. Overlay | Legend, description text | `drawWithCache` | Config change |

### Zero-Recomposition Data Updates

Data state is read inside draw lambdas (`Modifier.drawBehind`), not in the composable body. Data changes trigger draw invalidation only — never recomposition.

```kotlin
Modifier.drawBehind {
    val currentData = chartState.data  // snapshot read in draw phase
    drawLineData(currentData, transformer, animationPhase)
}
```

### Coordinate System

Three coordinate spaces with a `Transformer` object converting between them:

- **Data space:** `(x: Float, y: Float)` — the raw entry values
- **Viewport:** Visible window into data space (defines what range of data is on screen)
- **Pixel space:** `DrawScope` coordinates for actual rendering

Reverse transform (pixel → data) is used for touch/gesture hit testing.

### Path Caching

For line charts with append-only updates (common in streaming), the previously computed `Path` is reused and only the new segment is appended. Avoids rebuilding the entire path on every frame. The cache is invalidated when:
- Data is fully replaced (not appended)
- Viewport scale changes (zoom — future feature)
- Chart size changes

## Viewport Configuration

Controls the visible range of data and auto-scrolling behavior.

### Configuration

```kotlin
viewport = {
    // Visible range
    visibleXRange = 60f                  // Show 60 data points worth of X range
    // OR: visibleXRangeMinimum / visibleXRangeMaximum for bounds

    // Y-axis auto-scaling
    yAxisAutoScale = true                // Default: true — fits Y range to visible data
    yAxisMinimum = null                  // Override: fixed minimum (e.g., 0f for volume)
    yAxisMaximum = null                  // Override: fixed maximum

    // Auto-scroll (for streaming)
    autoScrollToEnd = true               // Default: false — scrolls to show latest data
    autoScrollAnimated = true            // Smooth scroll vs. instant jump

    // Programmatic control
    // (These are also available as methods on ChartState)
}
```

### Programmatic Viewport Control

Available on `ChartState` for streaming mode, or via a `ChartViewportState` for static mode:

```kotlin
val chartState = rememberLineChartState(capacity = 500)

// Scroll to show latest data
chartState.scrollToEnd(animated = true)

// Scroll to specific X position
chartState.scrollTo(x = 1000f, animated = true)

// Set visible range programmatically
chartState.setVisibleXRange(range = 120f)
```

### Auto-Scroll Behavior

When `autoScrollToEnd = true` and new data is appended:
- Chart automatically scrolls to show the latest entry at the right edge
- If the user has manually panned away (not at the end), auto-scroll pauses to avoid fighting user intent
- Auto-scroll resumes when the user scrolls back to the end, or on the next `replaceData()` call

## Gesture System

All gestures handled via Compose's `pointerInput` blocks. Each gesture is individually toggleable.

### Supported Gestures

| Gesture | Behavior | Default |
|---------|----------|---------|
| Tap to highlight | Tap selects nearest entry, shows marker | Enabled |
| Drag to pan | Horizontal drag shifts viewport | Enabled |
| Drag to scrub | Drag along chart selects nearest entry continuously | Enabled |
| Fling deceleration | Momentum after pan drag ends | Enabled |

### Pan vs Scrub Disambiguation

When both `dragToPan` and `dragToScrub` are enabled, the gesture system determines intent as follows:

- **Scrub threshold:** A configurable pixel radius (default: `48.dp`) around visible data points. If the initial touch-down falls within this threshold of any rendered data point (in pixel space), the drag enters scrub mode.
- **Dense data fallback:** When the dataset is dense enough that every pixel falls within the threshold (i.e., more data points than horizontal pixels), scrub mode always wins. This is the expected behavior for dense stock charts.
- **Sparse data:** When data points are widely spaced and the touch-down is far from any point, pan mode activates.
- **Override:** When `dragToPan = false`, all drags go to scrub mode regardless of touch position (no ambiguity). This is the recommended configuration for stock ticker charts.

The threshold is configurable:

```kotlin
gestures = {
    dragToPan = true
    dragToScrub = true
    scrubThreshold = 48.dp    // Default; set higher to favor scrub
}
```

### Configuration

```kotlin
gestures = {
    tapToHighlight = true
    dragToPan = false
    dragToScrub = true
    flingDeceleration = true  // only applies when dragToPan = true
    scrubThreshold = 48.dp   // pixel radius for scrub detection
}
```

## Highlight & Scrub System

When an entry is selected (via tap or scrub), the following visual elements appear:

### Scrub Indicator (on the data point)

- **LineChart:** Filled circle at the entry position on the line, with optional outer ring
- **BarChart:** Selected bar changes color or gets a border highlight
- **CandleChart:** Selected candle gets a border highlight or color shift
- Vertical crosshair line following the selected position

### Marker Popup (floating info box)

A `@Composable` lambda provided by the consumer. Rendered in the overlay composition layer (not the draw phase). Positioned near the highlighted point with smart edge-avoidance. Animates in/out with highlight.

```kotlin
marker = { entry ->
    // Any composable — full flexibility
    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF1A1A1A)) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("$${entry.y}", color = Color.White)
            Text(formatTime(entry.x), color = Color.Gray)
        }
    }
}
```

### Dynamic Axis Labels During Scrub

While scrubbing, labels appear on the Y-axis and X-axis at the exact position of the highlighted entry. These are composables in the overlay layer (not draw-phase), positioned based on the highlight pixel coordinates:

- **Y-axis label:** Small pill/badge on the Y-axis showing the exact value (e.g., price). Slides up/down smoothly as scrub moves.
- **X-axis label:** Small pill/badge on the X-axis showing the exact value (e.g., timestamp). Slides left/right as scrub moves.
- Both disappear when scrub ends (with highlight fade-out animation).
- Each has its own formatter, allowing different formatting from static axis labels (e.g., more decimal places for active price).

### Highlight Configuration

```kotlin
highlight = {
    circleRadius = 6.dp
    circleColor = Color.White
    circleStrokeWidth = 2.dp
    circleStrokeColor = Color(0xFF00C853)
    crosshairEnabled = true
    crosshairColor = Color(0xFF888888)
    crosshairWidth = 1.dp
    yAxisLabel = true
    yAxisLabelColor = Color.White
    yAxisLabelBackground = Color(0xFF00C853)
    yAxisLabelFormatter = { value -> "$${String.format("%.2f", value)}" }
    xAxisLabel = true
    xAxisLabelFormatter = { value -> formatTimestamp(value.toLong(), "HH:mm:ss") }
}
```

### Consumer Callback

```kotlin
onValueSelected: ((Entry) -> Unit)? = null       // LineChart
onValueSelected: ((BarEntry) -> Unit)? = null     // BarChart
onValueSelected: ((CandleEntry) -> Unit)? = null  // CandleChart
```

Each chart's callback is typed to its specific entry type.

## Animation System

Three types of animations, all state read in draw phase (zero recomposition):

### 1. Entry/Appear Animations

When the chart first displays or data is set for the first time. Uses `Animatable<Float>` phase (0 to 1):

- **LineChart:** Line draws left-to-right (phaseX) or bottom-to-top (phaseY)
- **BarChart:** Bars grow upward from baseline
- **CandleChart:** Candle bodies expand from center

### 2. Data Transition Animations

When data changes (new snapshot swapped in via `replaceData()` or static `data` parameter change):

- Each entry's Y value animates from old position to new position
- New entries fade in, removed entries fade out
- Configurable: use `ChartAnimation.None` for high-frequency real-time ticks, `ChartAnimation.Spring()` for timeframe switches
- **Streaming `append()` calls do not trigger transition animations** — they update instantly (frame-batched). Transitions only apply to full data swaps.

### 3. Highlight Animations

When an entry is selected:

- Selected point scales up with spring animation
- Crosshair lines fade in
- Marker popup animates in
- Dynamic axis labels slide to position

### ChartAnimation Types

```kotlin
sealed interface ChartAnimation {
    object None : ChartAnimation
    data class EaseInOut(val duration: Duration) : ChartAnimation
    data class EaseIn(val duration: Duration) : ChartAnimation
    data class EaseOut(val duration: Duration) : ChartAnimation
    data class Spring(
        val dampingRatio: Float = Spring.DampingRatioMediumBouncy,
        val stiffness: Float = Spring.StiffnessMedium,
    ) : ChartAnimation
}
```

**Note:** Duration uses Kotlin's `kotlin.time.Duration` (e.g., `1000.milliseconds` or `1.seconds`).

### Configuration

```kotlin
animation = {
    appear = ChartAnimation.EaseInOut(duration = 1.seconds)
    dataTransition = ChartAnimation.None         // instant for real-time streaming
    // or: dataTransition = ChartAnimation.Spring()  // for timeframe switches
}
```

## Chart Theme & Styling

Standalone theme system with three layers of specificity.

### Layer Hierarchy

1. **Library defaults** — Dark theme defaults (suitable for stock charts)
2. **ChartThemeProvider** — Shared styling via `CompositionLocal` across multiple charts
3. **Per-chart style lambda** — Overrides specific properties, inherits the rest

### ChartStyle

```kotlin
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
)
```

**Note:** All pixel-space dimensions use `Dp` consistently. Text sizes use `TextUnit` (`sp`).

### Usage

```kotlin
// Global theme
ChartThemeProvider(style = MyChartTheme) {
    LineChart(data = lineData, ...)
    BarChart(data = barData, ...)
}

// Per-chart override
LineChart(
    data = lineData,
    style = {
        backgroundColor = Color(0xFF0D1117)
        gridColor = Color(0xFF21262D)
    }
)
```

Per-dataset colors (`lineColor`, `barColor`, `increasingColor`, `decreasingColor`) remain on the DataSet, not in the theme. The theme handles chart chrome: background, grid, axes, highlights, legend.

## Axis Configuration

Stock-essential axes for the initial release: single Y-axis (price), X-axis (time).

### Supported Features

- Custom formatters via lambda (price formatting, timestamp formatting)
- Grid lines (toggleable)
- Limit lines (e.g., day open price — horizontal reference lines with label)
- Label count configuration
- Label positioning (top, bottom for X-axis)

### DSL Scope Classes

```kotlin
class XAxisScope {
    var position: AxisPosition = AxisPosition.Bottom
    var formatter: ((Float) -> String)? = null
    var gridEnabled: Boolean = true
    var drawLabels: Boolean = true
    var labelCount: Int = 5
    var axisLineEnabled: Boolean = true
}

class YAxisScope {
    var formatter: ((Float) -> String)? = null
    var gridEnabled: Boolean = true
    var drawLabels: Boolean = true
    var labelCount: Int = 5
    var axisLineEnabled: Boolean = true
    var limitLines: List<LimitLine> = emptyList()
}
```

### DSL Scope Definitions (All Scopes)

```kotlin
class GestureScope {
    var tapToHighlight: Boolean = true
    var dragToPan: Boolean = true
    var dragToScrub: Boolean = true
    var flingDeceleration: Boolean = true
    var scrubThreshold: Dp = 48.dp
}

class HighlightScope {
    var circleRadius: Dp = 6.dp
    var circleColor: Color = Color.White
    var circleStrokeWidth: Dp = 2.dp
    var circleStrokeColor: Color = Color.Unspecified
    var crosshairEnabled: Boolean = true
    var crosshairColor: Color = Color(0xFF888888)
    var crosshairWidth: Dp = 1.dp
    var yAxisLabel: Boolean = false
    var yAxisLabelColor: Color = Color.White
    var yAxisLabelBackground: Color = Color.Unspecified
    var yAxisLabelFormatter: ((Float) -> String)? = null
    var xAxisLabel: Boolean = false
    var xAxisLabelFormatter: ((Float) -> String)? = null
}

class AnimationScope {
    var appear: ChartAnimation = ChartAnimation.EaseInOut(1.seconds)
    var dataTransition: ChartAnimation = ChartAnimation.None
}

class ChartStyleScope {
    var backgroundColor: Color = Color.Unspecified
    var gridColor: Color = Color.Unspecified
    var gridLineWidth: Dp = Dp.Unspecified
    var axisLineColor: Color = Color.Unspecified
    var axisLabelColor: Color = Color.Unspecified
    var axisLabelSize: TextUnit = TextUnit.Unspecified
    var highlightLineColor: Color = Color.Unspecified
    var highlightLineWidth: Dp = Dp.Unspecified
    // Unspecified values fall back to ChartThemeProvider or library defaults
}

class ViewportScope {
    var visibleXRange: Float? = null            // Number of X units visible; null = show all
    var visibleXRangeMinimum: Float? = null     // Min visible range (limits zoom-out)
    var visibleXRangeMaximum: Float? = null     // Max visible range (limits zoom-in)
    var yAxisAutoScale: Boolean = true          // Auto-fit Y range to visible data
    var yAxisMinimum: Float? = null             // Fixed Y minimum override
    var yAxisMaximum: Float? = null             // Fixed Y maximum override
    var autoScrollToEnd: Boolean = false        // Auto-scroll on new data append
    var autoScrollAnimated: Boolean = true      // Smooth vs instant auto-scroll
}
```

### Configuration

```kotlin
xAxis = {
    position = AxisPosition.Bottom
    formatter = { value -> formatTimestamp(value.toLong()) }
    gridEnabled = true
    labelCount = 5
    drawLabels = true
}

yAxis = {
    formatter = { value -> "$${String.format("%.2f", value)}" }
    gridEnabled = true
    limitLines = listOf(
        LimitLine(value = dayOpenPrice, label = "Open", color = Color.Yellow)
    )
}
```

## Legend Configuration

```kotlin
class LegendScope {
    var enabled: Boolean = true
    var position: LegendPosition = LegendPosition.Top
    var orientation: LegendOrientation = LegendOrientation.Horizontal
    var formSize: Dp = 10.dp
    var textColor: Color = Color.Unspecified  // Falls back to ChartStyle.legendTextColor
    var textSize: TextUnit = TextUnit.Unspecified  // Falls back to ChartStyle.legendTextSize
}

enum class LegendPosition { Top, Bottom, Left, Right }
enum class LegendOrientation { Horizontal, Vertical }
```

## Public API — Full Examples

### LineChart (Stock Price)

```kotlin
LineChart(
    modifier = Modifier.fillMaxSize(),
    state = priceChartState,  // streaming mode
    xAxis = {
        position = AxisPosition.Bottom
        formatter = { value -> formatTimestamp(value.toLong()) }
        gridEnabled = true
        labelCount = 5
    },
    yAxis = {
        formatter = { value -> "$${String.format("%.2f", value)}" }
        gridEnabled = true
        limitLines = listOf(
            LimitLine(value = dayOpenPrice, label = "Open", color = Color.Yellow)
        )
    },
    gestures = {
        tapToHighlight = true
        dragToPan = false
        dragToScrub = true
    },
    highlight = {
        circleRadius = 6.dp
        circleColor = Color.White
        crosshairEnabled = true
        yAxisLabel = true
        yAxisLabelFormatter = { value -> "$${String.format("%.2f", value)}" }
        xAxisLabel = true
        xAxisLabelFormatter = { value -> formatTimestamp(value.toLong(), "HH:mm:ss") }
    },
    marker = { entry ->
        PriceMarker(price = entry.y, time = entry.x)
    },
    animation = {
        appear = ChartAnimation.EaseInOut(1.seconds)
        dataTransition = ChartAnimation.None
    },
    style = {
        backgroundColor = Color(0xFF0D1117)
        gridColor = Color(0xFF21262D)
    },
    viewport = {
        visibleXRange = 60f
        autoScrollToEnd = true
        yAxisAutoScale = true
    },
    legend = { enabled = false },
    onValueSelected = { entry -> },
)
```

### BarChart (Volume)

```kotlin
BarChart(
    modifier = Modifier.fillMaxSize().height(120.dp),
    state = volumeChartState,  // streaming mode
    xAxis = {
        position = AxisPosition.Bottom
        drawLabels = false
    },
    yAxis = {
        formatter = { value -> formatVolume(value) }
    },
    animation = {
        appear = ChartAnimation.EaseOut(800.milliseconds)
        dataTransition = ChartAnimation.None
    },
    viewport = {
        visibleXRange = 60f
        autoScrollToEnd = true
        yAxisAutoScale = true
        yAxisMinimum = 0f
    },
    onValueSelected = { entry -> },
)
```

### CandleChart (OHLC)

```kotlin
CandleChart(
    modifier = Modifier.fillMaxSize(),
    state = candleChartState,  // streaming mode
    xAxis = {
        position = AxisPosition.Bottom
        formatter = { value -> formatTimestamp(value.toLong()) }
    },
    yAxis = {
        formatter = { value -> "$${String.format("%.2f", value)}" }
        limitLines = listOf(
            LimitLine(value = dayOpenPrice, label = "Open", color = Color.Yellow)
        )
    },
    highlight = {
        crosshairEnabled = true
        yAxisLabel = true
        yAxisLabelFormatter = { value -> "$${String.format("%.4f", value)}" }
        xAxisLabel = true
        xAxisLabelFormatter = { value -> formatTimestamp(value.toLong(), "HH:mm:ss") }
    },
    marker = { entry ->
        CandleMarker(open = entry.open, high = entry.high, low = entry.low, close = entry.close)
    },
    gestures = {
        tapToHighlight = true
        dragToPan = true
        dragToScrub = true
    },
    animation = {
        appear = ChartAnimation.EaseInOut(1.seconds)
        dataTransition = ChartAnimation.None
    },
    viewport = {
        visibleXRange = 60f
        autoScrollToEnd = true
        yAxisAutoScale = true
    },
    onValueSelected = { entry -> },
)
```

## Testing Strategy

- Unit tests for data model (Entry, DataSet, ChartData operations, BarEntry stack sum derivation)
- Unit tests for Transformer (coordinate conversion accuracy, bidirectional)
- Unit tests for ring buffer (append, overflow, bounded size, thread safety)
- Unit tests for path caching (append-only detection, cache invalidation triggers)
- Unit tests for viewport (visible range, auto-scroll, Y-axis auto-scaling)
- Compose UI tests for gesture handling (tap, drag, scrub, pan/scrub disambiguation)
- Compose UI tests for rendering (screenshot tests for visual regression)
- Compose UI tests for marker positioning (edge avoidance, position tracking)
- Performance benchmarks for draw phase with 1K, 5K, 10K data points
- Streaming stress test: sustained 60fps under rapid `append()` calls

## Future Expansion (Not In Initial Scope)

- Pie chart, Radar chart, Scatter chart, Bubble chart
- CombinedChart (multiple chart types overlaid)
- Dual Y-axes (left + right)
- Pinch-to-zoom
- Label rotation
- Compose Multiplatform (KMP) support
- Data point selection via long-press
- Accessibility (contentDescription, TalkBack support)
