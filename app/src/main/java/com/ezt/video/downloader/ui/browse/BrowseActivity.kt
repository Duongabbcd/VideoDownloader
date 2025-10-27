package com.ezt.video.downloader.ui.browse

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.JavascriptInterface
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebChromeClient.CustomViewCallback
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.ezt.video.downloader.R
import com.ezt.video.downloader.ads.RemoteConfig
import com.ezt.video.downloader.ads.type.BannerAds.BANNER_HOME
import com.ezt.video.downloader.ads.type.InterAds
import com.ezt.video.downloader.database.models.main.ResultItem
import com.ezt.video.downloader.database.viewmodel.DownloadViewModel
import com.ezt.video.downloader.database.viewmodel.ResultViewModel
import com.ezt.video.downloader.databinding.ActivityBrowseBinding
import com.ezt.video.downloader.ui.BaseActivity2
import com.ezt.video.downloader.ui.home.MainActivity
import com.ezt.video.downloader.ui.home.MainActivity.Companion.loadBanner
import com.ezt.video.downloader.ui.info.DownloadInfoActivity
import com.ezt.video.downloader.ui.tab.TabActivity
import com.ezt.video.downloader.ui.tab.viewmodel.TabViewModel
import com.ezt.video.downloader.ui.whatsapp.WhatsAppActivity
import com.ezt.video.downloader.util.Common.gone
import com.ezt.video.downloader.util.Common.visible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BrowseActivity : BaseActivity2<ActivityBrowseBinding>(ActivityBrowseBinding::inflate) {
    private lateinit var resultViewModel : ResultViewModel
    private lateinit var downloadViewModel : DownloadViewModel

    private var sharedPreferences: SharedPreferences? = null

    private val urlNew by lazy {
        intent.getStringExtra("receivedURL") ?: ""
    }

    private var customView: View? = null
    private var customViewCallback: CustomViewCallback? = null
    private var originalSystemUiVisibility = 0

    private var jsInterfaceAdded = false
    private var queryList = mutableListOf<String>()
    private var videoDetected = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        downloadViewModel = ViewModelProvider(this)[DownloadViewModel::class.java]
        resultViewModel = ViewModelProvider(this)[ResultViewModel::class.java]

        println("BrowseActivity 0")
        InterAds.preloadInterAds(
            this@BrowseActivity,
            alias = InterAds.ALIAS_INTER_DOWNLOAD,
            adUnit = InterAds.INTER_AD1
        )


        if (urlNew.isEmpty()) {
            Toast.makeText(this, "Invalid URL", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.apply {
            homePage.setOnClickListener {
                InterAds.showPreloadInter(this@BrowseActivity, alias = InterAds.ALIAS_INTER_DOWNLOAD, {
                    finish()
                }, {
                    finish()
                })
            }
            topSearchBar.setText(urlNew).also {
              updateTabValue(urlNew)
            }
            setupWebView()
            val result = formatUrl(urlNew)
            println("BrowseActivity: $result")
            webView.loadUrl(result)

            refreshBtn.setOnClickListener {
                webView.loadUrl(result)
            }

            downloadBtn.setOnClickListener {
                resultViewModel.getFilteredList().observe(this@BrowseActivity) { items ->
                    kotlin.runCatching {
//                recentlySearch?.isVisible = it.isNotEmpty()
                        items.onEach {
                            println("showSingleDownloadSheet 4: $it")
                        }
                        if(resultViewModel.repository.itemCount.value > 1 || resultViewModel.repository.itemCount.value == -1){
                            println("It is here 123")
                        }else if (resultViewModel.repository.itemCount.value == 1){
                            if (sharedPreferences!!.getBoolean("download_card", true)){
                                if(items.size == 1 ){

                                    showSingleDownloadSheet(
                                        items[0],
                                        DownloadViewModel.Type.valueOf(sharedPreferences!!.getString("preferred_download_type", "video")!!)
                                    )
                                }
                            }
                        }
                    }
                }
                startSearchFacebookURL()
            }

            binding.topSearchBar.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {

                    val query = binding.topSearchBar.text.toString().trim()

                    if (query.isNotEmpty()) {
                        webView.loadUrl(result).also {
                            if(MainActivity.currentTabPosition >= 0) {
                                updateTabValue(result)
                            }
                        }
                    }

                    true // consume the action
                } else {
                    false
                }
            }
        }
    }

    private fun updateTabValue(urlNew: String) {
        val position = MainActivity.currentTabPosition
        println("currentTabPosition 1: $position")
        if(position >= 0) {
            val allTabs = TabViewModel.getAllTabs(this@BrowseActivity).toMutableList()
            allTabs[position] = urlNew
            TabViewModel.addNewTab(this@BrowseActivity, allTabs)
        }
    }

    private fun startSearchFacebookURL() {
        binding.loading.visible()
        lifecycleScope.launch(Dispatchers.IO){
            resultViewModel.deleteAll()

            val check1 = sharedPreferences!!.getBoolean("quick_download", false)
            val check2 = sharedPreferences!!.getString("preferred_download_type", "video") == "command"
            println("startSearchFacebookURL is ${queryList}")
            val check3 = Patterns.WEB_URL.matcher(queryList.first()).matches()
            val check4 = sharedPreferences!!.getBoolean("download_card", true)
            println("startSearch 1: $check1 and $check2 and $check3 and ${queryList.size}")
            if(check1|| check2){
                if (queryList.size == 1 && check3 ){
                    println("startSearch 2: $check4")
                    if (check4) {
                        withContext(Dispatchers.Main){
                            println("showSingleDownloadSheet 6")
                            showSingleDownloadSheet(
                                resultItem = downloadViewModel.createEmptyResultItem(queryList.first()),
                                type = DownloadViewModel.Type.valueOf(sharedPreferences!!.getString("preferred_download_type", "video")!!)
                            )
                            binding.loading.gone()
                        }
                    } else {
                        val downloadItem = downloadViewModel.createDownloadItemFromResult(
                            result = downloadViewModel.createEmptyResultItem(queryList.first()),
                            givenType = DownloadViewModel.Type.valueOf(sharedPreferences!!.getString("preferred_download_type", "video")!!)
                        )
                        downloadViewModel.queueDownloads(listOf(downloadItem))
                        withContext(Dispatchers.Main) {
                            binding.loading.gone()
                        }
                    }

                }else{
                    resultViewModel.parseQueries(queryList){
                        lifecycleScope.launch {
                            withContext(Dispatchers.Main) {
                                binding.loading.gone()
                            }
                        }
                    }

                }
            }else{
                resultViewModel.parseQueries(queryList){
                    lifecycleScope.launch {
                        withContext(Dispatchers.Main) {
                            binding.loading.gone()
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun showSingleDownloadSheet(
        resultItem: ResultItem,
        type: DownloadViewModel.Type,
        disableUpdateData : Boolean = false
    ){
        val intent = Intent(this@BrowseActivity, DownloadInfoActivity::class.java)

        intent.putExtra("result", resultItem)
        intent.putExtra("facebookURL", "BrowseActivity")
        intent.putExtra("isFromFB", false)
        intent.putExtra("type", downloadViewModel.getDownloadType(type, resultItem.url))
        if (disableUpdateData) {
            intent.putExtra("disableUpdateData", true)
        }

        startActivity(intent)
    }

    private fun setupWebView() = binding.webView.apply {
        println("urlNew: $urlNew")
        settings.apply {
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = false
            loadsImagesAutomatically = true
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false


            userAgentString = if(urlNew.contains("facebook", true)) {
                DESKTOP_URGENT
            } else {
                MOBILE_URGENT
            }


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }
        }

        // For navigation and error handling
        webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                videoDetected = false
                binding.progressBar.visibility = View.VISIBLE
                showLoadingAnimation()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.progressBar.visibility = View.GONE
                hideLoadingAnimation()
                injectVideoDetectionJS()
            }

            override fun onReceivedError(
                view: WebView?, request: WebResourceRequest?, error: WebResourceError?
            ) {
                Log.e("WebViewError", "Error loading page: ${error?.description}")
//                Toast.makeText(context, "Failed to load page", Toast.LENGTH_SHORT).show()
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val newUrl = request?.url.toString()
                binding.topSearchBar.setText(newUrl)   // 🔥 Update your TextView with the new URL
                updateTabValue(newUrl)
                view?.loadUrl(newUrl)
                return true
            }
        }

        // Enable fullscreen video support
        webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (customView != null) {
                    callback?.onCustomViewHidden()
                    return
                }

                customView = view
                customViewCallback = callback
                originalSystemUiVisibility = window.decorView.systemUiVisibility

                (window.decorView as FrameLayout).addView(
                    view,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )

                window.decorView.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            }

            override fun onHideCustomView() {
                (window.decorView as FrameLayout).removeView(customView)
                customView = null
                window.decorView.systemUiVisibility = originalSystemUiVisibility
                customViewCallback?.onCustomViewHidden()
            }
        }
    }


    private fun showLoadingAnimation() {
        binding.downloadBtn.setAnimation(R.raw.animation1) // your loading animation
        binding.downloadBtn.visibility = View.VISIBLE
        binding.downloadBtn.playAnimation()
    }

    private fun hideLoadingAnimation() {
        binding.downloadBtn.cancelAnimation()
        binding.downloadBtn.visibility = View.GONE
    }

    private fun showVideoDetectedAnimation() {
        binding.downloadBtn.setAnimation(R.raw.animation2) // your loading animation
        binding.downloadBtn.visibility = View.VISIBLE
        binding.downloadBtn.playAnimation()
    }

    private fun injectVideoDetectionJS() {
        val js = """
        (function() {
            function notifyIfVideoExists() {
                var videos = document.getElementsByTagName('video');
                if (videos.length > 0) {
                    var src = videos[0].src || (videos[0].querySelector('source')?.src);
                    if (src) {
                        Android.onVideoDetected(src);
                    }
                    return;
                }

                var links = document.getElementsByTagName('a');
                for (var i = 0; i < links.length; i++) {
                    if (links[i].href && links[i].href.match(/\.(mp4|mov|m3u8|youtube\.com|vimeo\.com)/i)) {
                        Android.onVideoDetected(links[i].href);
                        return;
                    }
                }
            }

            var observer = new MutationObserver(function(mutations) {
                notifyIfVideoExists();
            });

            observer.observe(document.body, { childList: true, subtree: true });

            // Initial scan
            notifyIfVideoExists();
        })();
    """.trimIndent()

        binding.webView.evaluateJavascript(js, null)
    }

    
    override fun onBackPressed() {
        when {
            customView != null -> binding.webView.webChromeClient?.onHideCustomView()
            binding.webView.canGoBack() -> binding.webView.goBack()
            else -> super.onBackPressed()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onResume() {
        super.onResume()
        Log.d(
            TAG,
            "Banner Conditions: ${RemoteConfig.BANNER_ALL_2} and ${RemoteConfig.ADS_DISABLE_2}"
        )
        if (RemoteConfig.BANNER_ALL_2 == "0" || RemoteConfig.ADS_DISABLE_2 == "0") {
            binding.frBanner.root.gone()
        } else {
            loadBanner(this, BANNER_HOME)
        }


        if (!jsInterfaceAdded) {
            binding.webView.addJavascriptInterface(object {
                @JavascriptInterface
                fun onVideoDetected(url: String) {
                    println("addJavascriptInterface: $url")
                    runOnUiThread {
                        if (!videoDetected) {
                            showVideoDetectedAnimation()
                        }

                        if (!queryList.contains(url)) {
                            queryList.clear() // Optional: keep only the latest video
                            queryList.add(url)
                            println("Video URL added to queryList: $url")
                        }
                    }
                }
            }, "Android")
            jsInterfaceAdded = true
        }
    }


    companion object {
        val DESKTOP_URGENT =  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.5993.90 Safari/537.36"
        val MOBILE_URGENT =   "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.5993.90 Mobile Safari/537.36"
        private val TAG = BrowseActivity::class.java.simpleName

        fun formatUrl(url: String): String {
            return when {
                URLUtil.isValidUrl(url) -> url
                url.contains(".com", ignoreCase = true) -> "https://$url"
                else -> "https://www.google.com/search?q=$url"
            }
        }
    }
}