package com.ezt.priv.shortvideodownloader.ui.more.guidance

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class GuidanceAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int {
        return GuidanceActivity.Companion.numberPage
    }

    override fun createFragment(position: Int): Fragment {
        return GuidanceFragmentNew.newInstance(position)
    }

}
