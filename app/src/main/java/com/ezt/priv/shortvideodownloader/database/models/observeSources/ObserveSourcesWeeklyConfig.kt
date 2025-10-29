package com.ezt.priv.shortvideodownloader.database.models.observeSources

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ObserveSourcesWeeklyConfig(
    var weekDays: List<Int>
) : Parcelable