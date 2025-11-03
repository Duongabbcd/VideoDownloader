package com.ezt.priv.shortvideodownloader.ui.tab

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import com.ezt.priv.shortvideodownloader.ads.RemoteConfig
import com.ezt.priv.shortvideodownloader.ads.type.BannerAds.BANNER_HOME
import com.ezt.priv.shortvideodownloader.ads.type.InterAds
import com.ezt.priv.shortvideodownloader.databinding.ActivityTabBinding
import com.ezt.priv.shortvideodownloader.ui.BaseActivity2
import com.ezt.priv.shortvideodownloader.ui.browse.BrowseActivity
import com.ezt.priv.shortvideodownloader.ui.home.MainActivity
import com.ezt.priv.shortvideodownloader.ui.home.MainActivity.Companion.currentTabPosition
import com.ezt.priv.shortvideodownloader.ui.home.MainActivity.Companion.loadBanner
import com.ezt.priv.shortvideodownloader.ui.tab.adapter.OnEditTabListener
import com.ezt.priv.shortvideodownloader.ui.tab.adapter.TabAdapter
import com.ezt.priv.shortvideodownloader.ui.tab.viewmodel.TabViewModel
import com.ezt.priv.shortvideodownloader.util.Common.gone
import com.ezt.priv.shortvideodownloader.util.Common.visible

class TabActivity : BaseActivity2<ActivityTabBinding>(ActivityTabBinding::inflate),
    OnEditTabListener {
    private lateinit var tabAdapter: TabAdapter
    private lateinit var tabViewModel: TabViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tabAdapter = TabAdapter(this)
        tabViewModel = ViewModelProvider(this)[TabViewModel::class.java]
        InterAds.preloadInterAds(
            this@TabActivity,
            alias = InterAds.ALIAS_INTER_DOWNLOAD,
            adUnit = InterAds.INTER_AD1
        )

        binding.apply {
            backIcon.setOnClickListener {
                InterAds.showPreloadInter(this@TabActivity, alias = InterAds.ALIAS_INTER_DOWNLOAD, {
                    finish()
                } , {
                    finish()
                })
            }

            allTabs.adapter = tabAdapter
            tabViewModel.tabs.observe(this@TabActivity) { list ->
                println("onCreate: $list")
                if (list.isEmpty()) {
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
        Log.d(
            TAG, "Banner Conditions: ${RemoteConfig.BANNER_ALL_2} and ${RemoteConfig.ADS_DISABLE}"
        )
        if (RemoteConfig.BANNER_ALL_2 == "0" || RemoteConfig.ADS_DISABLE == "0") {
            binding.frBanner.root.gone()
        } else {
            loadBanner(this, BANNER_HOME)
        }

        tabViewModel.displayAllCurrentTabs()
    }

    override fun onClickListener(tab: String) {
        if (tab.contains("Home", true)) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            val intent = Intent(this, BrowseActivity::class.java)
            intent.putExtra("receivedURL", tab)
            startActivity(intent)
            finish()
        }
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
        private val TAG = TabActivity::class.java.simpleName
    }

}