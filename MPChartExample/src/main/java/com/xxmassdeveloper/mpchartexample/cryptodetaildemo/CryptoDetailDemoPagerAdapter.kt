package com.xxmassdeveloper.mpchartexample.cryptodetaildemo

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

@Suppress("DEPRECATION")
internal class CryptoDetailDemoPagerAdapter(
    fm: FragmentManager,
    private val tabTitles: List<String>,
) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getCount(): Int = TAB_COUNT

    override fun getItem(position: Int): Fragment = when (position) {
        TAB_STREAM -> StreamStubFragment()
        else -> IgnoredTabFragment()
    }

    override fun getPageTitle(position: Int): CharSequence = tabTitles[position]

    companion object {
        const val TAB_STREAM = 0
        const val TAB_COUNT = 2
    }
}
