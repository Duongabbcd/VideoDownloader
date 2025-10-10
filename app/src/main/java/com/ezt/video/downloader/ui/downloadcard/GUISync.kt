package com.ezt.video.downloader.ui.downloadcard

import com.ezt.video.downloader.database.models.main.ResultItem

interface GUISync {
    fun updateTitleAuthor(t: String, a: String)
    fun updateUI(res: ResultItem?)
}