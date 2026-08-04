package com.xxmassdeveloper.mpchartexample.cryptodetaildemo

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import com.xxmassdeveloper.mpchartexample.R
import androidx.viewpager.widget.ViewPager

/**
 * Mirrors [com.stockbit.cryptodetail.ui.detail.CryptoDetailFragment] wiring:
 * XML scaffold (CoordinatorLayout + AppBarLayout + ViewPager) with Compose islands.
 */
class CryptoDetailScrollScrubDemoActivity : AppCompatActivity() {

    private val chartPoints = generateDemoChartPoints()

    private var appBarLayout: AppBarLayout? = null
    private var appBarBehavior: AppBarLayout.Behavior? = null
    private var latestAppBarOffset = 0
    private var lockedAppBarOffset = 0
    private var isScrubLockActive = false

    private val appBarDragCallback = object : AppBarLayout.Behavior.DragCallback() {
        override fun canDrag(appBarLayout: AppBarLayout): Boolean = !isScrubLockActive
    }

    private val scrubOffsetGuard = AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
        if (!isScrubLockActive) {
            latestAppBarOffset = verticalOffset
            return@OnOffsetChangedListener
        }
        if (verticalOffset != lockedAppBarOffset) {
            appBarBehavior?.setTopAndBottomOffset(lockedAppBarOffset)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crypto_scroll_scrub_demo)

        appBarLayout = findViewById(R.id.abl_demo_detail)
        appBarBehavior = (appBarLayout?.layoutParams as? CoordinatorLayout.LayoutParams)
            ?.behavior as? AppBarLayout.Behavior
        appBarBehavior?.setDragCallback(appBarDragCallback)
        appBarLayout?.addOnOffsetChangedListener(scrubOffsetGuard)

        setupToolbar()
        setupHeader()
        setupGlow()
        setupBottomBar()
        setupViewPager()
        setupGlowScrollSync()
    }

    private fun setupToolbar() {
        findViewById<ComposeView>(R.id.cv_demo_toolbar)?.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CryptoDetailDemoToolbar(onBack = ::finish)
            }
        }
    }

    private fun setupHeader() {
        findViewById<ComposeView>(R.id.cv_demo_header)?.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                var scrubbedPoint by mutableStateOf<DemoChartPoint?>(null)

                CryptoDetailDemoHeaderContent(
                    chartPoints = chartPoints,
                    scrubbedPoint = scrubbedPoint,
                    onScrub = { point ->
                        scrubbedPoint = point
                    },
                    onScrubStart = ::beginChartScrubLock,
                    onScrubEnd = {
                        scrubbedPoint = null
                        endChartScrubLock()
                    },
                )
            }
        }
    }

    private fun setupGlow() {
        findViewById<ComposeView>(R.id.cv_demo_glow)?.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFF7931A).copy(alpha = 0.25f),
                                Color(0xFFF7931A).copy(alpha = 0.08f),
                                Color.Transparent,
                            ),
                            center = Offset(size.width * 0.5f, 0f),
                            radius = size.height * 0.75f,
                        ),
                    )
                }
            }
        }
    }

    private fun setupBottomBar() {
        findViewById<ComposeView>(R.id.cv_demo_bottom_bar)?.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CryptoDetailDemoBottomBar()
            }
        }
    }

    private fun setupViewPager() {
        val tabLayout = findViewById<TabLayout>(R.id.tl_demo_tabs) ?: return
        val viewPager = findViewById<ViewPager>(R.id.vp_demo_pager) ?: return

        val adapter = CryptoDetailDemoPagerAdapter(
            fm = supportFragmentManager,
            tabTitles = listOf("Stream", "Other"),
        )
        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = 1
        tabLayout.setupWithViewPager(viewPager)
    }

    private fun setupGlowScrollSync() {
        val glowView = findViewById<View>(R.id.cv_demo_glow) ?: return
        appBarLayout?.addOnOffsetChangedListener { _, verticalOffset ->
            glowView.translationY = verticalOffset.toFloat()
        }
    }

    /**
     * Pins the current AppBar collapse offset while scrubbing.
     *
     * Do not toggle [AppBarLayout.LayoutParams.scrollFlags] — that relayout expands the header.
     * Do not disable RecyclerView nested scrolling — that makes [ScrollingViewBehavior] expand the AppBar.
     */
    private fun beginChartScrubLock() {
        if (isScrubLockActive) return
        lockedAppBarOffset = latestAppBarOffset
        isScrubLockActive = true
        appBarBehavior?.setTopAndBottomOffset(lockedAppBarOffset)
        requestChartGestureOwnership(active = true)
        setStreamListScrollEnabled(enabled = false)
    }

    private fun endChartScrubLock() {
        if (!isScrubLockActive) return
        isScrubLockActive = false
        requestChartGestureOwnership(active = false)
        setStreamListScrollEnabled(enabled = true)
    }

    private fun requestChartGestureOwnership(active: Boolean) {
        var view: View? = findViewById(R.id.cv_demo_header)
        while (view != null) {
            (view.parent as? ViewGroup)?.requestDisallowInterceptTouchEvent(active)
            view = view.parent as? View
        }
    }

    private fun setStreamListScrollEnabled(enabled: Boolean) {
        supportFragmentManager.fragments
            .filterIsInstance<StreamStubFragment>()
            .forEach { fragment -> fragment.setListScrollEnabled(enabled) }
    }
}
