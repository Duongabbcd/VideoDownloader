package com.ezt.priv.shortvideodownloader.ui.downloadcard

import com.ezt.priv.shortvideodownloader.database.models.main.ResultItem

interface GUISync {
    fun updateTitleAuthor(t: String, a: String)
    fun updateUI(res: ResultItem?)
}