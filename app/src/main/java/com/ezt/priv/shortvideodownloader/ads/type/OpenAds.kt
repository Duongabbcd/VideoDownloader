package com.ezt.priv.shortvideodownloader.ads.type

import android.app.Activity
import android.util.Log
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAdRevenue
import com.adjust.sdk.AdjustConfig
import com.ezt.priv.shortvideodownloader.BuildConfig
import com.ezt.priv.shortvideodownloader.MyApplication
import com.ezt.priv.shortvideodownloader.ads.RemoteConfig
import com.ezt.priv.shortvideodownloader.ads.helper.Prefs
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date


object OpenAds {

    private const val OPEN_TEST_ID = "ca-app-pub-3607148519095421/8324452420"

    private const val OPEN_ID_DEFAULT = OPEN_TEST_ID

    private var appOpenAd: AppOpenAd? = null
    private var isOpenShowingAd = false
    val disableClasses: ArrayList<Class<*>> = arrayListOf()

    private var loadTimeOpenAd: Long = 0

    fun initOpenAds(context: Activity, callback: (Boolean) -> Unit) {
        if (RemoteConfig.ADS_DISABLE == "0" || RemoteConfig.AD_OPEN_APP == "0") {
            callback(false)
            return
        }
        val placement =  context::class.java.simpleName
//        val mode = analyticsLogger.getCurrentModeByScreen(placement)
//        analyticsLogger.updateUserProperties(context, placement ,mode)

        val now = System.currentTimeMillis()
        println("initOpenAds: $appOpenAd and isOpenAdsCanUse:  ${isOpenAdsCanUse()}")
        if (appOpenAd == null || !isOpenAdsCanUse()) {
            appOpenAd = null
            AppOpenAd.load(
                context,
                if (BuildConfig.DEBUG) OPEN_TEST_ID else OPEN_ID_DEFAULT,
                getAdRequest(),
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        appOpenAd = ad
                        val adapterClassName =
                            appOpenAd?.responseInfo?.mediationAdapterClassName ?: ""
                        println("onAdLoaded: $appOpenAd and $ad")
                        appOpenAd?.setOnPaidEventListener { adValue ->
                            try {
                                MyApplication.initROAS(adValue.valueMicros, adValue.currencyCode)

                                val adRevenue =
                                    AdjustAdRevenue(AdjustConfig.AD_REVENUE_ADMOB)
                                val adReceivedAmount: Double = adValue.valueMicros / 1_000_000.0
                                val adCurrencyCode = adValue.currencyCode
                                adRevenue.setRevenue(
                                    adReceivedAmount,
                                    adCurrencyCode
                                )
                                Adjust.trackAdRevenue(adRevenue)
                                println("OpenAds initOpenAds onAdLoaded: ${adValue.valueMicros} $adReceivedAmount and $adCurrencyCode")
//                                analyticsLogger.logAdImpression(
//                                    "open",
//                                    context,
//                                    adapterClassName,
//                                    adReceivedAmount,
//                                    adCurrencyCode
//                                )
                            } catch (e: Exception) {
                                callback.invoke(false)
                                e.printStackTrace()
                            }
                        }
                        val loadingTime = System.currentTimeMillis() - now
//                        analyticsLogger.logAdRequest(
//                            "open",
//                            context::class.java.simpleName,
//                            adapterClassName,
//                            1,
//                            loadingTime
//                        )

                        loadTimeOpenAd = Date().time
                        callback.invoke(true)
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        println("initOpenAds onAdFailedToLoad: $loadAdError")
                        val adapterClassName =
                            appOpenAd?.responseInfo?.mediationAdapterClassName ?: ""
                        appOpenAd = null
                        val loadingTime = System.currentTimeMillis() - now
//                        analyticsLogger.logAdRequest(
//                            "open",
//                            context::class.java.simpleName,
//                            adapterClassName,
//                            0,
//                            loadingTime
//                        )
                        callback.invoke(false)
                    }
                }
            )
        } else {
            callback.invoke(false)
        }
    }

    private fun isOpenAdsCanUse(): Boolean {
        val dateDifference = Date().time - loadTimeOpenAd
        val numMilliSecondsPerHour = 3600000
        return dateDifference < numMilliSecondsPerHour * 4
    }

    private fun getAdRequest(): AdRequest {
        return AdRequest.Builder().build()
    }

    fun isCanShowOpenAds(): Boolean {
        val result = appOpenAd != null && !isOpenShowingAd
        println("isCanShowOpenAds: $appOpenAd and $isOpenShowingAd")
        return result
    }

    fun showOpenAds(context: Activity, callback: () -> Unit) {
        try {
            val prefs = Prefs(MyApplication.instance)
            if (prefs.premium || prefs.isRemoveAd) {
                callback.invoke()
                return
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val now = System.currentTimeMillis()
        val condition = System.currentTimeMillis() - lastTimeShowAds > 10_000L
        println("showOpenAds: $condition and ${isCanShowOpenAds()}")
        if (condition) {
            if (isCanShowOpenAds()) {
                appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.d("OpenAds", "onAdFailedToShowFullScreenContent")
                        appOpenAd = null
                        callback.invoke()
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d("OpenAds", "onAdShowedFullScreenContent")
                        isOpenShowingAd = true
                    }

                    override fun onAdDismissedFullScreenContent() {
                        Log.d("OpenAds", "onAdDismissedFullScreenContent")
                        val placement =  context::class.java.simpleName
//                        val mode = analyticsLogger.getCurrentModeByScreen(placement)
//                        analyticsLogger.updateUserProperties(context, placement ,mode)


                        isOpenShowingAd = false
                        appOpenAd = null
                        initOpenAds(context) {

                        }
                        InterAds.startDelay()
                        startDelay()
                        callback.invoke()

                    }
                }
                appOpenAd?.show(context)
            } else {
                callback.invoke()

            }
        } else {
            callback.invoke()

        }
    }

    fun resetOpenAds() {
        appOpenAd = null
    }


    private var lastTimeShowAds = 0L

    fun startDelay() {
        lastTimeShowAds = System.currentTimeMillis()
    }

    fun disableAdsOpenForActivity(activityClass: Class<*>) {
        if (!disableClasses.contains(activityClass)) {
            disableClasses.add(activityClass)
        }
    }

    fun enableAdsOpenForActivity(activityClass: Class<*>) {
        disableClasses.remove(activityClass)
    }
}