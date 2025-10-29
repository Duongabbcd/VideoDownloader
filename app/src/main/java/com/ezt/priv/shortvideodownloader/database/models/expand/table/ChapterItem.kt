package com.ezt.priv.shortvideodownloader.database.models.expand.table

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize


@Parcelize
data class ChapterItem(
    @SerializedName(value = "start_time")
    var start_time: Long,
    @SerializedName(value = "end_time")
    var end_time: Long,
    @SerializedName(value = "title")
    var title: String,
) : Parcelable