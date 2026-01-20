package com.ezt.priv.shortvideodownloader.ads

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.ads.AdmobUtils.isNetworkConnected
import com.google.android.gms.ads.AdValue

object AdsManager {
    var isDebug = true
    var isTestDevice = false


    var countClickVideo = 0

}