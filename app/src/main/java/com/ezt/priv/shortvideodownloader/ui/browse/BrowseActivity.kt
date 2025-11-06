package com.ezt.priv.shortvideodownloader.ui.browse

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.URLUtil
import android.webkit.WebChromeClient.CustomViewCallback
import android.widget.Toast
import androidx.activity.viewModels
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.ezt.priv.shortvideodownloader.ads.RemoteConfig
import com.ezt.priv.shortvideodownloader.ads.type.BannerAds.BANNER_HOME
import com.ezt.priv.shortvideodownloader.ads.type.InterAds
import com.ezt.priv.shortvideodownloader.database.models.expand.table.ChapterItem
import com.ezt.priv.shortvideodownloader.database.models.expand.table.Format
import com.ezt.priv.shortvideodownloader.database.models.main.ResultItem
import com.ezt.priv.shortvideodownloader.database.viewmodel.DownloadViewModel
import com.ezt.priv.shortvideodownloader.database.viewmodel.HistoryViewModel
import com.ezt.priv.shortvideodownloader.database.viewmodel.ResultViewModel
import com.ezt.priv.shortvideodownloader.databinding.ActivityBrowseBinding
import com.ezt.priv.shortvideodownloader.ui.BaseActivity2
import com.ezt.priv.shortvideodownloader.ui.browse.detector.SingleLiveEvent
import com.ezt.priv.shortvideodownloader.ui.browse.qualifier.VideFormatEntityList
import com.ezt.priv.shortvideodownloader.ui.browse.qualifier.VideoInfo
import com.ezt.priv.shortvideodownloader.ui.browse.viewmodel.BrowserViewModel
import com.ezt.priv.shortvideodownloader.ui.browse.viewmodel.DownloadButtonState
import com.ezt.priv.shortvideodownloader.ui.browse.viewmodel.MainViewModel
import com.ezt.priv.shortvideodownloader.ui.browse.viewmodel.SettingViewModel
import com.ezt.priv.shortvideodownloader.ui.browse.webtab.WebTab
import com.ezt.priv.shortvideodownloader.ui.home.MainActivity
import com.ezt.priv.shortvideodownloader.ui.home.MainActivity.Companion.loadBanner
import com.ezt.priv.shortvideodownloader.ui.info.DownloadInfoActivity
import com.ezt.priv.shortvideodownloader.ui.tab.viewmodel.TabViewModel
import com.ezt.priv.shortvideodownloader.util.Common.gone
import com.ezt.priv.shortvideodownloader.util.Utils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BrowseActivity : BaseActivity2<ActivityBrowseBinding>(ActivityBrowseBinding::inflate), BrowserServicesProvider,OnDownloadMediaFileListener {
    private lateinit var resultViewModel : ResultViewModel
    private lateinit var downloadViewModel : DownloadViewModel
    lateinit var mainViewModel: MainViewModel

    private var sharedPreferences: SharedPreferences? = null

    private val urlNew by lazy {
        intent.getStringExtra("receivedURL") ?: ""
    }

    private lateinit var browserViewModel: BrowserViewModel

    private var customView: View? = null
    private var customViewCallback: CustomViewCallback? = null
    private var originalSystemUiVisibility = 0

    private var jsInterfaceAdded = false
    private var queryList = mutableListOf<String>()
    private var videoDetected = false

//    lateinit var proxiesViewModel: ProxiesViewModel

    val settingsViewModel: SettingViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        downloadViewModel = ViewModelProvider(this)[DownloadViewModel::class.java]
        resultViewModel = ViewModelProvider(this)[ResultViewModel::class.java]

        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
        browserViewModel = ViewModelProvider(this)[BrowserViewModel::class.java]

        sendURL = urlNew
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
            openFragment(WebTabFragment.newInstance().apply {
                val args = Bundle().apply {
                    putInt(TAB_INDEX_KEY, 0)
                }
                arguments = args
            })
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


    @SuppressLint("SetJavaScriptEnabled")
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
    }



    override fun getOpenTabEvent(): SingleLiveEvent<WebTab> {
        return browserViewModel.openPageEvent
    }

    override fun getCloseTabEvent(): SingleLiveEvent<WebTab> {
        return browserViewModel.closePageEvent
    }

    override fun getUpdateTabEvent(): SingleLiveEvent<WebTab> {
        return browserViewModel.updateWebTabEvent
    }

    override fun getTabsListChangeEvent(): ObservableField<List<WebTab>> {
        return browserViewModel.tabs
    }

    override fun getPageTab(position: Int): WebTab {
        val list = browserViewModel.tabs.get() ?: listOf(WebTab.HOME_TAB)
        return if (position in list.indices) list[position] else WebTab("error", "error")
    }

    override fun getHistoryVModel(): HistoryViewModel {
        TODO("Not yet implemented")
    }

    override fun getWorkerM3u8MpdEvent(): MutableLiveData<DownloadButtonState> {
        return browserViewModel.workerM3u8MpdEvent
    }

    override fun getWorkerMP4Event(): MutableLiveData<DownloadButtonState> {
        return browserViewModel.workerMP4Event
    }

    override fun getCurrentTabIndex(): ObservableInt {
        return browserViewModel.currentTab
    }


    override fun onStart() {
        super.onStart()
        browserViewModel.start()
    }


    override fun onStop() {
        super.onStop()
        browserViewModel.stop()
    }

    override fun onDownloadMediaFile(resultItem: ResultItem) {
        showSingleDownloadSheet(resultItem, type = DownloadViewModel.Type.video)
    }

    companion object {
        val DESKTOP_URGENT =  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.5993.90 Safari/537.36"
        val MOBILE_URGENT =   "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.5993.90 Mobile Safari/537.36"
        private val TAG = BrowseActivity::class.java.simpleName
        var sendURL = ""
        fun formatUrl(url: String): String {
            return when {
                URLUtil.isValidUrl(url) -> url
                url.contains(".com", ignoreCase = true) -> "https://$url"
                else -> "https://www.google.com/search?q=$url"
            }
        }

        fun VideoInfo.toResultItem() : ResultItem {
            return ResultItem(
                id = 0L,
                url = downloadUrls.first().url.toString(),
                title = title,
                author = downloadUrls.first().url.host,
                duration = Utils.formatDuration(duration),
                thumb = thumbnail,
                website = downloadUrls.first().url.host,
                playlistTitle = "",
                formats = formats.toDownloadableFormats(),
                urls = "",
                chapters = mutableListOf<ChapterItem>(),
                playlistURL = "",
                playlistIndex = 0,
                creationTime = 0L,
                availableSubtitles = listOf()
            )
        }

        fun VideFormatEntityList.toDownloadableFormats() : MutableList<Format> {
            return this.formats.map { it ->
                Format(
                    format_id = it.formatId.toString(),
                    container = it.ext ?: "mp4",
                    vcodec = it.vcodec ?: "",
                    acodec = it.acodec ?: "",
                    encoding = "",
                    filesize = it.fileSize,
                    tbr = it.tbr.toString(),
                    fps = it.fps.toString(),
                    asr = it.asr.toString(),
                    format_note = "${it.width}x${it.height}",
                    url = it.url ?: ""
                )
            }.toMutableList()
        }
    }

}