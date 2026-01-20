package com.ezt.priv.shortvideodownloader.ui.home

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.OnClickListener
import android.view.View.OnTouchListener
import android.view.View.VISIBLE
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ezt.priv.shortvideodownloader.ui.adapter.HomeAdapter
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.ads.RemoteConfig
import com.ezt.priv.shortvideodownloader.ads.type.NativeAds
import com.ezt.priv.shortvideodownloader.database.models.expand.non_table.SearchSuggestionItem
import com.ezt.priv.shortvideodownloader.database.models.expand.non_table.SearchSuggestionType
import com.ezt.priv.shortvideodownloader.database.models.main.ResultItem
import com.ezt.priv.shortvideodownloader.database.viewmodel.DownloadViewModel
import com.ezt.priv.shortvideodownloader.database.viewmodel.HistoryViewModel
import com.ezt.priv.shortvideodownloader.database.viewmodel.ResultViewModel
import com.ezt.priv.shortvideodownloader.databinding.FragmentHomeBinding
import com.ezt.priv.shortvideodownloader.ui.BaseFragment
import com.ezt.priv.shortvideodownloader.ui.adapter.SearchSuggestionsAdapter
import com.ezt.priv.shortvideodownloader.ui.browse.BrowseActivity
import com.ezt.priv.shortvideodownloader.ui.home.adapter.BookmarkListAdapter
import com.ezt.priv.shortvideodownloader.ui.info.DownloadInfoActivity
import com.ezt.priv.shortvideodownloader.ui.language.LanguageActivity
import com.ezt.priv.shortvideodownloader.ui.more.WebViewActivity
import com.ezt.priv.shortvideodownloader.ui.more.guidance.GuidanceActivity
import com.ezt.priv.shortvideodownloader.ui.more.settings.SettingsActivity
import com.ezt.priv.shortvideodownloader.ui.social.FacebookInfoActivity
import com.ezt.priv.shortvideodownloader.ui.tab.TabActivity
import com.ezt.priv.shortvideodownloader.ui.tab.viewmodel.TabViewModel
import com.ezt.priv.shortvideodownloader.util.Common.gone
import com.ezt.priv.shortvideodownloader.util.Common.visible
import com.ezt.priv.shortvideodownloader.util.Extensions.enableFastScroll
import com.ezt.priv.shortvideodownloader.util.Extensions.isURL
import com.ezt.priv.shortvideodownloader.util.NotificationUtil
import com.ezt.priv.shortvideodownloader.util.ThemeUtil
import com.ezt.priv.shortvideodownloader.util.UiUtil
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.search.SearchView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.sequences.forEach

class HomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate), HomeAdapter.OnItemClickListener, SearchSuggestionsAdapter.OnItemClickListener, OnClickListener {
    private var inputQueries: MutableList<String>? = null
    private var homeAdapter: HomeAdapter? = null
    private var searchSuggestionsAdapter: SearchSuggestionsAdapter? = null
    private var queriesConstraint: ConstraintLayout? = null

    private var downloadSelectedFab: ExtendedFloatingActionButton? = null
    private var downloadAllFab: ExtendedFloatingActionButton? = null

    private var homeFabs: LinearLayout? = null
    private var notificationUtil: NotificationUtil? = null
    private var downloadQueue: ArrayList<ResultItem>? = null

    private lateinit var playlistNameFilterScrollView: HorizontalScrollView
    private lateinit var playlistNameFilterChipGroup: ChipGroup

    private lateinit var resultViewModel : ResultViewModel
    private lateinit var downloadViewModel : DownloadViewModel
    private lateinit var historyViewModel : HistoryViewModel
    private lateinit var tabViewModel: TabViewModel

    private var fragmentView: View? = null
    private var activity: Activity? = null
    private var mainActivity: MainActivity? = null
    private var fragmentContext: Context? = null
    private var layoutinflater: LayoutInflater? = null
//    private var shimmerCards: ShimmerFrameLayout? = null
    private var providersChipGroup: ChipGroup? = null
    private var chipGroupDivider: View? = null
    private var queriesChipGroup: ChipGroup? = null
    private var searchSuggestionsRecyclerView: RecyclerView? = null
    private var uiHandler: Handler? = null
    private var resultsList: List<ResultItem?>? = null
    private lateinit var selectedObjects: ArrayList<ResultItem>
    private var quickLaunchSheet = false
    private var sharedPreferences: SharedPreferences? = null
    private var actionMode: ActionMode? = null
    private var loadingItems: Boolean = false
    private var queryList = mutableListOf<String>()

    private var showDownloadAllFab: Boolean = false
    private var showClipboardFab: Boolean = false

    private lateinit var bookmarkListAdapter: BookmarkListAdapter

    private var isRight = false
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity = getActivity()
        mainActivity = activity as MainActivity?

        mainActivity?.let {
            binding.rlNative.gone()
            if(RemoteConfig.NATIVE_HOME == "0") {
                binding.rlNative.gone()
                return@let
            }
            LanguageActivity.showNative(it, NativeAds.ALIAS_NATIVE_HOME, binding.frNative, fullScreen = false,{
                binding.rlNative.visible()
                binding.mLoadingView.root.visibility = GONE
            }, {
                if (RemoteConfig.NATIVE_HOME == "0") {
                    binding.rlNative.visibility = GONE
                    return@showNative
                }
                NativeAds.preloadNativeAds(
                    it,
                    alias = NativeAds.ALIAS_NATIVE_FULLSCREEN,
                    adId = NativeAds.NATIVE_INTRO_FULLSCREEN
                )
                LanguageActivity.showNative(it, alias = NativeAds.ALIAS_NATIVE_HOME,
                    binding.frNative, fullScreen = false, {
                        binding.mLoadingView.root.visibility = View.GONE
                    }, {
                        Handler(Looper.getMainLooper()).postDelayed({
                            binding.rlNative.visibility = View.GONE
                        }, 1500) // Delay 1500 ms (1.5 seconds)
                    })
            } )

        }

        quickLaunchSheet = false
        notificationUtil = NotificationUtil(requireContext())
        selectedObjects = arrayListOf()
        
        fragmentContext = context
        layoutinflater = LayoutInflater.from(context)
        uiHandler = Handler(Looper.getMainLooper())
        selectedObjects = ArrayList()

        downloadViewModel = ViewModelProvider(requireActivity())[DownloadViewModel::class.java]
        historyViewModel = ViewModelProvider(this)[HistoryViewModel::class.java]
        tabViewModel = ViewModelProvider(this)[TabViewModel::class.java]

        downloadQueue = ArrayList()
        resultsList = mutableListOf()
        selectedObjects = ArrayList()

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        //initViews
        queriesChipGroup = view.findViewById(R.id.queries)
        queriesConstraint = view.findViewById(R.id.queries_constraint)
        homeFabs = view.findViewById(R.id.home_fabs)
        downloadSelectedFab = homeFabs!!.findViewById(R.id.download_selected_fab)
        downloadAllFab = homeFabs!!.findViewById(R.id.download_all_fab)
        playlistNameFilterScrollView = view.findViewById(R.id.playlist_selection_chips_scrollview)
        playlistNameFilterChipGroup = view.findViewById(R.id.playlist_selection_chips)

        runCatching { binding.homeToolbar.title = ThemeUtil.getStyledAppName(requireContext()) }

        homeAdapter =
            HomeAdapter(
                this,
                requireActivity()
            )
        bookmarkListAdapter = BookmarkListAdapter()
        binding.allBookmarks.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.allBookmarks.adapter = bookmarkListAdapter
        binding.allBookmarks.enableFastScroll()

//        shimmerCards = view.findViewById(R.id.shimmer_results_framelayout)
        bookmarkListAdapter.submitList(displayedAllowedList(), displayedAllowedList().size > 8)

        searchSuggestionsAdapter = SearchSuggestionsAdapter(
            this,
            requireActivity()
        )
        searchSuggestionsRecyclerView = view.findViewById(R.id.search_suggestions_recycler)
        searchSuggestionsRecyclerView?.layoutManager = LinearLayoutManager(context)
        searchSuggestionsRecyclerView?.adapter = searchSuggestionsAdapter
        searchSuggestionsRecyclerView?.itemAnimator = null

        val progressBar = view.findViewById<View>(R.id.progress)

        tabViewModel.tabs.observe(viewLifecycleOwner) {
            binding.tab.text = it.size.toString()
        }

        resultViewModel = ViewModelProvider(this)[ResultViewModel::class.java]
        resultViewModel.getFilteredList().observe(requireActivity()) { items ->
            kotlin.runCatching {
//                recentlySearch?.isVisible = it.isNotEmpty()
                items.onEach {
                    println("showSingleDownloadSheet 4: $it")
                }
                homeAdapter!!.submitList(items)
                resultsList = items
                progressBar.isVisible = loadingItems && resultsList!!.isNotEmpty()
                val result = checkClipboard()
                if(resultViewModel.repository.itemCount.value > 1 || resultViewModel.repository.itemCount.value == -1){
                    showDownloadAllFab =
                        items.size > 1 && items[0].playlistTitle.isNotEmpty() && !loadingItems && !result.isNullOrEmpty()
                                && binding.searchBar.text.isNotEmpty()

                    println("showDownloadAllFab: $showDownloadAllFab")
                    downloadAllFab!!.isVisible = showDownloadAllFab
                    binding.copiedUrlFab.isVisible = showDownloadAllFab
                }else if (resultViewModel.repository.itemCount.value == 1){
                    if (sharedPreferences!!.getBoolean("download_card", true)){
                        if(items.size == 1 && quickLaunchSheet && parentFragmentManager.findFragmentByTag("downloadSingleSheet") == null){

                            showSingleDownloadSheet(
                                items[0],
                                DownloadViewModel.Type.valueOf(sharedPreferences!!.getString("preferred_download_type", "video")!!)
                            )
                        }
                    }
                }else{
                    showDownloadAllFab = false
                    downloadAllFab!!.visibility = GONE
                }
                quickLaunchSheet = true
            }
        }

        resultViewModel.items.observe(requireActivity()) {
            updateMultiplePlaylistResults(it
                .filter { it2 -> it2.playlistTitle != "" && it2.playlistTitle != "YTDLNIS_SEARCH" }
                .map { it2 -> it2.playlistTitle }
                .distinct()
            )
        }

        initMenu()
        downloadSelectedFab?.tag = "downloadSelected"
        downloadSelectedFab?.setOnClickListener(this)
        downloadAllFab?.tag = "downloadAll"
        downloadAllFab?.setOnClickListener(this)

        if (arguments?.getString("url") != null){
            val url = requireArguments().getString("url")
            if (inputQueries == null) inputQueries = mutableListOf()
            binding.searchBar.setText(url)
            val argList = url!!.split("\n").filter { it.isURL() }.toMutableList()
            argList.removeAll(listOf("", null))
            inputQueries!!.addAll(argList)
        }

        if (inputQueries != null) {
            lifecycleScope.launch(Dispatchers.IO){
                resultViewModel.deleteAll()
                resultViewModel.parseQueries(inputQueries!!){}
                inputQueries = null
            }
        }
        binding.tab.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            startActivity(Intent(ctx, TabActivity::class.java))
        }

        binding.searchView.addTransitionListener { _, _, newState ->
            if (newState == SearchView.TransitionState.SHOWING){
                mainActivity?.hideBottomNavigation()
            }else if (newState == SearchView.TransitionState.HIDING){
                mainActivity?.showBottomNavigation()
            }
        }

        mainActivity?.onBackPressedDispatcher?.addCallback(this) {
            if (binding.searchView.isShowing == true) {
                binding.searchView.hide()
            }else{
                mainActivity?.finishAffinity()
            }
        }

        binding.guidanceBtn.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            startActivity(Intent(ctx, GuidanceActivity::class.java))
        }

        lifecycleScope.launch {
            launch{
                resultViewModel.uiState.collectLatest { res ->
                    if (res.errorMessage != null){
                        println("resultViewModel: ${res.errorMessage}")
                        val isSingleQueryAndURL = queryList.size == 1 && Patterns.WEB_URL.matcher(queryList.first()).matches()

                        kotlin.runCatching {
                            UiUtil.handleNoResults(requireActivity(), res.errorMessage!!,
                                url = if (isSingleQueryAndURL) queryList.first() else null,
                                continueAnyway = isSingleQueryAndURL,
                                continued = {
                                    lifecycleScope.launch {
                                        if (sharedPreferences!!.getBoolean("download_card", true)) {
                                            withContext(Dispatchers.Main){
                                                println("showSingleDownloadSheet 5")
                                                showSingleDownloadSheet(
                                                    resultItem = downloadViewModel.createEmptyResultItem(queryList.first()),
                                                    type = DownloadViewModel.Type.valueOf(sharedPreferences!!.getString("preferred_download_type", "video")!!),
                                                    disableUpdateData = true
                                                )
                                            }
                                        } else {
                                            val downloadItem = downloadViewModel.createDownloadItemFromResult(
                                                result = downloadViewModel.createEmptyResultItem(queryList.first()),
                                                givenType = DownloadViewModel.Type.valueOf(sharedPreferences!!.getString("preferred_download_type", "video")!!)
                                            )
                                            downloadViewModel.queueDownloads(listOf(downloadItem))
                                        }
                                    }
                                },
                                cookieFetch = {
                                    binding.searchBar.setText("")

                                    val myIntent = Intent(requireContext(), WebViewActivity::class.java)
                                    myIntent.putExtra("url", "https://${URL(queryList.first()).host}")
                                    cookiesFetchedResultLauncher.launch(myIntent)
                                },
                                closed = {}
                            )
                        }
                        resultViewModel.uiState.update {it.copy(errorMessage  = null) }
                    }

                    loadingItems = res.processing
                    progressBar.isVisible = loadingItems && resultsList!!.isNotEmpty()
                    if (res.processing){
//                        binding.recyclerViewHome.setPadding(0,0,0,0)
//                        shimmerCards!!.startShimmer()
//                        shimmerCards!!.visibility = VISIBLE
                    }else{
//                        binding.recyclerViewHome.setPadding(0,0,0,100)
//                        shimmerCards!!.stopShimmer()
//                        shimmerCards!!.visibility = GONE

                        showDownloadAllFab = resultsList!!.size > 1 && resultsList!![0]!!.playlistTitle.isNotEmpty()
                        downloadAllFab!!.isVisible = showDownloadAllFab
                    }
                }
            }
        }

        lifecycleScope.launch {
            launch{
                downloadViewModel.alreadyExistsUiState.collectLatest { res ->
                    if (res.isNotEmpty()){
                        withContext(Dispatchers.Main){
                            val bundle = bundleOf(
                                Pair("duplicates", ArrayList(res))
                            )
                            delay(500)
                            findNavController().navigate(R.id.downloadsAlreadyExistDialog, bundle)
                        }
                        downloadViewModel.alreadyExistsUiState.value = mutableListOf()
                    }
                }
            }
        }

        binding.settingIcon.setOnClickListener {
            val intent = Intent(context, SettingsActivity::class.java)
            startActivity(intent)
        }


    }

    private fun displayedAllowedList(): List<List<Bookmark>> {
        val combinedList = defaultList1 + defaultList2
        val filteredList = combinedList.filter { bookmark ->
            val host =
                Uri.parse(bookmark.url).host.orEmpty().removePrefix("www.") // e.g., "facebook.com"
            RemoteConfig.ALLOWED_LIST.any { allowed ->
                bookmark.name.equals(allowed, ignoreCase = true) || host.contains(
                    allowed,
                    ignoreCase = true
                )
            }
        }

        return listOf(filteredList)
    }

    private var cookiesFetchedResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            binding.loading.visible()
            sharedPreferences?.edit()?.putBoolean("use_cookies", true)?.apply()
            startSearch()
        }
    }

    override fun onResume() {
        super.onResume()
        tabViewModel.displayAllCurrentTabs()

        if(arguments?.getString("url") == null){
            if (!resultViewModel.uiState.value.processing){
//                resultViewModel.checkTrending()
                Log.d(TAG, "onResume: ${resultViewModel.uiState.value.processing}")
            }
        }else{
            arguments?.remove("url")
        }

        if (arguments?.getBoolean("showDownloadsWithUpdatedFormats") == true){
            notificationUtil?.cancelDownloadNotification(NotificationUtil.FORMAT_UPDATING_FINISHED_NOTIFICATION_ID)
            arguments?.remove("showDownloadsWithUpdatedFormats")
            CoroutineScope(Dispatchers.IO).launch {
                val ids = arguments?.getLongArray("downloadIds") ?: return@launch
                downloadViewModel.turnDownloadItemsToProcessingDownloads(ids.toList(), deleteExisting = true)
                withContext(Dispatchers.Main){
                    findNavController().navigate(R.id.downloadMultipleBottomSheetDialog2)
                }
            }
        }

        if(arguments?.getBoolean("search") == true){
            arguments?.remove("search")
            requireView().post {
                binding.searchBar.performClick()
            }
        }

        if (binding.searchView.currentTransitionState == SearchView.TransitionState.SHOWN){
            updateSearchViewItems(binding.searchView.editText.text.toString())
        }

        requireView().post {
            val clipboardList = checkClipboard()

            // Nothing in clipboard → continue normally
            if (clipboardList.isNullOrEmpty()) {
                resultViewModel.clearItems()
                return@post
            }

            latestURL = clipboardList.last()
            println("latestURL: $latestURL")

            if ((latestURL.contains("facebook", true) || latestURL.contains("instagram", true) || latestURL.contains("tiktok", true)) && !latestURL.contains("stories", true)) {
                val ctx = context ?: return@post

                val clipboard = ctx.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

                try {
                    clipboard.clearPrimaryClip()  // Preferred
                } catch (e: Exception) {
                    // Fallback for OEMs that misbehave
                    clipboard.setPrimaryClip(ClipData.newPlainText(null, null))
                }


                if (checkTheVideoURL(latestURL)) {
                    startActivity(Intent(ctx, FacebookInfoActivity::class.java).apply {
                        putExtra("facebookURL", latestURL)
                    })
                } else {
                    binding.copiedUrlFab.gone()
                    Toast.makeText(
                        ctx,
                        resources.getString(R.string.not_allowed),
                        Toast.LENGTH_SHORT
                    ).show()

                }
                return@post
            }

            binding.copiedUrlFab.visible()

            if(MainActivity.isSharedURL) {
                val ctx = context ?: return@post
                MainActivity.isSharedURL = false
                executeSearchingClipboard(ctx, clipboardList)
            }

            println("It is here")
            // Normal case: put URL into search bar
            binding.searchBar.setText(latestURL)
            showClipboardFab = true
            binding.copiedUrlFab.isVisible = true

            binding.copiedUrlFab.setOnClickListener {
                val ctx = context ?: return@setOnClickListener
                executeSearchingClipboard(ctx, clipboardList)
            }

            lifecycleScope.launch {
                binding.copiedUrlFab.extend()
                delay(1500)
                binding.copiedUrlFab.shrink()
            }
        }

    }

    private fun executeSearchingClipboard(ctx: Context, clipboardList: List<String>) {
        println("executeSearchingClipboard: $clipboardList")
        if (clipboardList.size == 1) {
            val downloadedURL = clipboardList.first()
            val check = checkTheVideoURL(downloadedURL)
            println("executeSearchingClipboard: $check")
            binding.copiedUrlFab.isVisible = false
            if (check) {
                binding.searchView.setText(downloadedURL)
                showClipboardFab = false
                initSearch(binding.searchView)
            } else {
                Toast.makeText(ctx, ctx.getString(R.string.not_allowed), Toast.LENGTH_SHORT).show()
                val clipboard = ctx.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                try {
                    clipboard.clearPrimaryClip()  // Preferred
                } catch (e: Exception) {
                    // Fallback for OEMs that misbehave
                    clipboard.setPrimaryClip(ClipData.newPlainText(null, null))
                }
                binding.searchBar.setText("")
            }

        } else {
            binding.searchBar.performClick()
            binding.searchBar.performClick()
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    delay(500)
                }
                clipboardList.forEach { onSearchSuggestionAdd(it) }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initMenu() {
        val queriesConstraint = requireView().findViewById<ConstraintLayout>(R.id.queries_constraint)
        val queriesInitStartBtn = queriesConstraint.findViewById<MaterialButton>(R.id.init_search_query)
        val isRightToLeft = resources.getBoolean(R.bool.is_right_to_left)

        providersChipGroup = binding.searchView.findViewById(R.id.providers)
        val providers = resources.getStringArray(R.array.search_engines)
        val providersValues = resources.getStringArray(R.array.search_engines_values).toMutableList()

        for(i in providersValues.indices){
            val provider = providers[i]
            val providerValue = providersValues[i]
            val tmp = layoutinflater!!.inflate(R.layout.filter_chip, providersChipGroup, false) as Chip
            tmp.text = provider
            tmp.id = i
            tmp.tag = providersValues[i]

            tmp.setOnClickListener {
                val editor = sharedPreferences?.edit()
                editor?.putString("search_engine", providerValue)
                editor?.apply()

            }

//            providersChipGroup?.addView(tmp)
        }

        chipGroupDivider = requireView().findViewById(R.id.chipGroupDivider)

        binding.searchView.addTransitionListener { _, _, newState ->
            if (newState == SearchView.TransitionState.SHOWN) {
                val currentProvider = sharedPreferences?.getString("search_engine", "ytsearch")
                providersChipGroup?.children?.forEach {
                    val tmp = providersChipGroup?.findViewById<Chip>(it.id)
                    if (tmp?.tag?.equals(currentProvider) == true) {
                        tmp.isChecked = true
                        return@forEach
                    }
                }

                if (Patterns.WEB_URL.matcher(binding.searchBar.text.toString()).matches() && binding.searchBar.text.isNotBlank()){
                    providersChipGroup?.visibility = GONE
                    chipGroupDivider?.visibility = GONE
                }else{
                    providersChipGroup?.visibility = GONE
                    chipGroupDivider?.visibility = GONE
                }

                updateSearchViewItems(binding.searchView.editText.text.toString())
            }
        }

        binding.searchView.editText.doAfterTextChanged {
            println("doAfterTextChanged")
            if (binding.searchView.currentTransitionState != SearchView.TransitionState.SHOWN) return@doAfterTextChanged
            updateSearchViewItems(it.toString())
        }

        binding.searchView.editText.setOnTouchListener(OnTouchListener { _, event ->
            try{
                val drawableLeft = 0
                val drawableRight = 2
                if (event.action == MotionEvent.ACTION_UP) {
                    if (
                        (isRightToLeft && (event.x < (binding.searchView.editText.left - binding.searchView.editText.compoundDrawables[drawableLeft].bounds.width()))) ||
                        (!isRightToLeft && (event.x > (binding.searchView.editText.right - binding.searchView.editText.compoundDrawables[drawableRight].bounds.width())))
                    ){

                        val present = queriesChipGroup!!.children.firstOrNull { (it as Chip).text.toString() == binding.searchView.editText.text.toString() }
                        if (present == null) {
                            val chip = layoutinflater!!.inflate(R.layout.input_chip, queriesChipGroup, false) as Chip
                            chip.text = binding.searchView.editText.text
                            chip.chipBackgroundColor = ColorStateList.valueOf(MaterialColors.getColor(requireContext(), R.attr.colorSecondaryContainer, Color.BLACK))
                            chip.setOnClickListener {
                                queriesChipGroup!!.removeView(chip)
                                if (queriesChipGroup!!.childCount == 0) queriesConstraint.visibility = GONE
                            }
                            queriesChipGroup!!.addView(chip)
                        }
                        if (queriesChipGroup!!.childCount == 0) queriesConstraint.visibility = GONE
                        else queriesConstraint.visibility = VISIBLE
                        binding.searchView.editText.setText("")
                        return@OnTouchListener true

                    }
                }
            } catch (ignored: Exception) {
                ignored.printStackTrace()
            }
            false
        })

        binding.searchView.editText.setOnEditorActionListener { _, _, _ ->
            println("searchView 1")
            val ctx = context ?: return@setOnEditorActionListener false
            val text = binding.searchView.editText.text.toString()
            if(text.contains("youtube",true ) || text.contains("youtu.be", true)) {
                binding.searchBar.setText("")
                Toast.makeText(ctx, resources.getString(R.string.youtube_desc), Toast.LENGTH_SHORT)
                    .show()
                return@setOnEditorActionListener false // Exit early: prevent further execution
            }

            if(text.contains("http", true)) {
                if (checkTheVideoURL(text)) {
                    initSearch(binding.searchView)
                } else {
                    Toast.makeText(ctx, ctx.getString(R.string.not_allowed), Toast.LENGTH_SHORT)
                        .show()
                }

            } else {
                val encodedKeyword = URLEncoder.encode(text, StandardCharsets.UTF_8.toString())
                val urlNew = "https://www.google.com/search?q=$encodedKeyword&tbm=vid"
                startActivity(Intent(requireContext(), BrowseActivity::class.java).apply {
                    putExtra("receivedURL", urlNew)
                })
            }

            true
        }

        binding.mainMenu.setOnClickListener {
            // Do the action that used to be inside the menu
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    resultViewModel.cancelParsingQueries()
                }
            }

            binding.searchBar.text = ""

        }

        queriesChipGroup!!.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (queriesChipGroup!!.childCount == 0) queriesConstraint.visibility = GONE
            else queriesConstraint.visibility = VISIBLE
        }

        queriesInitStartBtn.setOnClickListener {
            println("searchView 2")
            initSearch(binding.searchView)
        }
    }

    @SuppressLint("InflateParams")
    private fun updateSearchViewItems(searchQuery: String) = lifecycleScope.launch(Dispatchers.Main) {
        lifecycleScope.launch {
            if (binding.searchView.editText.text.isEmpty()){
                binding.searchView.editText.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
            }else{
//                binding.searchView.editText.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_plus, 0)
                binding.searchView.editText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    0,
                    0,
                    0,
                    0
                )
            }

            val combinedList = mutableListOf<SearchSuggestionItem>()

            val history = withContext(Dispatchers.IO){
                resultViewModel.getSearchHistory().map { it.query }.filter { it.contains(searchQuery, ignoreCase = true) }
            }.map {
                SearchSuggestionItem(it, SearchSuggestionType.HISTORY)
            }
            val suggestions = if (!sharedPreferences!!.getBoolean("search_suggestions", false)){
                withContext(Dispatchers.IO){
                    resultViewModel.getSearchSuggestions(searchQuery)
                }
            }else{
                emptyList()
            }.map {
                SearchSuggestionItem(it, SearchSuggestionType.SUGGESTION)
            }

            combinedList.addAll(history)
            combinedList.addAll(suggestions)

            val url = checkClipboard()
            url?.apply {
                val alreadyHasThem =
                    this.all { queriesChipGroup?.children?.any { c -> (c as Chip).text.contains(it) } == true }
                if (this.isNotEmpty() && !alreadyHasThem){
                    combinedList.add(0, SearchSuggestionItem(this.joinToString("\n"), SearchSuggestionType.CLIPBOARD))
                }
            }
            val input = combinedList.filter {
                !it.text.contains("youtube", true) && !it.text.contains(
                    "youtu.be",
                    true
                ) || !it.text
                    .contains("www", true)
            }

            input.onEach {
                println("input 2: $it")
            }
            searchSuggestionsAdapter?.submitList(input)

            if (Patterns.WEB_URL.matcher(binding.searchView.editText.text).matches()){
                providersChipGroup?.visibility = GONE
                chipGroupDivider?.visibility = GONE
            }else{
                providersChipGroup?.visibility = VISIBLE
                chipGroupDivider?.visibility = VISIBLE
            }
        }

    }

    private fun initSearch(searchView: SearchView){
        binding.loading.visible()
        binding.root.isEnabled =false

        queryList = mutableListOf()
        if (queriesChipGroup!!.childCount > 0){
            queriesChipGroup!!.children.forEach {
                val query = (it as Chip).text.toString().trim {it2 -> it2 <= ' '}
                if (query.isNotEmpty()){
                    queryList.add(query)
                }
            }
            queriesChipGroup!!.removeAllViews()
        }
        val searchText = searchView.editText.text.toString()
        if (searchView.editText.text.isNotBlank()) {
            queryList.add(searchText)
        }

        if (queryList.isEmpty()) return
        if (queryList.size == 1){
            binding.searchBar.setText(searchView.text)
        }

        searchView.hide()
        val checkIcognito = sharedPreferences!!.getBoolean("incognito", false)
        println("checkIcognito: $checkIcognito")
        if(!checkIcognito){
            queryList.forEach { q ->
                resultViewModel.addSearchQueryToHistory(q)
            }
        }
        startSearch()
    }

    private fun startSearch() {
        lifecycleScope.launch(Dispatchers.IO){
            resultViewModel.deleteAll()

            val check1 = sharedPreferences!!.getBoolean("quick_download", false)
            val check2 = sharedPreferences!!.getString("preferred_download_type", "video") == "command"
            println("startSearch: $check1 and $check2")
            if(check1|| check2){
                if (queryList.size == 1 && Patterns.WEB_URL.matcher(queryList.first()).matches()){
                    val check = sharedPreferences!!.getBoolean("download_card", true)
                    if (check) {
                        withContext(Dispatchers.Main){
                            println("showSingleDownloadSheet 6")
                            showSingleDownloadSheet(
                                resultItem = downloadViewModel.createEmptyResultItem(queryList.first()),
                                type = DownloadViewModel.Type.valueOf(sharedPreferences!!.getString("preferred_download_type", "video")!!)
                            )
                            binding.loading.gone()
                            binding.root.isEnabled = true
                        }
                    } else {
                        val downloadItem = downloadViewModel.createDownloadItemFromResult(
                            result = downloadViewModel.createEmptyResultItem(queryList.first()),
                            givenType = DownloadViewModel.Type.valueOf(sharedPreferences!!.getString("preferred_download_type", "video")!!)
                        )
                        downloadViewModel.queueDownloads(listOf(downloadItem))
                        withContext(Dispatchers.Main) {
                            binding.loading.gone()
                            binding.root.isEnabled = true
                        }
                    }

                }else{
                    resultViewModel.parseQueries(queryList){
                        lifecycleScope.launch {
                            withContext(Dispatchers.Main) {
                                binding.loading.gone()
                                binding.root.isEnabled = true
                            }
                        }
                    }

                }
            }else{
                resultViewModel.parseQueries(queryList){
                    lifecycleScope.launch {
                        withContext(Dispatchers.Main) {
                            binding.loading.gone()
                            binding.root.isEnabled = true
                        }
                    }
                }
            }
        }
    }

    fun scrollToTop() {
//        binding.recyclerViewHome.scrollToPosition(0)
        runCatching { (binding.searchBar.parent as AppBarLayout).setExpanded(true, true) }
    }

    @SuppressLint("ResourceType")
    override fun onButtonClick(videoURL: String, type: DownloadViewModel.Type?) {
        Log.e(TAG, type.toString() + " " + videoURL)
        val item = resultsList!!.find { it?.url == videoURL }
        Log.e(TAG, resultsList!![0].toString() + " " + videoURL)
//        binding.recyclerViewHome.findViewWithTag<MaterialButton>("""${item?.url}##$type""")
        if (sharedPreferences!!.getBoolean("download_card", true)) {
            println("showSingleDownloadSheet 3")
            showSingleDownloadSheet(item!!, type!!)
        } else {
            lifecycleScope.launch{
                val downloadItem = withContext(Dispatchers.IO){
                    downloadViewModel.createDownloadItemFromResult(
                        result = item!!,
                        givenType = type!!)
                }
                downloadViewModel.queueDownloads(listOf(downloadItem))
            }
        }
    }

    override fun onLongButtonClick(videoURL: String, type: DownloadViewModel.Type?) {
        Log.e(TAG, type.toString() + " " + videoURL)
        val item = resultsList!!.find { it?.url == videoURL }
        println("showSingleDownloadSheet 1")
        showSingleDownloadSheet(item!!, type!!)
    }

    @SuppressLint("RestrictedApi")
    private fun showSingleDownloadSheet(
        resultItem: ResultItem,
        type: DownloadViewModel.Type,
        disableUpdateData : Boolean = false
    ) {
        val intent = Intent(mainActivity, DownloadInfoActivity::class.java)
        intent.putExtra("isFromFB", false)
        intent.putExtra("result", resultItem)
        intent.putExtra("type", downloadViewModel.getDownloadType(type, resultItem.url))
        if (disableUpdateData) {
            intent.putExtra("disableUpdateData", true)
        }

        startActivity(intent)
    }

    override fun onCardClick(videoURL: String, add: Boolean) {
        lifecycleScope.launch {
            val item = resultsList?.find { it?.url == videoURL }
            if (actionMode == null) actionMode =
                (activity as AppCompatActivity?)!!.startSupportActionMode(contextualActionBar)
            actionMode?.apply {
                if (add) selectedObjects.add(item!!)
                else selectedObjects.remove(item!!)

                if (selectedObjects.isEmpty()) {
                    this.finish()
                }else{
                    actionMode?.title = "${selectedObjects.size} ${getString(R.string.selected)}"
                    this.menu.findItem(R.id.select_between).isVisible = false
                    if(selectedObjects.size == 2){
                        val selectedIDs = selectedObjects.sortedBy { it.id }
                        val resultsInMiddle = withContext(Dispatchers.IO){
                            resultViewModel.getResultsBetweenTwoItems(selectedIDs.first().id, selectedIDs.last().id)
                        }.toMutableList()
                        this.menu.findItem(R.id.select_between).isVisible = resultsInMiddle.isNotEmpty()
                    }
                }
            }
        }
    }

    override fun onCardDetailsClick(videoURL: String) {
        if (parentFragmentManager.findFragmentByTag("resultDetails") == null && resultsList != null && resultsList!!.isNotEmpty()){
            val bundle = Bundle()
            bundle.putParcelable("result", resultsList!!.first{it!!.url == videoURL}!!)
            findNavController().navigate(R.id.resultCardDetailsDialog, bundle)
        }
    }

    override fun onClick(v: View) {
        val viewIdName: String = try {
            v.tag.toString()
        } catch (e: Exception) {""}
        if (viewIdName.isNotEmpty()) {
            if (viewIdName == "downloadAll") {
                val showDownloadCard = sharedPreferences!!.getBoolean("download_card", true)
                downloadViewModel.turnResultItemsToProcessingDownloads(resultsList!!.map { it!!.id }, downloadNow = !showDownloadCard)
                if (showDownloadCard){
                    findNavController().navigate(R.id.downloadMultipleBottomSheetDialog2)
                }
            }
        }
    }

    private val contextualActionBar = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode!!.menuInflater.inflate(R.menu.main_menu_context, menu)
            mode.title = "${selectedObjects.size} ${getString(R.string.selected)}"
            binding.searchBar.isEnabled = false
            playlistNameFilterChipGroup.children.forEach { it.isEnabled = false }
            (activity as MainActivity).disableBottomNavigation()
            downloadAllFab!!.isVisible = false
            binding.copiedUrlFab.isVisible = false
            return true
        }

        override fun onPrepareActionMode(
            mode: ActionMode?,
            menu: Menu?
        ): Boolean {
            return false
        }

        override fun onActionItemClicked(
            mode: ActionMode?,
            item: MenuItem?
        ): Boolean {
            return when (item!!.itemId) {
                R.id.select_between -> {
                    lifecycleScope.launch {
                        val selectedIDs = selectedObjects.sortedBy { it.id }
                        val resultsInMiddle = withContext(Dispatchers.IO){
                            resultViewModel.getResultsBetweenTwoItems(selectedIDs.first().id, selectedIDs.last().id)
                        }.toMutableList()
                        if (resultsInMiddle.isNotEmpty()){
                            selectedObjects.addAll(resultsInMiddle)
                            homeAdapter?.checkMultipleItems(selectedObjects.map { it.url })
                            actionMode?.title = "${selectedObjects.count()} ${getString(R.string.selected)}"
                        }
                        mode?.menu?.findItem(R.id.select_between)?.isVisible = false
                    }
                    true
                }
                R.id.delete_results -> {
                    val deleteDialog = MaterialAlertDialogBuilder(fragmentContext!!)
                    deleteDialog.setTitle(getString(R.string.you_are_going_to_delete_multiple_items))
                    deleteDialog.setNegativeButton(getString(R.string.cancel)) { dialogInterface: DialogInterface, _: Int -> dialogInterface.cancel() }
                    deleteDialog.setPositiveButton(getString(R.string.ok)) { _: DialogInterface?, _: Int ->
                        if (selectedObjects.size == resultsList?.size){
                            lifecycleScope.launch(Dispatchers.IO){
                                resultViewModel.deleteAll()
                            }
                        }else{
                            resultViewModel.deleteSelected(selectedObjects.toList())
                        }
                        clearCheckedItems()
                        actionMode?.finish()
                    }
                    deleteDialog.show()
                    true
                }
                R.id.download -> {
                    lifecycleScope.launch {
                        val showDownloadCard = sharedPreferences!!.getBoolean("download_card", true)
                        if (showDownloadCard && selectedObjects.size == 1) {
                            println("showSingleDownloadSheet 2")
                            showSingleDownloadSheet(
                                selectedObjects[0],
                                downloadViewModel.getDownloadType(url = selectedObjects[0].url)
                            )
                        }else{
                            downloadViewModel.turnResultItemsToProcessingDownloads(selectedObjects.map { it.id }, downloadNow = !showDownloadCard)
                            if (showDownloadCard){
                                findNavController().navigate(R.id.downloadMultipleBottomSheetDialog2)
                            }
                        }
                        clearCheckedItems()
                        actionMode?.finish()
                    }
                    true
                }
                R.id.select_all -> {
                    homeAdapter?.checkAll(resultsList)
                    selectedObjects.clear()
                    resultsList?.forEach { selectedObjects.add(it!!) }
                    mode?.title = "(${selectedObjects.size}) ${resources.getString(R.string.all_items_selected)}"
                    true
                }
                R.id.invert_selected -> {
                    homeAdapter?.invertSelected(resultsList)
                    val invertedList = arrayListOf<ResultItem>()
                    resultsList?.forEach {
                        if (!selectedObjects.contains(it)) invertedList.add(it!!)
                    }
                    selectedObjects.clear()
                    selectedObjects.addAll(invertedList)
                    actionMode!!.title = "${selectedObjects.size} ${getString(R.string.selected)}"
                    if (invertedList.isEmpty()) actionMode?.finish()
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            actionMode = null
            (activity as MainActivity).enableBottomNavigation()
            clearCheckedItems()
            binding.searchBar.isEnabled = true
            playlistNameFilterChipGroup.children.forEach { it.isEnabled = true }
//            binding.searchBar.menu.forEach { it.isEnabled = true }
//            binding.searchBar.expand(binding.homeAppbarlayout)

            downloadAllFab!!.isVisible = showDownloadAllFab
            binding.copiedUrlFab.isVisible = showClipboardFab
        }
    }

    private fun clearCheckedItems(){
        homeAdapter?.clearCheckedItems()
        selectedObjects.forEach {
            homeAdapter?.notifyItemChanged(resultsList!!.indexOf(it))
        }
        selectedObjects.clear()
    }


    private fun checkClipboard(): List<String>? {
        return runCatching {
            val clipboard = requireContext().getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clipText = clipboard.primaryClip?.getItemAt(0)?.coerceToText(requireContext())?.toString()
                ?: return null

            println("checkClipboard: $clipText")
            if(clipText.isEmpty()) {
                binding.copiedUrlFab.gone()
            } else {
                binding.copiedUrlFab.visible()
            }

            val urls = clipText
                .split("\r", "\n")
                .map { it.trim() }
                .filter { Patterns.WEB_URL.matcher(it).matches() }

            val containsYouTube = urls.any {
                it.contains("youtube.com", ignoreCase = true) || it.contains("youtu.be", ignoreCase = true)
            }

            if (containsYouTube) {
                // Clear clipboard
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                // Show toast
                binding.searchBar.setText("")
                Toast.makeText(requireContext(), resources.getString(R.string.youtube_desc), Toast.LENGTH_SHORT).show()
                return null
            }

            return urls
        }.getOrNull()
    }

    override fun onStop() {
        actionMode?.finish()
        super.onStop()
    }


    companion object {
        private const val TAG = "HomeFragment"

        var latestURL = ""

        private val defaultList1 = listOf<Bookmark>(
            Bookmark("Facebook", "https://www.facebook.com/", "com.facebook.katana", R.drawable.icon_facebook),
            Bookmark("Instagram", "https://www.instagram.com/", "com.instagram.android", R.drawable.icon_instagram),
            Bookmark("WhatsApp", "https://www.whatsapp.com", "com.whatsapp", R.drawable.icon_whatsapp),
            Bookmark("TikTok", "https://www.tiktok.com", "com.zhiliaoapp.musically", R.drawable.icon_tiktok),
            Bookmark("Twitter", "https://x.com",  "com.twitter.android", R.drawable.icon_twitter),
            Bookmark("Dailymotion", "https://www.dailymotion.com",  "com.dailymotion.dailymotion",  R.drawable.icon_dailymotion),
            Bookmark("Vimeo", "https://vimeo.com", "com.vimeo.android.videoapp", R.drawable.icon_vimeo),
            Bookmark("Tubidy", "https://tubidy.cv", "com.tubidy", R.drawable.icon_tubidy),

        )

        private val defaultList2 = listOf<Bookmark>(
//            Bookmark("Pinterest", "https://www.pinterest.com/videos", "com.pinterest", R.drawable.icon_pinterest),
            Bookmark("ImDb", "https://www.imdb.com", "com.imdb.mobile", R.drawable.icon_imdb),
        )

        fun checkTheVideoURL(downloadedURL: String): Boolean {
            val uri = Uri.parse(downloadedURL)
            val host = uri.host.orEmpty() // "www.dailymotion.com"
            val cleanHost = host.removePrefix("www.")
            return RemoteConfig.ALLOWED_LIST.any { cleanHost.contains(it, true) }
        }
    }

    override fun onSearchSuggestionClick(text: String, isAllowed: Boolean) {
        val res = text.split("\n")
        if (res.size == 1){
            showClipboardFab = false
            binding.copiedUrlFab.isVisible = false
            binding.searchView.setText(text)
            println("searchView 3: $text and $res and $isAllowed")
            if (!checkTheVideoURL(text) && !isAllowed) {
                Toast.makeText(
                    requireContext(),
                    resources.getString(R.string.not_allowed),
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            if (!checkTheVideoURL(text)) {
                Toast.makeText(
                    requireContext(),
                    resources.getString(R.string.please_wait),
                    Toast.LENGTH_SHORT
                ).show()
            }

            if(text.contains("http", true)) {
                initSearch(binding.searchView)
            } else {
                resultViewModel.addSearchQueryToHistory(text)
                val encodedKeyword = URLEncoder.encode(text, StandardCharsets.UTF_8.toString())
                val urlNew = "https://www.google.com/search?q=$encodedKeyword&tbm=vid"
                startActivity(Intent(requireContext(), BrowseActivity::class.java).apply {
                    putExtra("receivedURL", urlNew)
                })
            }

        }else{
            res.forEach {
                onSearchSuggestionAdd(it)
            }
        }

    }

    override fun onSearchSuggestionAdd(text: String) {
        val items = text.split("\n")

        Toast.makeText(
            requireContext(),
            resources.getString(R.string.added_history),
            Toast.LENGTH_SHORT
        ).show()

        items.forEach {t ->
            val present = queriesChipGroup!!.children.firstOrNull { (it as Chip).text.toString() == t }
            if (present == null) {
                val chip = layoutinflater!!.inflate(R.layout.input_chip, queriesChipGroup, false) as Chip
                chip.text = t
                chip.chipBackgroundColor = ColorStateList.valueOf(MaterialColors.getColor(requireContext(), R.attr.colorSecondaryContainer, Color.BLACK))
                chip.setOnClickListener {
                    if (queriesChipGroup!!.childCount == 1) queriesConstraint!!.visibility = View.GONE
                    queriesChipGroup!!.removeView(chip)
                }
                queriesChipGroup!!.addView(chip)
            }

        }

        binding.searchView.editText.setText("")
        queriesConstraint?.isVisible = queriesChipGroup?.childCount!! > 0

        searchSuggestionsAdapter?.getList()?.apply {
            if (this.isNotEmpty()) {
                if (this.first().type == SearchSuggestionType.CLIPBOARD){
                    val newList = this.toMutableList().drop(1)
                    val input = newList.filter {
                        !it.text.contains(
                            "youtube",
                            true
                        ) || !it.text.contains("youtu.be", true) || !it.text.contains("www", true)
                    }
                    input.onEach {
                        println("input 1: $it")
                    }
                    searchSuggestionsAdapter?.submitList(input)
                }
            }

        }
    }

    override fun onSearchSuggestionLongClick(text: String, position: Int) {
        val deleteDialog = MaterialAlertDialogBuilder(requireContext())
        deleteDialog.setTitle(getString(R.string.you_are_going_to_delete) + " \"" + text + "\"!")
        deleteDialog.setNegativeButton(getString(R.string.cancel)) { dialogInterface: DialogInterface, _: Int -> dialogInterface.cancel() }
        deleteDialog.setPositiveButton(getString(R.string.ok)) { _: DialogInterface?, _: Int ->
            resultViewModel.removeSearchQueryFromHistory(text)
            updateSearchViewItems(binding.searchView.editText.text.toString())
        }
        deleteDialog.show()
    }

    override fun onSearchSuggestionAddToSearchBar(text: String) {
        binding.searchView.editText.setText(text)
        binding.searchView.editText.setSelection(binding.searchView.editText.length())
    }


    private fun updateMultiplePlaylistResults(playlistTitles: List<String>) {
        playlistNameFilterChipGroup.children.filter { it.tag != "all" }.forEach {
            playlistNameFilterChipGroup.removeView(it)
        }

        if (playlistTitles.isEmpty() || playlistTitles.size == 1) {
            playlistNameFilterScrollView.isVisible = false
            return
        }

        playlistNameFilterChipGroup.children.first().setOnClickListener {
            resultViewModel.setPlaylistFilter("")
        }

        for (t in playlistTitles) {
            val tmp = layoutinflater!!.inflate(R.layout.filter_chip, playlistNameFilterChipGroup, false) as Chip
            tmp.text = t
            tmp.tag = t
            tmp.setOnClickListener {
                resultViewModel.setPlaylistFilter(t)
            }

            playlistNameFilterChipGroup.addView(tmp)
        }

        if (playlistNameFilterChipGroup.children.all { !(it as Chip).isChecked }) {
            (playlistNameFilterChipGroup.children.first() as Chip).isChecked = true
        }

        playlistNameFilterScrollView.isVisible = true
    }

//    private var listener: OnSearchCompleteListener? = null
    interface OnSearchCompleteListener {
        fun onSearchComplete(isShowed: Boolean)
    }
//    override fun onAttach(context: Context) {
//        super.onAttach(context)
//        if (context is OnSearchCompleteListener) {
//            listener = context
//        } else {
//            throw RuntimeException("$context must implement OnSearchCompleteListener")
//        }
//    }
//
//    override fun onDetach() {
//        super.onDetach()
//        listener = null
//    }

}

data class Bookmark(val name: String, val url: String,val packageName: String? = null, var imagePath: Int = 0)