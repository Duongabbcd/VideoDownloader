package com.ezt.priv.shortvideodownloader.database.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.ezt.priv.shortvideodownloader.database.VideoDownloadDB
import com.ezt.priv.shortvideodownloader.database.dao.CommandTemplateDao
import com.ezt.priv.shortvideodownloader.database.models.main.DownloadItem
import com.ezt.priv.shortvideodownloader.util.extractors.ytdlp.YTDLPUtil


class YTDLPViewModel(private val application: Application) : AndroidViewModel(application) {
    private val dbManager: VideoDownloadDB
    private val commandTemplateDao: CommandTemplateDao
    private val ytdlpUtil: YTDLPUtil

    init {
        dbManager =  VideoDownloadDB.getInstance(application)
        commandTemplateDao = VideoDownloadDB.getInstance(application).commandTemplateDao
        ytdlpUtil = YTDLPUtil(application, commandTemplateDao)
    }

    fun parseYTDLRequestString(item: DownloadItem) : String {
        val req = ytdlpUtil.buildYoutubeDLRequest(item)
        return ytdlpUtil.parseYTDLRequestString(req)
    }

    fun getVersion(channel: String) : String {
        return ytdlpUtil.getVersion(application, channel)
    }
}