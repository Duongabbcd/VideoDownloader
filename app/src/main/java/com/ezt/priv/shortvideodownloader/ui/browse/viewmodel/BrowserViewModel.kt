package com.ezt.priv.shortvideodownloader.ui.browse.viewmodel

import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import com.ezt.priv.shortvideodownloader.database.viewmodel.SettingsViewModel
import com.ezt.priv.shortvideodownloader.ui.browse.HOME_TAB_INDEX
import com.ezt.priv.shortvideodownloader.ui.browse.detector.SingleLiveEvent
import com.ezt.priv.shortvideodownloader.ui.browse.scheduler.BaseViewModel
import com.ezt.priv.shortvideodownloader.ui.browse.webtab.WebTab
import javax.inject.Inject

//@OpenForTesting
class BrowserViewModel @Inject constructor() : BaseViewModel() {

    companion object {
        const val SEARCH_URL = "https://duckduckgo.com/?t=ffab&q=%s"

        var instance: BrowserViewModel? = null
    }

    var settingsModel: SettingsViewModel? = null

    val openPageEvent = SingleLiveEvent<WebTab>()

    val closePageEvent = SingleLiveEvent<WebTab>()

    val selectWebTabEvent = SingleLiveEvent<WebTab>()

    val updateWebTabEvent = SingleLiveEvent<WebTab>()

    val workerM3u8MpdEvent = MutableLiveData<DownloadButtonState>()

    val workerMP4Event = MutableLiveData<DownloadButtonState>()

    val progress = ObservableInt(0)

    val tabs = ObservableField(listOf(WebTab.HOME_TAB))

    val currentTab = ObservableInt(HOME_TAB_INDEX)

    override fun start() {
        instance = this
    }

    override fun stop() {
        instance = null
    }
}