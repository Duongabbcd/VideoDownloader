package com.ezt.video.downloader.database.models.expand.non_table

import android.os.Parcelable
import com.ezt.video.downloader.database.models.main.DownloadItem
import kotlinx.parcelize.Parcelize

@Parcelize
data class AlreadyExistsItem(
    var downloadItem: DownloadItem,
    var historyID: Long? = null
) : Parcelable
