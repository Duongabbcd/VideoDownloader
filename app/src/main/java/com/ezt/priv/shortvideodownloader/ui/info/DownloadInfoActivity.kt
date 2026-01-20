package com.ezt.priv.shortvideodownloader.ui.info

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.edit
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.ads.RemoteConfig
import com.ezt.priv.shortvideodownloader.ads.type.BannerAds.BANNER_HOME
import com.ezt.priv.shortvideodownloader.ads.type.InterAds
import com.ezt.priv.shortvideodownloader.database.models.main.DownloadItem
import com.ezt.priv.shortvideodownloader.database.models.main.ResultItem
import com.ezt.priv.shortvideodownloader.database.viewmodel.CommandTemplateViewModel
import com.ezt.priv.shortvideodownloader.database.viewmodel.DownloadViewModel
import com.ezt.priv.shortvideodownloader.database.viewmodel.DownloadViewModel.Type
import com.ezt.priv.shortvideodownloader.database.viewmodel.HistoryViewModel
import com.ezt.priv.shortvideodownloader.database.viewmodel.ResultViewModel
import com.ezt.priv.shortvideodownloader.databinding.ActivityDownloadInfoBinding
import com.ezt.priv.shortvideodownloader.ui.BaseActivity
import com.ezt.priv.shortvideodownloader.ui.BaseActivity2
import com.ezt.priv.shortvideodownloader.ui.browse.BrowseActivity
import com.ezt.priv.shortvideodownloader.ui.connection.InternetConnectionViewModel
import com.ezt.priv.shortvideodownloader.ui.downloadcard.BackgroundToForegroundPageTransformer
import com.ezt.priv.shortvideodownloader.ui.downloadcard.DownloadAudioFragment
import com.ezt.priv.shortvideodownloader.ui.downloadcard.DownloadFragmentAdapter
import com.ezt.priv.shortvideodownloader.ui.downloadcard.DownloadVideoFragment
import com.ezt.priv.shortvideodownloader.ui.downloadcard.DownloadsAlreadyExistBaseDialog
import com.ezt.priv.shortvideodownloader.ui.home.HomeFragment
import com.ezt.priv.shortvideodownloader.ui.home.MainActivity
import com.ezt.priv.shortvideodownloader.ui.home.MainActivity.Companion.loadBanner
import com.ezt.priv.shortvideodownloader.ui.more.WebViewActivity
import com.ezt.priv.shortvideodownloader.util.Common
import com.ezt.priv.shortvideodownloader.util.Common.gone
import com.ezt.priv.shortvideodownloader.util.Common.visible
import com.ezt.priv.shortvideodownloader.util.UiUtil
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import kotlin.collections.isNotEmpty
import kotlin.collections.none
import kotlin.getValue

class DownloadInfoActivity :
    BaseActivity2<ActivityDownloadInfoBinding>(ActivityDownloadInfoBinding::inflate) {
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager2: ViewPager2
    private lateinit var fragmentAdapter: DownloadFragmentAdapter
    private lateinit var downloadViewModel: DownloadViewModel
    private lateinit var historyViewModel: HistoryViewModel
    private lateinit var resultViewModel: ResultViewModel
    private lateinit var behavior: BottomSheetBehavior<View>
    private lateinit var commandTemplateViewModel: CommandTemplateViewModel
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var updateItem: Button
    private lateinit var shimmerLoading: ShimmerFrameLayout
    private lateinit var title: View
    private lateinit var shimmerLoadingSubtitle: ShimmerFrameLayout
    private lateinit var subtitle: View
    private lateinit var parentActivity: BaseActivity
    private val connectionViewModel: InternetConnectionViewModel by viewModels()

    private lateinit var result: ResultItem
    private lateinit var type: Type
    private var ignoreDuplicates: Boolean = false
    private var disableUpdateData: Boolean = false
    private var currentDownloadItem: DownloadItem? = null
    private var incognito: Boolean = false

    private val isFromFB by lazy {
        intent.getBooleanExtra("isFromFB", false)
    }
    private val facebookURL by lazy {
        intent.getStringExtra("facebookURL") ?: ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val res: ResultItem?
        val dwl: DownloadItem?

        downloadViewModel = ViewModelProvider(this)[DownloadViewModel::class.java]
        historyViewModel = ViewModelProvider(this)[HistoryViewModel::class.java]
        resultViewModel = ViewModelProvider(this)[ResultViewModel::class.java]
        commandTemplateViewModel = ViewModelProvider(this)[CommandTemplateViewModel::class.java]
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)


        connectionViewModel.isConnectedLiveData.observe(this) { isConnected ->
//            checkInternetConnected(isConnected)
        }


        if (Build.VERSION.SDK_INT >= 33) {
            res = intent.getParcelableExtra("result", ResultItem::class.java)
            dwl = intent.getParcelableExtra("downloadItem", DownloadItem::class.java)
        } else {
            res = intent.getParcelableExtra<ResultItem>("result")
            dwl = intent.getParcelableExtra<DownloadItem>("downloadItem")
        }
        type = intent.getSerializableExtra("type") as Type ?: Type.audio
        disableUpdateData = intent.getBooleanExtra("disableUpdateData", false) == true
        ignoreDuplicates = intent.getBooleanExtra("ignore_duplicates", false) == true



        if (res == null) {
            return
        }
        println("DownloadBottomSheetDialog 1: $res")
        println("DownloadBottomSheetDialog 2: $dwl")
        println("DownloadBottomSheetDialog 3: ${HomeFragment.latestURL}")
        res.formats.onEach {
            println("DownloadBottomSheetDialog 4: $it")
        }



        result = res
        currentDownloadItem = dwl
        incognito =
            currentDownloadItem?.incognito ?: sharedPreferences.getBoolean("incognito", false)

    }

    private fun checkInternetConnected(isConnected: Boolean) {
        if (!isConnected) {
            binding.origin.gone()
            binding.noInternet.root.visible()
            binding.noInternet.tryAgain.setOnClickListener {
                val connected = connectionViewModel.isConnectedLiveData.value == true
                if (connected) {
                    binding.origin.visible()
                    binding.noInternet.root.visibility = View.VISIBLE
                    // Maybe reload your data
                } else {
                    Toast.makeText(
                        this,
                        R.string.no_connection,
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }
        } else {
            binding.origin.visible()
            binding.noInternet.root.gone()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(
           TAG,
            "Banner Conditions: ${RemoteConfig.BANNER_ALL_2} and ${RemoteConfig.ADS_DISABLE}"
        )
        if (RemoteConfig.BANNER_ALL_2 == "0" || RemoteConfig.ADS_DISABLE == "0") {
            binding.frBanner.root.gone()
        } else {
            loadBanner(this, BANNER_HOME)
        }

        binding.backIcon.setOnClickListener {
            InterAds.showPreloadInter(this, alias = InterAds.ALIAS_INTER_DOWNLOAD, {
                executeBackScreen()
            } , {
                executeBackScreen()
            })
        }

        tabLayout = findViewById(R.id.download_tablayout)
        viewPager2 = findViewById(R.id.download_viewpager)
        updateItem = findViewById(R.id.update_item)
        viewPager2.isUserInputEnabled =
            sharedPreferences.getBoolean("swipe_gestures_download_card", true)


        //loading shimmers
        shimmerLoading = findViewById(R.id.shimmer_loading_title)
        title = findViewById(R.id.bottom_sheet_title)
        shimmerLoadingSubtitle = findViewById(R.id.shimmer_loading_subtitle)
        subtitle = findViewById(R.id.bottom_sheet_subtitle)

        shimmerLoading.setOnClickListener {
            lifecycleScope.launch {
                resultViewModel.cancelUpdateItemData()
                (updateItem.parent as LinearLayout).visibility = View.VISIBLE
            }
        }


        (viewPager2.getChildAt(0) as? RecyclerView)?.apply {
            isNestedScrollingEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        var commandTemplateNr = 0
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                commandTemplateNr = commandTemplateViewModel.getTotalNumber()
                if (!Patterns.WEB_URL.matcher(result.url).matches()) commandTemplateNr++
                if (commandTemplateNr <= 0) {
                    (tabLayout.getChildAt(0) as? ViewGroup)?.getChildAt(2)?.isClickable = true
                    (tabLayout.getChildAt(0) as? ViewGroup)?.getChildAt(2)?.alpha = 0.3f
                }
            }
        }

        //check if the item has formats and its audio-only
        val formats = result.formats
        var isAudioOnly = formats.isNotEmpty() && formats.none { !it.format_note.contains("audio") }
        if (isAudioOnly) {
            (tabLayout.getChildAt(0) as? ViewGroup)?.getChildAt(1)?.isClickable = true
            (tabLayout.getChildAt(0) as? ViewGroup)?.getChildAt(1)?.alpha = 0.3f
        }

        //remove outdated player url of 1hr so it can refetch it in the cut player
        if (result.creationTime > System.currentTimeMillis() - 3600000) result.urls = ""
        val fragmentManager = supportFragmentManager
        fragmentAdapter = DownloadFragmentAdapter(
            fragmentManager,
            lifecycle,
            result,
            currentDownloadItem,
            nonSpecific = result.url.endsWith(".txt"),
            isIncognito = incognito
        )

        viewPager2.adapter = fragmentAdapter
        viewPager2.isSaveFromParentEnabled = false

        tabLayout.post {
            when (type) {
                Type.audio -> {
                    tabLayout.getTabAt(0)!!.select()
                    viewPager2.setCurrentItem(0, false)
                }

                Type.video -> {
                    if (isAudioOnly) {
                        tabLayout.getTabAt(0)!!.select()
                        viewPager2.setCurrentItem(0, false)
                        Toast.makeText(
                            this@DownloadInfoActivity,
                            getString(R.string.audio_only_item),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        tabLayout.getTabAt(1)!!.select()
                        viewPager2.setCurrentItem(1, false)
                    }
                }

                else -> {
                    tabLayout.getTabAt(2)!!.select()
                    viewPager2.postDelayed({
                        viewPager2.setCurrentItem(2, false)
                    }, 200)
                }
            }

            //check if the item is coming from a text file
            val isCommandOnly =
                (type == Type.command && !Patterns.WEB_URL.matcher(result.url).matches())
            if (isCommandOnly) {
                (tabLayout.getChildAt(0) as? ViewGroup)?.getChildAt(0)?.isClickable = false
                (tabLayout.getChildAt(0) as? ViewGroup)?.getChildAt(0)?.alpha = 0.3f

                (tabLayout.getChildAt(0) as? ViewGroup)?.getChildAt(1)?.isClickable = false
                (tabLayout.getChildAt(0) as? ViewGroup)?.getChildAt(1)?.alpha = 0.3f

                (updateItem.parent as LinearLayout).visibility = View.GONE
            }
        }

        sharedPreferences.edit(commit = true) {
            putString(
                "last_used_download_type",
                type.toString()
            )
        }


        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab!!.position == 2 && commandTemplateNr == 0) {
                    tabLayout.selectTab(tabLayout.getTabAt(1))
                    val s = Snackbar.make(
                        binding.root,
                        getString(R.string.add_template_first),
                        Snackbar.LENGTH_LONG
                    )
                    val snackbarView: View = s.view
                    val snackTextView =
                        snackbarView.findViewById<View>(R.id.snackbar_text) as TextView
                    snackTextView.maxLines = 9999999
                    s.setAction(R.string.new_template) {
                        UiUtil.showCommandTemplateCreationOrUpdatingSheet(
                            item = null,
                            context = this@DownloadInfoActivity,
                            lifeCycle = this@DownloadInfoActivity,
                            commandTemplateViewModel = commandTemplateViewModel,
                            newTemplate = {
                                commandTemplateNr = 1
                                (tabLayout.getChildAt(0) as? ViewGroup)?.getChildAt(2)?.isClickable =
                                    true
                                (tabLayout.getChildAt(0) as? ViewGroup)?.getChildAt(2)?.alpha = 1f
                                tabLayout.selectTab(tabLayout.getTabAt(2))
                            },
                            dismissed = {

                            }
                        )
                    }
                    s.show()
                } else if (tab.position == 1 && isAudioOnly) {
                    tabLayout.selectTab(tabLayout.getTabAt(0))
                    Toast.makeText(
                        this@DownloadInfoActivity,
                        getString(R.string.audio_only_item),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    viewPager2.setCurrentItem(tab.position, false)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })

        viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                tabLayout.selectTab(tabLayout.getTabAt(position))
                runCatching {
                    sharedPreferences.edit(commit = true) {
                        putString(
                            "last_used_download_type",
                            listOf(Type.audio, Type.video, Type.command)[position].toString()
                        )
                    }
                    fragmentAdapter.updateWhenSwitching(viewPager2.currentItem)
                }
            }
        })

        viewPager2.setPageTransformer(BackgroundToForegroundPageTransformer())

        val shownFields = sharedPreferences.getStringSet(
            "modify_download_card",
            this@DownloadInfoActivity.resources.getStringArray(R.array.modify_download_card_values)
                .toSet()
        )!!.toList()

        val scheduleBtn = findViewById<Button>(R.id.bottomsheet_schedule_button)
        scheduleBtn.visibility = if (shownFields.contains("schedule")) {
            View.GONE
        } else {
            View.GONE
        }
        val download = findViewById<Button>(R.id.bottomsheet_download_button)
        download!!.setOnClickListener {
            val isWifiOnly = Common.getAllowWifiDownloadOnly(this@DownloadInfoActivity)

            if(isWifiOnly && !isConnectedToWifi(this@DownloadInfoActivity)) {
                Toast.makeText(
                    this,
                    resources.getString(R.string.can_not_download),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            Toast.makeText(this, resources.getString(R.string.starts), Toast.LENGTH_SHORT).show()
            val clipboard = this@DownloadInfoActivity.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            try {
                clipboard.clearPrimaryClip()  // Preferred
            } catch (e: Exception) {
                // Fallback for OEMs that misbehave
                clipboard.setPrimaryClip(ClipData.newPlainText(null, null))
            }

            lifecycleScope.launch {
                resultViewModel.cancelUpdateItemData()
                resultViewModel.cancelUpdateFormatsItemData()
                scheduleBtn.isEnabled = false
                val item: DownloadItem = getDownloadItem()
                Toast.makeText(
                    this@DownloadInfoActivity,
                    resources.getString(R.string.high_note),
                    Toast.LENGTH_SHORT
                ).show()
                if (item.videoPreferences.alsoDownloadAsAudio) {
                    val itemsToQueue = mutableListOf<DownloadItem>()
                    itemsToQueue.add(item)

                    getAlsoAudioDownloadItem(finished = {
                        itemsToQueue.add(it)

                        lifecycleScope.launch {
                            val result = withContext(Dispatchers.IO) {
                                downloadViewModel.queueDownloads(itemsToQueue, ignoreDuplicates)
                            }
                            withContext(Dispatchers.Main) {
                                handleDuplicatesAndDismiss(result.duplicateDownloadIDs)
                            }
                        }
                    })
                } else {
                    val result = withContext(Dispatchers.IO) {
                        downloadViewModel.queueDownloads(listOf(item), ignoreDuplicates)
                    }
                    handleDuplicatesAndDismiss(result.duplicateDownloadIDs)
                }
            }
        }

        download.setOnLongClickListener {
            return@setOnLongClickListener false
//            val dd = MaterialAlertDialogBuilder(this@DownloadInfoActivity)
//            dd.setTitle(getString(R.string.save_for_later))
//            dd.setNegativeButton(getString(R.string.cancel)) { dialogInterface: DialogInterface, _: Int -> dialogInterface.cancel() }
//            dd.setPositiveButton(getString(R.string.ok)) { _: DialogInterface?, _: Int ->
//                lifecycleScope.launch(Dispatchers.IO) {
//                    downloadViewModel.putToSaved(getDownloadItem())
//                }
//            }
//            dd.show()
//            true
        }

        val link = findViewById<Button>(R.id.bottom_sheet_link)
        link.visibility = if (shownFields.contains("url")) {
            View.GONE
        } else {
            View.GONE
        }

        if (Patterns.WEB_URL.matcher(result.url).matches()) {
            link.text = result.url
            link.setOnClickListener {
                UiUtil.openLinkIntent(this@DownloadInfoActivity, result.url)
            }
            link.setOnLongClickListener {
                UiUtil.copyLinkToClipBoard(this@DownloadInfoActivity, result.url)
                true
            }

            //if auto-update after the card is open is off
            if (result.title.isEmpty() && currentDownloadItem == null && sharedPreferences.getBoolean(
                    "quick_download",
                    false
                )
            ) {
                (updateItem.parent as LinearLayout).visibility = View.VISIBLE
                updateItem.setOnClickListener {
                    (updateItem.parent as LinearLayout).visibility = View.GONE
                    initUpdateData()
                }
            } else {
                (updateItem.parent as LinearLayout).visibility = View.GONE
            }

        } else {
            link.visibility = View.GONE
            (updateItem.parent as LinearLayout).visibility = View.GONE
        }

        val incognitoBtn = findViewById<Button>(R.id.bottomsheet_incognito)
        incognitoBtn.alpha = if (incognito) 1f else 0.3f
        incognitoBtn.setOnClickListener {
            if (incognito) {
                it.alpha = 0.3f
            } else {
                it.alpha = 1f
            }

            incognito = !incognito
            fragmentAdapter.isIncognito = incognito
            val onOff = if (incognito) getString(R.string.ok) else getString(R.string.disabled)
            Snackbar.make(
                incognitoBtn,
                "${getString(R.string.incognito)}: $onOff",
                Snackbar.LENGTH_SHORT
            ).show()
        }


        //update in the background if there is no data
        if (!disableUpdateData) {
            if (result.title.isEmpty() && currentDownloadItem == null && !sharedPreferences.getBoolean(
                    "quick_download",
                    false
                ) && type != Type.command
            ) {
                initUpdateData()
            } else {
                val usingGenericFormatsOrEmpty =
                    result.formats.isEmpty() || result.formats.any { it.format_note.contains("ytdlnisgeneric") }
                if (usingGenericFormatsOrEmpty && sharedPreferences.getBoolean(
                        "update_formats",
                        false
                    ) && !sharedPreferences.getBoolean("quick_download", false)
                ) {
                    initUpdateFormats(result)
                }
            }
        }

        lifecycleScope.launch {
            resultViewModel.uiState.collectLatest { res ->
                if (res.errorMessage != null) {
                    kotlin.runCatching {
                        UiUtil.handleNoResults(this@DownloadInfoActivity, res.errorMessage!!,
                            url = result.url,
                            continueAnyway = true,
                            continued = {},
                            cookieFetch = {
                                val myIntent = Intent(this@DownloadInfoActivity, WebViewActivity::class.java)
                                myIntent.putExtra("url", "https://${URL(result.url).host}")
                                cookiesFetchedResultLauncher.launch(myIntent)
                            },
                            closed = {

                            }
                        )
                    }

                    resultViewModel.uiState.update { it.copy(errorMessage = null) }
                }
            }
        }

        lifecycleScope.launch {
            resultViewModel.updatingData.collectLatest {
                kotlin.runCatching {
                    if (it) {
                        title.visibility = View.GONE
                        subtitle.visibility = View.GONE
                        shimmerLoading.visibility = View.VISIBLE
                        shimmerLoadingSubtitle.visibility = View.VISIBLE
                        shimmerLoading.startShimmer()
                        shimmerLoadingSubtitle.startShimmer()
                        (updateItem.parent as LinearLayout).visibility = View.GONE
                    } else {
                        title.visibility = View.VISIBLE
                        subtitle.visibility = View.VISIBLE
                        shimmerLoading.visibility = View.GONE
                        shimmerLoadingSubtitle.visibility = View.GONE
                        shimmerLoading.stopShimmer()
                        shimmerLoadingSubtitle.stopShimmer()
                    }
                }
            }
        }

        lifecycleScope.launch {
            resultViewModel.updatingFormats.collectLatest {
                kotlin.runCatching {
                    if (it) {
                        delay(500)
                        runCatching {
                            (fragmentAdapter.fragments[0] as DownloadAudioFragment).apply {
                                view?.findViewById<LinearProgressIndicator>(R.id.format_loading_progress)
                                    ?.apply {
                                        isVisible = true
                                        isClickable = true
                                        setOnClickListener {
                                            lifecycleScope.launch {
                                                resultViewModel.cancelUpdateFormatsItemData()
                                            }
                                        }
                                    }
                            }
                        }
                        runCatching {
                            (fragmentAdapter.fragments[1] as DownloadVideoFragment).apply {
                                view?.findViewById<LinearProgressIndicator>(R.id.format_loading_progress)
                                    ?.apply {
                                        isVisible = true
                                        isClickable = true
                                        setOnClickListener {
                                            lifecycleScope.launch {
                                                resultViewModel.cancelUpdateFormatsItemData()
                                            }
                                        }
                                    }
                            }
                        }
                    } else {
                        runCatching {
                            (fragmentAdapter.fragments[0] as DownloadAudioFragment).apply {
                                view?.findViewById<LinearProgressIndicator>(R.id.format_loading_progress)
                                    ?.apply {
                                        isVisible = false
                                        isClickable = false
                                    }
                            }
                        }
                        runCatching {
                            (fragmentAdapter.fragments[1] as DownloadVideoFragment).apply {
                                view?.findViewById<LinearProgressIndicator>(R.id.format_loading_progress)
                                    ?.apply {
                                        isVisible = false
                                        isClickable = false
                                    }
                            }
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            resultViewModel.updateResultData.collectLatest { result ->
                if (result == null) return@collectLatest
                kotlin.runCatching {
                    lifecycleScope.launch(Dispatchers.Main) {
                        if (result.size == 1 && result[0] != null) {
                            val res = result[0]!!
                            fragmentAdapter.setResultItem(res)

                            title.visibility = View.VISIBLE
                            subtitle.visibility = View.VISIBLE
                            shimmerLoading.visibility = View.GONE
                            shimmerLoadingSubtitle.visibility = View.GONE
                            shimmerLoading.stopShimmer()
                            shimmerLoadingSubtitle.stopShimmer()

                            val usingGenericFormatsOrEmpty =
                                res.formats.isEmpty() || res.formats.any { it.format_note.contains("ytdlnisgeneric") }
                            intent.putExtra("result", res)
                            if (usingGenericFormatsOrEmpty && sharedPreferences.getBoolean(
                                    "update_formats",
                                    false
                                )
                            ) {
                                initUpdateFormats(res)
                            }

                        } else if (result.size > 1) {
                            //open multi download card instead
                           result.onEach { item ->
                               println("result: $item")
                           }
                        }

                        resultViewModel.updateResultData.emit(null)
                    }

                }
            }
        }

        lifecycleScope.launch {
            // Make sure this runs on the main thread
            downloadViewModel.alreadyExistsUiState.collectLatest { res ->
                if (res.isNotEmpty()) {
                    // Optional UX delay
                    delay(500)

                    val bundle = bundleOf("duplicates" to ArrayList(res))
                    val dialog = DownloadsAlreadyExistBaseDialog().apply {
                        arguments = bundle
                    }

                    dialog.show(supportFragmentManager, "downloadsExistDialog")

                }
            }
        }

        lifecycleScope.launch {
            resultViewModel.updateFormatsResultData.collectLatest { formats ->
                if (formats == null) return@collectLatest
                kotlin.runCatching {
                    isAudioOnly =
                        formats.isNotEmpty() && formats.none { !it.format_note.contains("audio") }
                    if (isAudioOnly) {
                        (tabLayout.getChildAt(0) as? ViewGroup)?.getChildAt(1)?.isClickable = true
                        (tabLayout.getChildAt(0) as? ViewGroup)?.getChildAt(1)?.alpha = 0.3f
                        Toast.makeText(
                            this@DownloadInfoActivity,
                            getString(R.string.audio_only_item),
                            Toast.LENGTH_SHORT
                        ).show()
                        tabLayout.getTabAt(0)!!.select()
                        viewPager2.setCurrentItem(0, false)
                    }

                    lifecycleScope.launch {
                        withContext(Dispatchers.Main) {
                            runCatching {
                                val f1 = fragmentAdapter.fragments[0] as DownloadAudioFragment
                                val resultItem =
                                    downloadViewModel.createResultItemFromDownload(f1.downloadItem)
                                resultItem.formats = formats
                                fragmentAdapter.setResultItem(resultItem)
                                f1.view?.findViewById<LinearProgressIndicator>(R.id.format_loading_progress)?.visibility =
                                    View.GONE
                            }
                            runCatching {
                                val f1 = fragmentAdapter.fragments[1] as DownloadVideoFragment
                                val resultItem =
                                    downloadViewModel.createResultItemFromDownload(f1.downloadItem)
                                resultItem.formats = formats
                                fragmentAdapter.setResultItem(resultItem)
                                f1.view?.findViewById<LinearProgressIndicator>(R.id.format_loading_progress)?.visibility =
                                    View.GONE
                            }
                        }

                        if (formats.isNotEmpty()) {
                            result.formats = formats
                        }
                        resultViewModel.updateFormatsResultData.emit(null)
                    }
                }
            }
        }
    }

    private fun isConnectedToWifi(context: Context): Boolean {
        val connectivityManager = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun executeBackScreen() {
        if(facebookURL.contains("BrowseActivity", true)) {
            println("DownloadBottomSheetDialog 0: $facebookURL")
            val intent = Intent(this@DownloadInfoActivity, BrowseActivity::class.java).apply {
                // Optional: clear back stack
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        } else {
//                val clipboard = this@DownloadInfoActivity.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
//                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            println("facebookURL: $facebookURL")
            CoroutineScope(Dispatchers.IO).launch {
                resultViewModel.deleteAll(false)
            }

            if (isFromFB) {
                finish()
            } else {
                startActivity(Intent(this@DownloadInfoActivity, MainActivity::class.java).apply {
                    putExtra("facebookURL", "")
                    putExtra("isDisabled", true)
                })
            }

        }
    }

    override fun onBackPressed() {
        executeBackScreen()
    }


    private fun initUpdateData() {
        kotlin.runCatching {
            if (result.url.isBlank()) {
                return
            }
            if (resultViewModel.updatingData.value) return

            lifecycleScope.launch(Dispatchers.IO) {
                resultViewModel.updateItemData(result)
            }
        }
    }

    private fun initUpdateFormats(res: ResultItem) {
        kotlin.runCatching {
            if (resultViewModel.updatingFormats.value) return
            CoroutineScope(SupervisorJob()).launch(Dispatchers.IO) {
                resultViewModel.updateFormatItemData(res)
            }
        }
    }

    private fun handleDuplicatesAndDismiss(res: List<DownloadViewModel.AlreadyExistsIDs>) {
        //do nothing
    }

    private var cookiesFetchedResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            sharedPreferences.edit().putBoolean("use_cookies", true).apply()
            updateItem.isVisible = true
            initUpdateData()
        }
    }

    private fun getDownloadItem(selectedTabPosition: Int = tabLayout.selectedTabPosition) : DownloadItem {
        return fragmentAdapter.getDownloadItem(selectedTabPosition)
    }

    private fun getAlsoAudioDownloadItem(finished: (it: DownloadItem) -> Unit) {
        try {
            val ff = fragmentAdapter.fragments[0] as DownloadAudioFragment
            getDownloadItem(1).videoPreferences.audioFormatIDs.apply {
                if (this.isNotEmpty()) {
                    ff.updateSelectedAudioFormat(this.first())
                }
            }
            finished(ff.downloadItem)
        }catch (e: Exception){
            val fragmentLifecycleCallback = object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
                    supportFragmentManager.unregisterFragmentLifecycleCallbacks(this)
                    if (f is DownloadAudioFragment) {
                        f.requireView().post {
                            getDownloadItem(1).videoPreferences.audioFormatIDs.firstOrNull()?.let {
                                f.updateSelectedAudioFormat(it)
                            }
                            finished(f.downloadItem)
                        }
                    }
                }
            }

            supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleCallback, true)
            viewPager2.setCurrentItem(0, true)
            e.printStackTrace()
        }
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val downloadItem = getDownloadItem()
        intent.putExtra("result", result)
        intent.putExtra("downloadItem", downloadItem)
        intent.putExtra("type", downloadItem.type)
    }


    companion object {
        private val TAG = DownloadInfoActivity::class.java.simpleName
    }
}