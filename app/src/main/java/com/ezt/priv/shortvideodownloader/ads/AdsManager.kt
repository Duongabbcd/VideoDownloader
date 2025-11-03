package com.ezt.priv.shortvideodownloader.ads

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.admob.max.dktlibrary.AdmobUtils
import com.admob.max.dktlibrary.utils.admod.NativeHolderAdmob
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.ads.AdmobUtils.isNetworkConnected
import com.google.android.gms.ads.AdValue

object AdsManager {
    var isDebug = true
    var isTestDevice = false


    var countClickVideo = 0

    fun showNativeFullScreen(
        context: Context,
        nativeHolder: NativeHolderAdmob,
        view: ViewGroup,
        isCheckTestDevice: Boolean = false
    ) {
        println("showNativeFullScreen: $isCheckTestDevice and ${isTestDevice}")
        if (isTestDevice || isCheckTestDevice) {
            view.visibility = View.GONE
            return
        }
        if (isNetworkConnected(context)) {
            view.visibility = View.GONE
            return
        }
        if(isCheckTestDevice) {
            view.visibility = View.GONE
            return
        }

        AdmobUtils.showNativeFullScreenAdsWithLayout(
            context as Activity,
            nativeHolder,
            view,
            R.layout.ad_template_native_fullscreen,
            object :
                AdmobUtils.AdsNativeCallBackAdmod {
                override fun NativeFailed(massage: String) {
                    println("NativeFailed: $massage")
                }

                override fun NativeLoaded() {
                    view.visibility = View.VISIBLE
                }

                override fun onPaid(adValue: AdValue?, adUnitAds: String?) {
                    //do nothing
                }
            })
    }
}