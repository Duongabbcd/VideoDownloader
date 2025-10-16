package com.ezt.video.downloader.ui.downloads

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.work.WorkManager
import com.ezt.video.downloader.R
import com.ezt.video.downloader.database.viewmodel.DownloadViewModel
import com.ezt.video.downloader.databinding.FragmentDownloadQueueMainScreenBinding
import com.ezt.video.downloader.ui.BaseFragment
import com.ezt.video.downloader.ui.home.MainActivity
import com.ezt.video.downloader.util.Extensions.createBadge
import com.ezt.video.downloader.util.NavbarUtil
import com.ezt.video.downloader.util.NotificationUtil
import com.ezt.video.downloader.util.UiUtil
import com.google.android.material.appbar.MaterialToolbar
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
                        notificationUtil.cancelErroredNotification(this.toInt())
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
                            createBadge(it)
                        }
                    }
                }
                lifecycleScope.launch {
                    downloadViewModel.erroredDownloadsCount.collectLatest {
                        downloadTablayout.getTabAt(4)?.apply {
                            removeBadge()
                            if (it > 0) createBadge(it)
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