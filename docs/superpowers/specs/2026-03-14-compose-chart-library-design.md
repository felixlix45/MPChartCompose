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

Immutable public API with smart internals (Option B). Consumers work with immutable data classes. Internally, bounded datasets use a ring buffer for WebSocket streaming to avoid list copying on every tick. Frame-batching via `Channel` + `withFrameMillis` ensures 1 state update per vsync.

### Entries

```kotlin
data class Entry(val x: Float, val y: Float, val data: Any? = null)

data class BarEntry(val x: Float, val y: Float, val stackValues: FloatArray? = null)

data class CandleEntry(
    val x: Float,
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float,
    val data: Any? = null,
)
```

### DataSets

```kotlin
data class LineDataSet(
    val entries: List<Entry>,
    val label: String,
    val lineColor: Color = Color.Unspecified,
    val lineWidth: Float = 2f,
    val drawCircles: Boolean = true,
    val circleRadius: Float = 4f,
    val mode: LineMode = LineMode.Linear,  // Linear, CubicBezier, Stepped
    val fillEnabled: Boolean = false,
    val fillColor: Color = Color.Unspecified,
)

data class BarDataSet(
    val entries: List<BarEntry>,
    val label: String,
    val barColor: Color = Color.Unspecified,
    val barWidth: Float = 0.85f,
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

### ChartData Containers

```kotlin
data class LineData(val dataSets: List<LineDataSet>)
data class BarData(val dataSets: List<BarDataSet>, val barWidth: Float = 0.85f)
data class CandleData(val dataSets: List<CandleDataSet>)
```

### Internal Ring Buffer

Not exposed publicly. Used internally when a bounded-size dataset receives append-only updates from a WebSocket stream. Avoids full list copies on every tick.

## Rendering Architecture

Hybrid approach: Canvas-direct rendering with selective layer caching. All rendering uses Compose's `DrawScope` API — no Android graphics escape hatches.

### 5-Layer Draw Pipeline

Each layer has its own caching strategy to minimize redraw cost:

| Layer | Content | Cache Strategy | Redraws When |
|-------|---------|---------------|-------------|
| 1. Background | Fill color | `drawWithCache` | Size or theme change |
| 2. Grid & Axes | Grid lines, axis lines, tick marks, axis labels, limit lines | `drawWithCache` | Axis config or viewport change |
| 3. Data (HOT PATH) | Line paths, fill areas, bar rectangles, candle bodies/wicks | Per-frame with path caching | Every frame during streaming |
| 4. Highlights & Markers | Crosshair lines, scrub indicator, dynamic axis labels, value popup | Draw on selection change | Highlight state change |
| 5. Overlay | Legend, description text | `drawWithCache` | Config change |

### Zero-Recomposition Data Updates

Data state is read inside draw lambdas (`Modifier.drawBehind`), not in the composable body. Data changes trigger draw invalidation only — never recomposition.

```kotlin
Modifier.drawBehind {
    val currentData = chartState.data  // snapshot read in draw phase
    drawLineData(currentData, transformer, animationPhase)
}
```

### WebSocket Data Flow

1. Tick arrives (any thread)
2. Buffered in a `Channel`
3. `withFrameMillis` collector batches ticks per vsync (16ms)
4. Single atomic state swap
5. Draw invalidation only (no recomposition)
6. Canvas reads new data in draw phase

### Coordinate System

Three coordinate spaces with a `Transformer` object converting between them:

- **Data space:** `(x: Float, y: Float)` — the raw entry values
- **Viewport:** Visible window into data space (defines what range of data is on screen)
- **Pixel space:** `DrawScope` coordinates for actual rendering

Reverse transform (pixel → data) is used for touch/gesture hit testing.

### Path Caching

For line charts with append-only updates (common in streaming), the previously computed `Path` is reused and only the new segment is appended. Avoids rebuilding the entire path on every frame.

## Gesture System

All gestures handled via Compose's `pointerInput` blocks. Each gesture is individually toggleable.

### Supported Gestures

| Gesture | Behavior | Default |
|---------|----------|---------|
| Tap to highlight | Tap selects nearest entry, shows marker | Enabled |
| Drag to pan | Horizontal drag shifts viewport | Enabled |
| Drag to scrub | Drag along chart selects nearest entry continuously | Enabled |
| Fling deceleration | Momentum after pan drag ends | Enabled |

### Pan vs Scrub Detection

- If `dragToPan` is disabled, all drags go to scrub mode (no ambiguity)
- If both are enabled, drag starting near a data point enters scrub mode; otherwise pan mode

### Configuration

```kotlin
gestures = {
    tapToHighlight = true
    dragToPan = false
    dragToScrub = true
    flingDeceleration = true  // only applies when dragToPan = true
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

A `@Composable` lambda provided by the consumer. Positioned near the highlighted point with smart edge-avoidance (flips side if it would clip). Animates in/out with highlight.

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

While scrubbing, labels appear on the Y-axis and X-axis at the exact position of the highlighted entry:

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
onValueSelected = { entry -> /* fires on tap and during scrub */ }
```

## Animation System

Three types of animations, all state read in draw phase (zero recomposition):

### 1. Entry/Appear Animations

When the chart first displays or data is set for the first time. Uses `Animatable<Float>` phase (0 to 1):

- **LineChart:** Line draws left-to-right (phaseX) or bottom-to-top (phaseY)
- **BarChart:** Bars grow upward from baseline
- **CandleChart:** Candle bodies expand from center

### 2. Data Transition Animations

When data changes (new snapshot swapped in):

- Each entry's Y value animates from old position to new position
- New entries fade in, removed entries fade out
- Configurable: use `ChartAnimation.None` for high-frequency real-time ticks, `ChartAnimation.Spring()` for timeframe switches

### 3. Highlight Animations

When an entry is selected:

- Selected point scales up with spring animation
- Crosshair lines fade in
- Marker popup animates in
- Dynamic axis labels slide to position

### Configuration

```kotlin
animation = {
    appear = ChartAnimation.EaseInOut(duration = 1000.ms)
    dataTransition = ChartAnimation.None       // instant for real-time streaming
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
val MyChartTheme = ChartStyle(
    backgroundColor = Color(0xFF1A1A1A),
    gridColor = Color(0xFF2A2A2A),
    gridLineWidth = 0.5f,
    axisLineColor = Color(0xFF444444),
    axisLabelColor = Color(0xFFAAAAAA),
    axisLabelSize = 12.sp,
    limitLineColor = Color(0xFFFFAA00),
    highlightLineColor = Color(0xFFFFFFFF),
    highlightLineWidth = 1f,
    legendTextColor = Color(0xFFCCCCCC),
)
```

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

## Public API — Full Examples

### LineChart (Stock Price)

```kotlin
LineChart(
    modifier = Modifier.fillMaxSize(),
    data = LineData(
        dataSets = listOf(
            LineDataSet(
                entries = priceEntries,
                label = "BTC/USD",
                lineColor = Color(0xFF00C853),
                lineWidth = 2f,
                mode = LineMode.CubicBezier,
                fillEnabled = true,
                fillColor = Color(0xFF00C853).copy(alpha = 0.1f),
                drawCircles = false,
            )
        )
    ),
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
        appear = ChartAnimation.EaseInOut(1000.ms)
        dataTransition = ChartAnimation.None
    },
    style = {
        backgroundColor = Color(0xFF0D1117)
        gridColor = Color(0xFF21262D)
    },
    legend = { enabled = false },
    onValueSelected = { entry -> },
)
```

### BarChart (Volume)

```kotlin
BarChart(
    modifier = Modifier.fillMaxSize().height(120.dp),
    data = BarData(
        dataSets = listOf(
            BarDataSet(
                entries = volumeEntries,
                label = "Volume",
                barColor = Color(0xFF2196F3),
            )
        )
    ),
    xAxis = {
        position = AxisPosition.Bottom
        drawLabels = false
    },
    yAxis = {
        formatter = { value -> formatVolume(value) }
    },
    animation = {
        appear = ChartAnimation.EaseOut(800.ms)
        dataTransition = ChartAnimation.None
    },
    onValueSelected = { entry -> },
)
```

### CandleChart (OHLC)

```kotlin
CandleChart(
    modifier = Modifier.fillMaxSize(),
    data = CandleData(
        dataSets = listOf(
            CandleDataSet(
                entries = candleEntries,
                label = "BTC/USD",
                increasingColor = Color(0xFF00C853),
                decreasingColor = Color(0xFFFF1744),
                shadowColor = Color(0xFF888888),
                bodyWidth = 0.7f,
            )
        )
    ),
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
        appear = ChartAnimation.EaseInOut(1000.ms)
        dataTransition = ChartAnimation.None
    },
    onValueSelected = { entry -> },
)
```

## Testing Strategy

- Unit tests for data model (Entry, DataSet, ChartData operations)
- Unit tests for Transformer (coordinate conversion accuracy)
- Unit tests for ring buffer (append, overflow, bounded size)
- Unit tests for path caching (append-only detection, cache invalidation)
- Compose UI tests for gesture handling (tap, drag, scrub)
- Compose UI tests for rendering (screenshot tests for visual regression)
- Performance benchmarks for draw phase with 1K, 5K, 10K data points

## Future Expansion (Not In Initial Scope)

- Pie chart, Radar chart, Scatter chart, Bubble chart
- CombinedChart (multiple chart types overlaid)
- Dual Y-axes (left + right)
- Pinch-to-zoom
- Label rotation
- Compose Multiplatform (KMP) support
- Data point selection via long-press
