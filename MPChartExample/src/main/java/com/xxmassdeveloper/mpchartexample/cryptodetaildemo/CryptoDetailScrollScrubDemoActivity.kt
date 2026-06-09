package com.xxmassdeveloper.mpchartexample.cryptodetaildemo

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.tabs.TabLayout
import com.xxmassdeveloper.mpchartexample.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.viewpager.widget.ViewPager

/**
 * Mirrors [com.stockbit.cryptodetail.ui.detail.CryptoDetailFragment] wiring:
 * XML scaffold (CoordinatorLayout + AppBarLayout + ViewPager) with Compose islands.
 */
class CryptoDetailScrollScrubDemoActivity : AppCompatActivity() {

    private val isScrubbing = MutableStateFlow(false)
    private val chartPoints = generateDemoChartPoints()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crypto_scroll_scrub_demo)

        setupToolbar()
        setupHeader()
        setupGlow()
        setupBottomBar()
        setupViewPager()
        setupScrollListener()
        setupChartScrubLock()
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
                        isScrubbing.value = true
                    },
                    onScrubEnd = {
                        scrubbedPoint = null
                        isScrubbing.value = false
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

    private fun setupScrollListener() {
        val appBarLayout = findViewById<AppBarLayout>(R.id.abl_demo_detail) ?: return
        val glowView = findViewById<View>(R.id.cv_demo_glow) ?: return

        appBarLayout.addOnOffsetChangedListener(
            AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
                glowView.translationY = verticalOffset.toFloat()
            },
        )
    }

    private fun setupChartScrubLock() {
        val collapsingToolbar = findViewById<CollapsingToolbarLayout>(R.id.ctl_demo_header)
            ?: return
        val scrollFlags = (collapsingToolbar.layoutParams as AppBarLayout.LayoutParams).scrollFlags

        lifecycleScope.launch {
            isScrubbing
                .map { scrubbing -> scrubbing }
                .collect { scrubbing ->
                    val lp = collapsingToolbar.layoutParams as AppBarLayout.LayoutParams
                    lp.scrollFlags = if (scrubbing) {
                        AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL
                    } else {
                        scrollFlags
                    }
                    collapsingToolbar.layoutParams = lp
                }
        }
    }
}
