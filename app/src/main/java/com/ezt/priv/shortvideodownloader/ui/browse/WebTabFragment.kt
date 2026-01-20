package com.ezt.priv.shortvideodownloader.ui.browse

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ShareCompat
import androidx.databinding.Observable
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.ezt.priv.shortvideodownloader.database.viewmodel.HistoryViewModel
import com.ezt.priv.shortvideodownloader.databinding.FragmentWebTabBinding
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.database.models.main.ResultItem
import com.ezt.priv.shortvideodownloader.ui.CustomWebChromeClient
import com.ezt.priv.shortvideodownloader.ui.browse.BrowseActivity.Companion.sendURL
import com.ezt.priv.shortvideodownloader.ui.browse.BrowseActivity.Companion.toResultItem
import com.ezt.priv.shortvideodownloader.ui.browse.detector.SingleLiveEvent
import com.ezt.priv.shortvideodownloader.ui.browse.proxy_utils.CustomProxyController
import com.ezt.priv.shortvideodownloader.ui.browse.proxy_utils.OkHttpProxyClient
import com.ezt.priv.shortvideodownloader.ui.browse.qualifier.VideFormatEntityList
import com.ezt.priv.shortvideodownloader.ui.browse.qualifier.VideoInfo
import com.ezt.priv.shortvideodownloader.ui.browse.scheduler.BaseWebTabFragment
import com.ezt.priv.shortvideodownloader.ui.browse.viewmodel.DownloadButtonState
import com.ezt.priv.shortvideodownloader.ui.browse.viewmodel.DownloadButtonStateCanDownload
import com.ezt.priv.shortvideodownloader.ui.browse.viewmodel.DownloadButtonStateCanNotDownload
import com.ezt.priv.shortvideodownloader.ui.browse.viewmodel.DownloadButtonStateLoading
import com.ezt.priv.shortvideodownloader.ui.browse.viewmodel.MainViewModel
import com.ezt.priv.shortvideodownloader.ui.browse.viewmodel.VideoDetectionTabViewModel
import com.ezt.priv.shortvideodownloader.ui.browse.viewmodel.WebTabViewModel
import com.ezt.priv.shortvideodownloader.ui.browse.webtab.WebTab
import com.ezt.priv.shortvideodownloader.ui.browse.webtab.WebTabFactory
import com.ezt.priv.shortvideodownloader.ui.home.MainActivity
import com.ezt.priv.shortvideodownloader.ui.tab.viewmodel.TabViewModel
import com.ezt.priv.shortvideodownloader.util.Common.gone
import com.ezt.priv.shortvideodownloader.util.Common.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Arrays
import java.util.UUID
import javax.inject.Inject

interface BrowserServicesProvider : TabManagerProvider, PageTabProvider, HistoryProvider,
    WorkerEventProvider, CurrentTabIndexProvider

interface TabManagerProvider {
    fun getOpenTabEvent(): SingleLiveEvent<WebTab>

    fun getCloseTabEvent(): SingleLiveEvent<WebTab>

    fun getUpdateTabEvent(): SingleLiveEvent<WebTab>

    fun getTabsListChangeEvent(): ObservableField<List<WebTab>>
}

interface PageTabProvider {
    fun getPageTab(position: Int): WebTab
}

interface HistoryProvider {
    fun getHistoryVModel(): HistoryViewModel
}

interface WorkerEventProvider {
    fun getWorkerM3u8MpdEvent(): MutableLiveData<DownloadButtonState>

    fun getWorkerMP4Event(): MutableLiveData<DownloadButtonState>
}

interface CurrentTabIndexProvider {
    fun getCurrentTabIndex(): ObservableInt
}

interface BrowserListener {
    fun onBrowserMenuClicked()

    fun onBrowserReloadClicked()

    fun onTabCloseClicked()

    fun onBrowserStopClicked()

    fun onBrowserBackClicked()

    fun onBrowserForwardClicked()
}

const val HOME_TAB_INDEX = 0

const val TAB_INDEX_KEY = "TAB_INDEX_KEY"

@AndroidEntryPoint
class WebTabFragment : BaseWebTabFragment() {

    companion object {
        fun newInstance() = WebTabFragment()
    }


    @Inject
    lateinit var proxyController: CustomProxyController

    @Inject
    lateinit var okHttpProxyClient: OkHttpProxyClient

    private lateinit var dataBinding: FragmentWebTabBinding

    private lateinit var tabManagerProvider: TabManagerProvider

    private lateinit var pageTabProvider: PageTabProvider

    private lateinit var historyProvider: HistoryProvider

    private lateinit var workerEventProvider: WorkerEventProvider

    private lateinit var currentTabIndexProvider: CurrentTabIndexProvider

    private val tabViewModel: WebTabViewModel by viewModels()

    private val videoDetectionTabViewModel : VideoDetectionTabViewModel by viewModels()

    private val mainViewModel: MainViewModel by viewModels()

    private lateinit var webTab: WebTab

    private var videoToast: Toast? = null

    private var canGoCounter = 0

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            handleOnBackPress()
        }
    }

    private var onDownloadMediaFileListener: OnDownloadMediaFileListener? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val thisTabIndex = requireArguments().getInt(TAB_INDEX_KEY)
        webTab = WebTabFactory.createWebTabFromInput(sendURL)
        videoDetectionTabViewModel.settingsModel = browseActivity.settingsViewModel
        videoDetectionTabViewModel.webTabModel = tabViewModel

        val activity = requireActivity() as BrowseActivity

        tabManagerProvider = activity
        pageTabProvider  = activity
        historyProvider  = activity
        workerEventProvider  = activity
        currentTabIndexProvider  = activity

        tabViewModel.openPageEvent = tabManagerProvider.getOpenTabEvent()
        tabViewModel.closePageEvent = tabManagerProvider.getCloseTabEvent()
        tabViewModel.thisTabIndex.set(thisTabIndex)

        Log.d("WebTabFragment","onCreate Webview::::::::: $webTab $savedInstanceState")

        recreateWebView(savedInstanceState)

        dataBinding = FragmentWebTabBinding.inflate(inflater, container, false).apply {

            viewModel = tabViewModel
            browserMenuListener = tabListener
            settingsViewModel = browseActivity.settingsViewModel
            videoTabVModel = videoDetectionTabViewModel

            etSearch.addTextChangedListener(onInputTabChangeListener)
            this.etSearch.imeOptions = EditorInfo.IME_ACTION_DONE
            this.etSearch.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    this.etSearch.clearFocus()
                    viewModel?.viewModelScope?.launch {
                        delay(400)
                        tabViewModel.loadPage((this@apply.etSearch as EditText).text.toString())
                    }
                    false
                } else false
            }

            ivCloseTab.clipToOutline = true
            ivGoForward.clipToOutline = true
            ivGoBack.clipToOutline = true
            ivCloseRefresh.clipToOutline = true

            Glide.with(this@WebTabFragment).asGif().load(R.drawable.loading_floating)
                .into(loadingWavy)
            loadingWavy.clipToOutline = true

            configureWebView(this)

            tabViewModel.getTabTextInput().addOnPropertyChangedCallback(
                object : Observable.OnPropertyChangedCallback() {
                    override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                        val currentURL = tabViewModel.getTabTextInput().get() ?: ""
                        updateTabValue(currentURL, activity)
                        if(currentURL.contains("youtube", true) || currentURL.contains("youtu.be", true)) {
                            Toast.makeText(context, resources.getString(R.string.youtube_desc),
                                Toast.LENGTH_SHORT).show()
                            dataBinding.fab.gone()
                            loadingWavy.gone()
                        } else {
                            dataBinding.fab.visible()
                            loadingWavy.visible()
                        }
                    }
                }
            )
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner, backPressedCallback
        )

        addChangeRouteCallBack()

        tabViewModel.userAgent.set(
            webTab.getWebView()?.settings?.userAgentString
                ?: BrowseActivity.MOBILE_URGENT
        )

        val message = webTab.getMessage()
        if (message != null) {
            message.sendToTarget()
            webTab.flushMessage()
        } else {
            tabViewModel.loadPage(webTab.getUrl())
        }

        return dataBinding.root
    }

    private fun updateTabValue(urlNew: String, activity1: BrowseActivity) {
        val position = MainActivity.currentTabPosition
        println("currentTabPosition 1: $position")
        if(position >= 0) {
            val allTabs = TabViewModel.getAllTabs(activity1).toMutableList()
            allTabs[position] = urlNew
            TabViewModel.addNewTab(activity1, allTabs)
        }
    }

    override fun shareWebLink() {
        val link = webTab.getWebView()?.url
        if (link != null) {
//            shareLink(link)
        }
    }

    override fun bookmarkCurrentUrl() {
        val webview = webTab.getWebView()
        val url = webview?.url
        val favicon = webview?.favicon
        val name = webview?.title

        if (url == null) {
            return
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (!outState.isEmpty) {
            webTab.getWebView()?.saveState(outState)
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState != null && !savedInstanceState.isEmpty) {
            webTab.getWebView()?.restoreState(savedInstanceState)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        handleIndexChangeEvent()
        handleLoadPageEvent()
        handleChangeTabFocusEvent()
        handleWorkerEvent()
        handleOpenDetectedVideos()
        handleVideoPushed()
        tabViewModel.start()
        videoDetectionTabViewModel.start()
    }

    override fun onPause() {
        Log.d("WebTabFragment","onPause Webview::::::::: ${webTab.getUrl()}")
        super.onPause()
        onWebViewPause()
        backPressedCallback.remove()
    }

    override fun onResume() {
        Log.d("WebTabFragment","onResume Webview::::::::: ${webTab.getUrl()}")
        super.onResume()
        onWebViewResume()

        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner, backPressedCallback
        )
    }

    override fun onDestroy() {
        Log.d("WebTabFragment","onDestroy Webview::::::::: ${webTab.getUrl()}")
        super.onDestroy()
        webTab.getWebView()?.let { destroyWebView(it) }
        tabViewModel.stop()
        webTab.setWebView(null)
        videoDetectionTabViewModel.stop()
        tabManagerProvider.getTabsListChangeEvent()
            .removeOnPropertyChangedCallback(tabsListChangeListener)
    }

    private fun handleOpenDetectedVideos() {
        videoDetectionTabViewModel.showDetectedVideosEvent.observe(viewLifecycleOwner) {
            val list =  videoDetectionTabViewModel.detectedVideosList.get()?.toList()
            if(!list.isNullOrEmpty()) {
                val videoInfo = list.first()
                videoInfo.formats.formats.onEach {
                    println("showAlertVideoFound 0: $it")
                }
                val resultItem = videoInfo.toResultItem()
                onDownloadMediaFileListener?.onDownloadMediaFile(resultItem)
            }
        }
    }

    private fun handleVideoPushed() {
        videoDetectionTabViewModel.videoPushedEvent.observe(viewLifecycleOwner) {
            onVideoPushed()
        }
    }

    private fun onVideoPushed() {
        showToastVideoFound()

        val isDownloadsVisible = isDetectedVideosTabFragmentVisible()
        val isCond = !tabViewModel.isDownloadDialogShown.get() && !isDownloadsVisible
        if (context != null && browseActivity.settingsViewModel.getVideoAlertState()
                .get() && isCond
        ) {
            lifecycleScope.launch(Dispatchers.Main) {
                showAlertVideoFound()
            }
        }
    }

    private fun onVideoPreviewPropagate(
        videoInfo: VideoInfo, format: String, isForce: Boolean
    ) {
        Log.d("WebTabFragment",
            "onPreviewVideo: ${videoInfo.formats}  $format"
        )
        // start your activity by passing the intent
    }

    private fun onVideoDownloadPropagate(
        videoInfo: VideoInfo, videoTitle: String, format: String
    ) {
        val info = videoInfo.copy(
            id = UUID.randomUUID().toString(),
            title = FileNameCleaner.cleanFileName(videoTitle),
            formats = VideFormatEntityList(videoInfo.formats.formats.filter {
                it.format?.contains(
                    format
                ) ?: false
            })
        )

        browseActivity.mainViewModel.downloadVideoEvent.value = info

        context?.let {
            Toast.makeText(
                it, it.getString(R.string.download_started), Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun recreateWebView(savedInstanceState: Bundle?) {
        if (webTab.getMessage() == null || webTab.getWebView() == null) {
            webTab.setWebView(WebView(requireContext()))
        }

        if (savedInstanceState != null) {
            webTab.getWebView()?.restoreState(savedInstanceState)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView(fragmentWebTabBinding: FragmentWebTabBinding) {
        val currentWebView = this.webTab.getWebView()

        val webViewClient = CustomWebViewClient(
            tabViewModel,
            browseActivity.settingsViewModel,
            videoDetectionTabViewModel,
            okHttpProxyClient,
            tabManagerProvider.getUpdateTabEvent(),
            pageTabProvider,
            proxyController,
        )

        val chromeClient = CustomWebChromeClient(
            tabViewModel,
            tabManagerProvider.getUpdateTabEvent(),
            pageTabProvider,
            fragmentWebTabBinding,
            browseActivity
        )

        currentWebView?.webChromeClient = chromeClient
        currentWebView?.webViewClient = webViewClient

        val webSettings = webTab.getWebView()?.settings
        val webView = webTab.getWebView()

        webView?.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView?.isScrollbarFadingEnabled = true

        // TODO: turn on third-party from settings
//        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webSettings?.apply {
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            setSupportZoom(true)
            setSupportMultipleWindows(true)
            setGeolocationEnabled(false)
            allowContentAccess = true
            allowFileAccess = true
            offscreenPreRaster = false
            displayZoomControls = false
            builtInZoomControls = true
            loadWithOverviewMode = true
            layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
            useWideViewPort = true
            domStorageEnabled = true
            javaScriptEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            javaScriptCanOpenWindowsAutomatically = true
            mediaPlaybackRequiresUserGesture = false
            if (browseActivity.settingsViewModel.isDesktopMode.get()) {
                userAgentString = BrowseActivity.DESKTOP_URGENT
            }
        }
        fragmentWebTabBinding.webviewContainer.addView(
            webTab.getWebView(),
            LinearLayout.LayoutParams(-1, -1)
        )
    }

    private val onInputTabChangeListener = object : TextWatcher {
        override fun afterTextChanged(s: Editable) {
            val input = s.toString()

            tabViewModel.showTabSuggestions()
            tabViewModel.tabPublishSubject.onNext(input)
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }
    }

    private fun handleChangeTabFocusEvent() {
        var value = -1
        tabViewModel.changeTabFocusEvent.observe(viewLifecycleOwner) { isFocus ->
            isFocus.let {
                if (it) {
                    val oldValue = value
                    val start = dataBinding.etSearch.selectionStart
                    val end = dataBinding.etSearch.selectionEnd
                    value = (start + end) / 2
                    if (oldValue == value) {
                        dataBinding.etSearch.selectAll()

                    }
                    tabViewModel.isTabInputFocused.set(true)
                } else {
                    tabViewModel.isTabInputFocused.set(false)
                }
            }
        }
    }

    private fun handleLoadPageEvent() {
        tabViewModel.loadPageEvent.observe(viewLifecycleOwner) { tab ->
            if (tab.getUrl().startsWith("http")) {
                webTab.getWebView()?.stopLoading()
                webTab.getWebView()?.loadUrl(tab.getUrl())
            }
        }
    }

    private fun handleWorkerEvent() {
        workerEventProvider.getWorkerM3u8MpdEvent().observe(viewLifecycleOwner) { state ->
            if (state is DownloadButtonStateCanDownload && state.info?.id?.isNotEmpty() == true) {
                videoDetectionTabViewModel.pushNewVideoInfoToAll(state.info)
                val loadings = videoDetectionTabViewModel.m3u8LoadingList.get()
                loadings?.remove("m3u8")
                videoDetectionTabViewModel.m3u8LoadingList.set(loadings?.toMutableSet())
            }
            if (state is DownloadButtonStateLoading) {
                val loadings = videoDetectionTabViewModel.m3u8LoadingList.get()
                loadings?.add("m3u8")
                videoDetectionTabViewModel.m3u8LoadingList.set(loadings?.toMutableSet())
                videoDetectionTabViewModel.setButtonState(DownloadButtonStateLoading())
            }
            if (state is DownloadButtonStateCanNotDownload) {
                val loadings = videoDetectionTabViewModel.m3u8LoadingList.get()
                loadings?.remove("m3u8")
                videoDetectionTabViewModel.m3u8LoadingList.set(loadings?.toMutableSet())
                videoDetectionTabViewModel.setButtonState(DownloadButtonStateCanNotDownload())
            }
        }

        workerEventProvider.getWorkerMP4Event().observe(viewLifecycleOwner) { state ->
            if (state is DownloadButtonStateCanDownload && state.info?.id?.isNotEmpty() == true) {
                Log.d("WebTabFragment","Worker MP4 event CanDownload: ${state.info}")
                videoDetectionTabViewModel.pushNewVideoInfoToAll(state.info)
            } else {
                Log.d("WebTabFragment","Worker MP4 event state: $state")
            }
        }
    }

    private fun handleIndexChangeEvent() {
        tabManagerProvider.getTabsListChangeEvent()
            .addOnPropertyChangedCallback(tabsListChangeListener)
    }

    private val tabsListChangeListener = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            val tabs = tabManagerProvider.getTabsListChangeEvent().get()
            val webTab = tabs?.find { it.id == webTab.id }
            val index = tabs?.indexOf(webTab)
            if (index != null && index in tabs.indices) {
                tabViewModel.thisTabIndex.set(index)
            }
        }
    }

    private fun onWebViewPause() {
        webTab.getWebView()?.onPause()
    }

    private fun onWebViewResume() {
        webTab.getWebView()?.onResume()
    }

    private val tabListener = object : BrowserListener {
        override fun onBrowserMenuClicked() {
        }

        override fun onBrowserReloadClicked() {
            var url = webTab.getWebView()?.url
            var urlWasChange = false

            if (url?.contains("m.facebook") == true) {
                url = url.replace("m.facebook", "www.facebook")
                urlWasChange = true
                val isDesktop = browseActivity.settingsViewModel.isDesktopMode.get()
                if (!isDesktop) {
                    browseActivity.settingsViewModel.setIsDesktopMode(true)
                }
            }

            val userAgent =
                webTab.getWebView()?.settings?.userAgentString ?: tabViewModel.userAgent.get()
                ?: BrowseActivity.MOBILE_URGENT
            if (url != null) {
                videoDetectionTabViewModel.viewModelScope.launch(videoDetectionTabViewModel.executorReload) {
                    videoDetectionTabViewModel.onStartPage(url, userAgent)
                }

                if (url.contains("www.facebook") && urlWasChange) {
                    tabViewModel.openPage(url)
                    tabViewModel.closeTab(webTab)
                } else {
                    tabViewModel.onPageReload(webTab.getWebView())
                }
            }
        }


        override fun onTabCloseClicked() {
            startActivity(Intent(requireContext(), MainActivity::class.java))
            tabViewModel.closeTab(webTab)
            videoDetectionTabViewModel.cancelAllCheckJobs()
        }

        override fun onBrowserStopClicked() {
            tabViewModel.onPageStop(webTab.getWebView())
        }

        override fun onBrowserBackClicked() {
            val webView = webTab.getWebView()
            val canGoBack = webView?.canGoBack()
            if (canGoBack == true) {
                webView.goBack()
                tabViewModel.onGoBack(webView)
                videoDetectionTabViewModel.cancelAllCheckJobs()
            }

            if (canGoBack == false) {
                if (canGoCounter >= 1) {
                    canGoCounter = 0
                    browseActivity.mainViewModel.openNavDrawerEvent.call()
                } else {
                    canGoCounter++
                }
            }
        }

        override fun onBrowserForwardClicked() {
            val webView = webTab.getWebView()
            val canGoForward = webView?.canGoForward()
            if (canGoForward == true) {
                webView.goForward()
                tabViewModel.onGoForward(webView)
                videoDetectionTabViewModel.cancelAllCheckJobs()
            }
        }
    }

    private fun getWebViewClientCompat(webView: WebView?): CustomWebViewClient? {
        return try {
            val getWebViewClientMethod = WebView::class.java.getMethod("getWebViewClient")
            val client = getWebViewClientMethod.invoke(webView) as? CustomWebViewClient
            client
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun showAlertVideoFound() {
        if (!tabViewModel.isDownloadDialogShown.get()) {
            tabViewModel.isDownloadDialogShown.set(true)
            val client = getWebViewClientCompat(webTab.getWebView())

            videoDetectionTabViewModel.detectedVideosList.get()?.toList()?.onEach {
                println("showAlertVideoFound: $it")
            }

//            client?.videoAlert =
//                MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.video_found)
//            client?.videoAlert?.setOnDismissListener {
//                client.videoAlert = null
//            }
//            client?.videoAlert?.setMessage(R.string.whatshould)?.setPositiveButton(
//                R.string.view
//            ) { dialog, _ ->
//                tabViewModel.isDownloadDialogShown.set(false)
//                dialog.dismiss()
//            }?.setNeutralButton(R.string.dontshow) { dialog, _ ->
//                browseActivity.settingsViewModel.setShowVideoAlertOff()
//                tabViewModel.isDownloadDialogShown.set(false)
//                dialog.dismiss()
//            }?.setNegativeButton(R.string.cancel) { dialog, _ ->
//                tabViewModel.isDownloadDialogShown.set(false)
//                dialog.dismiss()
//            }?.show()
        }
    }

    private fun handleOnBackPress() {
        val isBrowserRoute = browseActivity.mainViewModel.currentItem.get() == 0
        val isCurrentTabSelected =
            currentTabIndexProvider.getCurrentTabIndex().get() == requireArguments().getInt(
                TAB_INDEX_KEY
            )
//        val isCurrentTabSelected = true
        val isStateResumed = viewLifecycleOwner.lifecycle.currentState == Lifecycle.State.RESUMED

        if (isStateResumed && isBrowserRoute && isCurrentTabSelected && isVisible) {
            webTab.getWebView()?.goBack()
        }
    }

    private fun setUserAgentIsDesktop(isDesktop: Boolean) {
        val settings = webTab.getWebView()?.settings
        if (isDesktop) {
            settings?.userAgentString = BrowseActivity.DESKTOP_URGENT
        } else {
            settings?.userAgentString = null
        }
    }

    private fun addChangeRouteCallBack() {
        browseActivity.mainViewModel.currentItem.removeOnPropertyChangedCallback(changeRouteCallBack)
        browseActivity.mainViewModel.currentItem.addOnPropertyChangedCallback(changeRouteCallBack)
    }

    private val changeRouteCallBack = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            val indexRoute = browseActivity.mainViewModel.currentItem.get()
//            val currentTabIndexSelected = currentTabIndexProvider.getCurrentTabIndex().get()
            val currentTabIndexSelected = 0
            val isCurrentTabSelected =
                currentTabIndexSelected == requireArguments().getInt(TAB_INDEX_KEY)
            val isBrowserRoute = indexRoute == 0
            val isNotHomeTabSelected = currentTabIndexSelected != HOME_TAB_INDEX
            val isVisible = this@WebTabFragment.isVisible
            if (isBrowserRoute && isNotHomeTabSelected && isCurrentTabSelected && isVisible) {
                activity?.onBackPressedDispatcher?.addCallback(
                    viewLifecycleOwner, backPressedCallback
                )
            } else {
                backPressedCallback.remove()
            }
        }
    }

    private fun showToastVideoFound() {
        return
        val context = context

        if (context != null) {
            Handler(Looper.getMainLooper()).postDelayed({
                videoToast?.cancel()
                videoToast = Toast.makeText(
                    context, context.getString(R.string.video_found), Toast.LENGTH_SHORT
                )
                videoToast?.show()
            }, 1)
        }
    }

    private fun destroyWebView(webView: WebView) {
        val webViewContainer: ViewGroup = webView.parent as ViewGroup
        webViewContainer.removeView(webView)
        webView.destroy()
        webTab.setWebView(null)
    }


    private fun isDetectedVideosTabFragmentVisible(): Boolean {
        val fragmentManager = requireActivity().supportFragmentManager
        return false
    }

    private val downloadListener = object : DownloadTabListener {
        override fun onCancel() {
            browseActivity.supportFragmentManager.popBackStack()
        }

        override fun onPreviewVideo(
            videoInfo: VideoInfo, format: String, isForce: Boolean
        ) {
            onVideoPreviewPropagate(videoInfo, format, isForce)
        }

        override fun onDownloadVideo(
            videoInfo: VideoInfo, format: String, videoTitle: String
        ) {
            onVideoDownloadPropagate(videoInfo, videoTitle, format)
        }

        override fun onSelectFormat(videoInfo: VideoInfo, format: String) {
            val formats =
                videoDetectionTabViewModel.selectedFormats.get()?.toMutableMap() ?: mutableMapOf()
            formats[videoInfo.id] = format
            videoDetectionTabViewModel.selectedFormats.set(formats)
        }

        override fun onFormatUrlShare(videoInfo: VideoInfo, format: String): Boolean {
            val foundFormat = videoInfo.formats.formats.find { thisFormat ->
                thisFormat.format?.contains(format) == true
            }
            if (foundFormat == null) {
                return false
            }

            ShareCompat.IntentBuilder(browseActivity).setType("text/plain")
                .setChooserTitle("Share Link")
                .setText(foundFormat.url).startChooser()
            return true
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnDownloadMediaFileListener) {
            onDownloadMediaFileListener = context
        } else {
            throw RuntimeException("$context must implement CallbackIntro")
        }
    }

    override fun onDetach() {
        super.onDetach()
        onDownloadMediaFileListener = null
    }
}


interface OnDownloadMediaFileListener {
    fun onDownloadMediaFile(resultItem: ResultItem)
}

interface DownloadTabListener : DownloadTabVideoListener, CandidateFormatListener {
    fun onCancel()
}

interface DownloadTabVideoListener {
    fun onPreviewVideo(
        videoInfo: VideoInfo,
        format: String,
        isForce: Boolean
    )

    fun onDownloadVideo(
        videoInfo: VideoInfo,
        format: String,
        videoTitle: String
    )
}

interface CandidateFormatListener {
    fun onSelectFormat(videoInfo: VideoInfo, format: String)

    fun onFormatUrlShare(videoInfo: VideoInfo, format: String): Boolean
}

object FileNameCleaner {
    private const val MAX_FILE_NAME_LENGTH = 100
    private val illegalChars = intArrayOf(
        34,
        60,
        62,
        124,
        0,
        1,
        2,
        3,
        4,
        5,
        6,
        7,
        8,
        9,
        10,
        11,
        12,
        13,
        14,
        15,
        16,
        17,
        18,
        19,
        20,
        21,
        22,
        23,
        24,
        25,
        26,
        27,
        28,
        29,
        30,
        31,
        58,
        42,
        63,
        92,
        47
    )

    init {
        Arrays.sort(illegalChars)
    }

    fun cleanFileName(badFileName: String): String {
        val cleanName = StringBuilder()
        for (element in badFileName) {
            val c = element.code
            if (Arrays.binarySearch(illegalChars, c) < 0) {
                cleanName.append(c.toChar())
            }
        }
        var finalName = cleanName.toString()
            .replace(".mp3", "")
            .replace(".mp4", "")
            .replace("/", "").replace("\\", "")
            .replace(":", "")
            .replace("*", "")
            .replace("?", "")
            .replace("\"", "")
            .replace("`", "")
            .replace("\'", "")
            .replace("<", "")
            .replace(">", "")
            .replace(".", "_")
            .replace("|", "")
            .replace(Regex("\\s*-\\s*"), "-")
            .replace(" ", "_").trim()
        if (finalName.isEmpty()) {
            finalName = "Untitled"
        }

        if (finalName.length > MAX_FILE_NAME_LENGTH) {
            return finalName.substring(0, MAX_FILE_NAME_LENGTH)
        }

        return finalName
    }
}
