package com.ezt.video.downloader.ui.tab

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.ezt.video.downloader.database.viewmodel.ResultViewModel
import com.ezt.video.downloader.databinding.ActivityTabBinding
import com.ezt.video.downloader.ui.BaseActivity2
import com.ezt.video.downloader.ui.tab.adapter.OnEditTabListener
import com.ezt.video.downloader.ui.tab.adapter.TabAdapter
import com.ezt.video.downloader.ui.tab.viewmodel.TabViewModel
import com.ezt.video.downloader.util.Common.gone
import com.ezt.video.downloader.util.Common.visible
import org.schabi.newpipe.extractor.timeago.patterns.th

class TabActivity : BaseActivity2<ActivityTabBinding>(ActivityTabBinding::inflate), OnEditTabListener {
    private lateinit var tabAdapter: TabAdapter
    private lateinit var tabViewModel: TabViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tabAdapter = TabAdapter(this)
        tabViewModel = ViewModelProvider(this)[TabViewModel::class.java]
        binding.apply {
            backIcon.setOnClickListener {
                finish()
            }
            allTabs.adapter = tabAdapter
            tabViewModel.tabs.observe(this@TabActivity) { list ->
                println("onCreate: $list")
                if(list.isEmpty()) {
                    noResults.root.visible()
                    allTabs.gone()
                } else {
                    noResults.root.gone()
                    allTabs.visible()
                    tabAdapter.submitList(list)
                }

            }

            newTabBtn.setOnClickListener {
                val allTabs = TabViewModel.getAllTabs(this@TabActivity).toMutableList()
                allTabs.add("Home")

                println("newTabBtn: $allTabs")
                tabViewModel.editAllCurrentTabs(allTabs)
            }

        }
    }


    override fun onResume() {
        super.onResume()
        tabViewModel.displayAllCurrentTabs()
    }

    override fun onDeleteTabListener(position: Int) {
        val allTabs = TabViewModel.getAllTabs(this@TabActivity).toMutableList()
        allTabs.removeAt(position)
        tabViewModel.editAllCurrentTabs(allTabs)
    }

    override fun onEditTabListener(position: Int) {
        println("onEditTabListener = $position")
        currentTabPosition = position
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    companion object {
        var currentTabPosition = -1
    }

}