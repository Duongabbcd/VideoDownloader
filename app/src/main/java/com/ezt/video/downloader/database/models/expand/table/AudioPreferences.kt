package com.ezt.video.downloader.database.models.expand.table

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AudioPreferences(
    var embedThumb: Boolean = true,
    var cropThumb: Boolean? = null,
    var splitByChapters: Boolean = false,
    var sponsorBlockFilters: ArrayList<String> = arrayListOf(),
    var bitrate: String = ""
) : Parcelable
