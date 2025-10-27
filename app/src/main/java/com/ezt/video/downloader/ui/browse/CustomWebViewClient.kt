package com.ezt.video.downloader.ui.browse

import android.graphics.Bitmap
import android.os.Build
import android.webkit.HttpAuthHandler
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.viewModelScope
import com.ezt.video.downloader.database.viewmodel.HistoryViewModel
import com.ezt.video.downloader.database.viewmodel.SettingsViewModel
import com.ezt.video.downloader.ui.browse.detector.IVideoDetector
import com.ezt.video.downloader.ui.browse.detector.SingleLiveEvent
import com.ezt.video.downloader.ui.browse.proxy_utils.CookieUtils
import com.ezt.video.downloader.ui.browse.proxy_utils.CustomProxyController
import com.ezt.video.downloader.ui.browse.proxy_utils.OkHttpProxyClient
import com.ezt.video.downloader.ui.browse.viewmodel.SettingViewModel
import com.ezt.video.downloader.ui.browse.viewmodel.WebTabViewModel
import com.ezt.video.downloader.ui.browse.webtab.WebTab
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

enum class ContentType {
    M3U8,
    MPD,
    VIDEO,
    AUDIO,
    OTHER
}

class CustomWebViewClient(
    private val tabViewModel: WebTabViewModel,
    private val settingsModel: SettingViewModel,
    private val videoDetectionModel: IVideoDetector,
    private val historyModel: HistoryViewModel,
    private val okHttpProxyClient: OkHttpProxyClient,
    private val updateTabEvent: SingleLiveEvent<WebTab>,
    private val pageTabProvider: PageTabProvider,
    private val proxyController: CustomProxyController
) : WebViewClient() {
    var videoAlert: MaterialAlertDialogBuilder? = null
    private var lastSavedHistoryUrl: String = ""
    private var lastSavedTitleHistory: String = ""
    private var lastRegularCheckUrl = ""
    private val regularJobsStorage: MutableMap<String, List<Disposable>> = mutableMapOf()

    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
        val viewTitle = view?.title
        val title = tabViewModel.currentTitle.get()
        val userAgent = view?.settings?.userAgentString ?: tabViewModel.userAgent.get()

        super.doUpdateVisitedHistory(view, url, isReload)
    }

    // TODO handle for proxy and others
    override fun onReceivedHttpAuthRequest(
        view: WebView?, handler: HttpAuthHandler?, host: String?, realm: String?
    ) {
        val creds = proxyController.getProxyCredentials()
        handler?.proceed(creds.first, creds.second)
    }

    override fun shouldInterceptRequest(
        view: WebView?, request: WebResourceRequest?
    ): WebResourceResponse? {
        val url = request?.url.toString()
        val isCheckM3u8 = settingsModel.isCheckIfEveryRequestOnM3u8.get()
        val isCheckOnMp4 = settingsModel.getIsCheckEveryRequestOnMp4Video().get()
        val isCheckOnAudio = settingsModel.isCheckOnAudio.get()

        if (isCheckOnMp4 || isCheckM3u8 || isCheckOnAudio) {
            val requestWithCookies = request?.let { resourceRequest ->
                try {
                    CookieUtils.webRequestToHttpWithCookies(
                        resourceRequest
                    )
                } catch (_: Throwable) {
                    null
                }
            }

            val contentType =
                VideoUtils.getContentTypeByUrl(url, requestWithCookies?.headers, okHttpProxyClient)

            when {

                contentType == ContentType.M3U8 || contentType == ContentType.MPD || url.contains(".m3u8") || url.contains(
                    ".mpd"
                ) || (url.contains(".txt") && url.contains("hentaihaven")) -> {
                    if (requestWithCookies != null && isCheckM3u8) {
                        videoDetectionModel.verifyLinkStatus(
                            requestWithCookies, tabViewModel.currentTitle.get(), true
                        )
                    }
                }

                else -> {
                    if ((isCheckOnMp4 || isCheckOnAudio) && contentType != ContentType.OTHER) {
                        val disposable = videoDetectionModel.checkRegularVideoOrAudio(
                            requestWithCookies,
                            isCheckOnAudio,
                            isCheckOnMp4
                        )

                        val currentUrl = tabViewModel.getTabTextInput().get() ?: ""
                        if (currentUrl != lastRegularCheckUrl) {
                            regularJobsStorage[lastRegularCheckUrl]?.forEach {
                                it.dispose()
                            }
                            regularJobsStorage.remove(lastRegularCheckUrl)
                            lastRegularCheckUrl = currentUrl
                        }
                        if (disposable != null) {
                            val overall = mutableListOf<Disposable>()
                            overall.addAll(regularJobsStorage[currentUrl]?.toList() ?: emptyList())
                            overall.add(disposable)
                            regularJobsStorage[currentUrl] = overall
                        }
                    }
                }
            }
        }

        return super.shouldInterceptRequest(
            view, request
        )
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)

        videoAlert = null
        val pageTab = pageTabProvider.getPageTab(tabViewModel.thisTabIndex.get())
        val headers = pageTab.getHeaders() ?: emptyMap()
        val favi = pageTab.getFavicon() ?: view.favicon ?: favicon

        updateTabEvent.value = WebTab(
            url,
            view.title,
            favi,
            headers,
            view,
            id = pageTab.id
        )
        tabViewModel.onStartPage(url, view.title)
    }

    override fun shouldOverrideUrlLoading(view: WebView, url: WebResourceRequest): Boolean {
        val isAdBlockerOn = settingsModel.isAdBlocker.get()
        val isAd = false

        return if (url.url.toString().startsWith("http") && url.isForMainFrame && !isAd) {
            if (!tabViewModel.isTabInputFocused.get()) {
                tabViewModel.setTabTextInput(url.url.toString())
            }
            false
        } else {
            true
        }
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        tabViewModel.finishPage(url)
    }

    override fun onRenderProcessGone(
        view: WebView?, detail: RenderProcessGoneDetail?
    ): Boolean {
        val pageTab = pageTabProvider.getPageTab(tabViewModel.thisTabIndex.get())

        val webView = pageTab.getWebView()
        if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                view == webView && detail?.didCrash() == true
            } else {
                view == webView
            }
        ) {
            webView?.destroy()
            return true
        }

        return super.onRenderProcessGone(view, detail)
    }
}