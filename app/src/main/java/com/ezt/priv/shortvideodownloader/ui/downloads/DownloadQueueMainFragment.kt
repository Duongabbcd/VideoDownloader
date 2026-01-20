package com.ezt.priv.shortvideodownloader.ui.downloads

import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.work.WorkManager
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.database.viewmodel.DownloadViewModel
import com.ezt.priv.shortvideodownloader.databinding.FragmentDownloadQueueMainScreenBinding
import com.ezt.priv.shortvideodownloader.ui.BaseFragment
import com.ezt.priv.shortvideodownloader.ui.home.MainActivity
import com.ezt.priv.shortvideodownloader.util.Extensions.createBadge
import com.ezt.priv.shortvideodownloader.util.NavbarUtil
import com.ezt.priv.shortvideodownloader.util.NotificationUtil
import com.ezt.priv.shortvideodownloader.util.UiUtil
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DownloadQueueMainFragment : BaseFragment<FragmentDownloadQueueMainScreenBinding>(FragmentDownloadQueueMainScreenBinding::inflate){
    private lateinit var downloadViewModel: DownloadViewModel
    private lateinit var workManager: WorkManager

    private lateinit var fragmentAdapter : DownloadListFragmentAdapter
    private lateinit var mainActivity: MainActivity
    private lateinit var notificationUtil: NotificationUtil
    private lateinit var sharedPreferences: SharedPreferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainActivity = activity as MainActivity
        notificationUtil = NotificationUtil(mainActivity)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        workManager = WorkManager.getInstance(requireContext())
        downloadViewModel = ViewModelProvider(requireActivity())[DownloadViewModel::class.java]

        binding.apply {
            val isInNavBar = NavbarUtil.getNavBarItems(requireActivity()).any { n -> n.itemId == R.id.downloadQueueMainFragment && n.isVisible }
            if (isInNavBar) {
                downloadsToolbar.navigationIcon = null
            }else{
                mainActivity.hideBottomNavigation()
            }
            downloadsToolbar.setNavigationOnClickListener {
                mainActivity.onBackPressedDispatcher.onBackPressed()
            }

            
            (downloadViewpager.getChildAt(0) as? RecyclerView)?.apply {
                isNestedScrollingEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER
            }

            val fragments = mutableListOf(
                ActiveDownloadsFragment(),
                QueuedDownloadsFragment(),
//            ScheduledDownloadsFragment(),
                CancelledDownloadsFragment(),
                ErroredDownloadsFragment(),
//            SavedDownloadsFragment()
            )

            fragmentAdapter = DownloadListFragmentAdapter(
                childFragmentManager,
                lifecycle,
                fragments
            )

            downloadViewpager.adapter = fragmentAdapter
            downloadViewpager.isSaveFromParentEnabled = false

            // Define a ColorStateList in code (optional, or you can load from resources)
            val tabTextColor = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_selected), // selected
                    intArrayOf(-android.R.attr.state_selected) // unselected
                ),
                intArrayOf(
                    Color.parseColor("#15AD72"), // selected color
                    Color.parseColor("#000000")  // unselected color
                )
            )

            TabLayoutMediator(downloadTablayout, downloadViewpager) { tab, position ->
                when (position) {
                    0 -> tab.text = getString(R.string.running)
                    1 -> tab.text = getString(R.string.in_queue)
//                2 -> tab.text = getString(R.string.scheduled)
                    2 -> tab.text = getString(R.string.cancelled)
                    3 -> tab.text = getString(R.string.errored)
//                5 -> tab.text = getString(R.string.saved)
                }
            }.attach()

            val textAppearanceResId = R.style.CustomTextStyleMedium16sp // your style
            // Loop through tabs to disable all-caps
            for (i in 0 until downloadTablayout.tabCount) {
                val tab = downloadTablayout.getTabAt(i)
                val tabTextView = (tab?.view?.getChildAt(1) as? TextView)
                tabTextView?.apply {
                    setTextAppearance(context, textAppearanceResId)
                    isAllCaps = false // disable automatic capitalization
                    setTextColor(tabTextColor) // apply selected/unselected colors
                }
            }

            downloadTablayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    downloadViewpager.setCurrentItem(tab!!.position, true)
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {
                }

                override fun onTabReselected(tab: TabLayout.Tab?) {
                }
            })

            downloadViewpager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    downloadTablayout.selectTab(downloadTablayout.getTabAt(position))
                    initMenu()
                }
            })
            initMenu()

            if (arguments?.getString("tab") != null){
                downloadTablayout.getTabAt(4)!!.select()
                downloadViewpager.postDelayed( {
                    downloadViewpager.setCurrentItem(4, false)
                    val reconfigureID = arguments?.getLong("reconfigure")
                    reconfigureID?.apply {
                        notificationUtil.cancelErrorNotification(this.toInt())
                        lifecycleScope.launch {
                            kotlin.runCatching {
                                val item = withContext(Dispatchers.IO){
                                    downloadViewModel.getItemByID(reconfigureID)
                                }
                                findNavController().navigate(R.id.downloadBottomSheetDialog, bundleOf(
                                    Pair("downloadItem", item),
                                    Pair("result", downloadViewModel.createResultItemFromDownload(item)),
                                    Pair("type", item.type)
                                )
                                )
                            }
                        }

                    }
                }, 200)
            }

            arguments?.clear()

            if (sharedPreferences.getBoolean("show_count_downloads", false)) {
                lifecycleScope.launch {
                    downloadViewModel.activeDownloadsCount.collectLatest {
                        downloadTablayout.getTabAt(0)?.apply {
                            createBadge(it)
                        }
                    }
                }
                lifecycleScope.launch {
                    downloadViewModel.queuedDownloadsCount.collectLatest {
                        downloadTablayout.getTabAt(1)?.apply {
                            createBadge(it)
                        }
                    }
                }
                lifecycleScope.launch {
                    downloadViewModel.scheduledDownloadsCount.collectLatest {
                        downloadTablayout.getTabAt(2)?.apply {
                            createBadge(it)
                        }
                    }
                }
                lifecycleScope.launch {
                    downloadViewModel.cancelledDownloadsCount.collectLatest {
                        downloadTablayout.getTabAt(3)?.apply {
//                            createBadge(it)
                        }
                    }
                }
                lifecycleScope.launch {
                    downloadViewModel.erroredDownloadsCount.collectLatest {
                        downloadTablayout.getTabAt(4)?.apply {
                            removeBadge()
//                            if (it > 0) createBadge(it)
                        }
                    }
                }
                lifecycleScope.launch {
                    downloadViewModel.savedDownloadsCount.collectLatest {
                        downloadTablayout.getTabAt(5)?.apply {
                            removeBadge()
                            if (it > 0) createBadge(it)
                        }
                    }
                }
            }
        }



    }

    private fun initMenu() {

        binding.downloadsToolbar.setOnMenuItemClickListener { m: MenuItem ->
            try{
                when(m.itemId){
                    R.id.clear_all -> {
                        UiUtil.showGenericDeleteAllDialog(requireContext()) {
                            downloadViewModel.deleteAll()
                        }
                    }
                    R.id.clear_queue -> {
                        UiUtil.showGenericDeleteAllDialog(requireContext()) {
                            downloadViewModel.cancelAllDownloads()
                        }
                    }
                }
            }catch (e: Exception){
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            }

            true
        }
    }

    fun scrollToActive(){
        binding.apply {
            downloadTablayout.getTabAt(0)!!.select()
            downloadViewpager.setCurrentItem(0, true)
        }

    }
}