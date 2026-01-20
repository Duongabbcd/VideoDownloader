package com.ezt.priv.shortvideodownloader.ads.type

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.Window
import android.widget.LinearLayout
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAdRevenue
import com.adjust.sdk.AdjustConfig
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.BuildConfig
import com.ezt.priv.shortvideodownloader.MyApplication
import com.ezt.priv.shortvideodownloader.ads.AdmobUtils
import com.ezt.priv.shortvideodownloader.ads.AdsManager.countClickVideo
import com.ezt.priv.shortvideodownloader.ads.AdsManager.isTestDevice
import com.ezt.priv.shortvideodownloader.ads.RemoteConfig
import com.ezt.priv.shortvideodownloader.ads.helper.Prefs
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.Date


object InterAds {
    val TIME_DELAY = if (BuildConfig.DEBUG) 10_000L else 40_000L
    const val INTER_AD1 = "ca-app-pub-3607148519095421/3847534257"

    const val INTER_SPLASH ="ca-app-pub-3607148519095421/5272296564"

    const val INTER_TEST_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val INTER_ID_DEFAULT = "your-ad-id"

    const val ALIAS_INTER_DOWNLOAD = "alias_inter_download"
    const val ALIAS_INTER_RINGTONE = "alias_inter_ringtone"
    const val ALIAS_INTER_WALLPAPER = "alias_inter_wallpaper"
    const val ALIAS_INTER_CALLSCREEN = "alias_inter_callscreen"
    const val ALIAS_INTER_SPLASH = "alias_inter_splash"

    var currentTimeMillis = 0L
    var isMoreThan40Seconds = false

    private var adObserver = MutableLiveData<InterstitialAd?>(null)

    private var mInterstitialAd: InterstitialAd?
        get() = adObserver.value
        set(value) {
            adObserver.value = value
        }

    var interPreloadMap = mutableMapOf<String, MutableLiveData<InterAdWrapper>>()

    class InterAdWrapper(var intersAd: InterstitialAd?) {
        var state: Int = -1
        var adId: String? = null
    }


    private var isLoading = false
    private var isShowing = false
    private var loadTimeAd: Long = 0

    private var loadingDialog: Dialog? = null

    private fun getAdRequest(): AdRequest = AdRequest.Builder().build()

    private fun isCanLoadAds(): Boolean =
        !isLoading && !isShowing && (mInterstitialAd == null || isAdsOverdue())

    fun isCanShowAds(): Boolean {
        println("isCanShowAds: isLoading $isLoading  isShowing $isShowing isInDelayTime() ${isInDelayTime()} and ${mInterstitialAd == null}")
        val result = !isLoading && !isShowing && !isInDelayTime() && mInterstitialAd != null
        return result
    }

    fun isCanShowAds2(interAdWrapper: InterAdWrapper): Boolean {
        println("isCanShowAds: isLoading $isLoading  isShowing $isShowing  and ${interAdWrapper.intersAd != null}")
        val result = !isLoading && !isShowing && interAdWrapper.intersAd != null
        return result
    }


    fun isInDelayTime(): Boolean = System.currentTimeMillis() - lastTimeShowAds < TIME_DELAY

    private fun isCanShowAdsIgnoreDelay(): Boolean =
        !isLoading && !isShowing && mInterstitialAd != null && !isAdsOverdue()

    private fun isAdsOverdue(): Boolean {
        val dateDifference = Date().time - loadTimeAd
        return dateDifference > 4 * 3600000L
    }

    private fun showSelectedAds(
        activity: FragmentActivity,
        interAdWrapper: InterAdWrapper,
        callback: (Boolean) -> Unit,
        needLoadAfterShow: Boolean = true
    ) {
        println("showSelectedAds: ${activity.isDestroyed} and ${activity.isFinishing}")

        try {
            val prefs = Prefs(MyApplication.instance)
            if (prefs.premium || prefs.isRemoveAd) {
                callback.invoke(false)
                return
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        println("isCanShowAds: ${isCanShowAds2(interAdWrapper)}")
        if (isCanShowAds2(interAdWrapper)) {
            println("isCanShowAds: ${interAdWrapper.state} and ${interAdWrapper.adId}")
            try {
                showSelectedAdsFull(
                    activity,
                    interAdWrapper,
                    callback,
                    needLoadAfterShow = needLoadAfterShow
                )
            } catch (e: Exception) {
                e.printStackTrace()
                callback.invoke(false)

            }
        } else {
            callback.invoke(false)
        }
    }

    private fun showDialog(activity: Activity) {
        try {
            if (loadingDialog == null) {
                loadingDialog = Dialog(activity).apply {
                    requestWindowFeature(Window.FEATURE_NO_TITLE)
                    setContentView(R.layout.ads_dialog_loading)
                    setCancelable(false)
                    window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    window?.setLayout(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
            }
            if (!activity.isDestroyed && !activity.isFinishing && loadingDialog?.isShowing == false) {
                loadingDialog?.show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }


    fun dismissAdDialog() {
        loadingDialog?.takeIf { it.isShowing }?.dismiss()
    }

    private fun showSelectedAdsFull(
        context: Activity,
        interAdWrapper: InterAdWrapper,
        callback: (Boolean) -> Unit,
        needLoadAfterShow: Boolean = true
    ) {
        val now = System.currentTimeMillis()
        val adapterClassName =
            interAdWrapper.intersAd?.responseInfo?.mediationAdapterClassName ?: ""
        interAdWrapper.intersAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                interAdWrapper.intersAd = null
                isShowing = false
                callback.invoke(false)
                println("onAdFailedToShowFullScreenContent is here")
//                dismissAdDialog()
            }

            override fun onAdShowedFullScreenContent() {
//                dismissAdDialog()
                println("onAdShowedFullScreenContent is here")
                isShowing = true
            }

            override fun onAdDismissedFullScreenContent() {
                println("onAdDismissedFullScreenContent is here")
                val placement =  context::class.java.simpleName
//                val mode = analyticsLogger.getCurrentModeByScreen(placement)
//                analyticsLogger.updateUserProperties(context, placement ,mode)
                callback.invoke(true)

                isShowing = false
                interAdWrapper.intersAd = null
                startDelay()


                // Preload next ad asynchronously
                Handler(Looper.getMainLooper()).post {
                    if (needLoadAfterShow) {
                        println("Ad preloaded after screen transition: $currentAlias and $currentAdUnit")
                        preloadInterAds(context, currentAlias, currentAdUnit)
                    } else {
                        println("Ad preloaded after screen transition: $needLoadAfterShow")
                    }
                }


            }
        }
        interAdWrapper.intersAd?.show(context)
    }

    fun isShowing(): Boolean = isShowing

    private var lastTimeShowAds = 0L

    fun startDelay() {
        lastTimeShowAds = System.currentTimeMillis()
    }

    private var currentAlias = ""
    private var currentAdUnit = ""

    fun preloadInterAds(context: Activity, alias: String, adUnit: String) {
        println("RemoteConfig.ADS_DISABLE 123: ${RemoteConfig.ADS_DISABLE}")
        if (RemoteConfig.ADS_DISABLE == "0") {
            return
        }

        currentAlias = alias
        currentAdUnit = adUnit
        val mutableLiveData = MutableLiveData(InterAdWrapper(null))
        interPreloadMap[alias] = mutableLiveData

        val wrapper = interPreloadMap[alias]?.value
        if (Prefs(MyApplication.instance).premium || Prefs(MyApplication.instance).isRemoveAd) {
            wrapper?.state = -1
            interPreloadMap[alias]?.postValue(wrapper)
            return
        }

        val interAdId = adUnit
        wrapper?.apply {
            state = 0
            adId = interAdId
        }

        val now = System.currentTimeMillis()
        val divide = (now - currentTimeMillis) / 1000

        isMoreThan40Seconds = divide >= RemoteConfig.INTER_SPACE.toInt()
        println("preloadInterAds - isMoreThan40Seconds: $divide and $isMoreThan40Seconds")

        when (alias) {
            ALIAS_INTER_DOWNLOAD -> {
                println("Interstitial condition download:  $countClickVideo")
                println("preloadInterAds wrapper download: ${context.javaClass.simpleName} and $wrapper")
                if (RemoteConfig.INTER_ALL == "0" || isTestDevice) {
                    interPreloadMap.remove(alias)
                    return
                }

                if (countClickVideo == 0) {
                    interPreloadMap[alias]?.postValue(wrapper)
                } else {
                    println("Interstitial condition download: $isMoreThan40Seconds and ${countClickVideo >= RemoteConfig.INTER_ALL.toInt()}")

                    if (isMoreThan40Seconds && countClickVideo >= RemoteConfig.INTER_ALL.toInt()) {

                        interPreloadMap[alias]?.postValue(wrapper)
                    } else {
                        interPreloadMap.remove(alias)
                        return
                    }
                }

            }
        }

        val placement =  context::class.java.simpleName
//        val mode = analyticsLogger.getCurrentModeByScreen(placement)
//
//        analyticsLogger.updateUserProperties(context, placement ,mode)

        InterstitialAd.load(
            context,
            if (BuildConfig.DEBUG) INTER_TEST_ID else interAdId,
            getAdRequest(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interAd: InterstitialAd) {
                    mInterstitialAd = interAd
                    val adapterClassName = interAd.responseInfo.mediationAdapterClassName ?: ""
                    println("InterAds adapterClassName: $adapterClassName")
                    interAd.onPaidEventListener = OnPaidEventListener { adValue ->
                        try {
                            MyApplication.initROAS(adValue.valueMicros, adValue.currencyCode)
                            val adRevenue = AdjustAdRevenue(AdjustConfig.AD_REVENUE_ADMOB)
                            val adReceivedAmount: Double = adValue.valueMicros / 1_000_000.0
                            val adCurrencyCode = adValue.currencyCode
                            adRevenue.setRevenue(
                                adReceivedAmount,
                                adCurrencyCode
                            )
                            println("InterAds preloadInterAds onAdLoaded: ${adValue.valueMicros} $adReceivedAmount and $adCurrencyCode")
                            Adjust.trackAdRevenue(adRevenue)
//                            analyticsLogger.logAdImpression(
//                                "inter",
//                                context,
//                                adapterClassName,
//                                adReceivedAmount,
//                                adCurrencyCode
//                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    val loadingTime = System.currentTimeMillis() - now
//                    analyticsLogger.logAdRequest(
//                        "inter",
//                        context::class.java.simpleName,
//                        adapterClassName,
//                        1,
//                        loadingTime
//                    )
                    println("onAdLoaded: $interAd and $mInterstitialAd")
                    wrapper?.apply {
                        this.intersAd = interAd
                        this.state = 1
                    }
                    interPreloadMap[alias]?.postValue(wrapper)
                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    val loadingTime = System.currentTimeMillis() - now
                    val adapterClassName =
                        mInterstitialAd?.responseInfo?.mediationAdapterClassName ?: ""
//                    analyticsLogger.logAdRequest(
//                        "inter",
//                        context::class.java.simpleName,
//                        adapterClassName,
//                        0,
//                        loadingTime
//                    )
                    mInterstitialAd = null
                    wrapper?.state = -1
                    interPreloadMap[alias]?.postValue(wrapper)
                    println("preloadInterAds: ${p0.message}")
                }
            }
        )
    }

    fun preloadInterDownload(
        context: Activity,
        alias: String = ALIAS_INTER_SPLASH,
        adUnit: String = INTER_SPLASH
    ) {
        println("InterAds preloadInterDownload: ${RemoteConfig.ADS_DISABLE} and $alias and $adUnit" )
        if (RemoteConfig.ADS_DISABLE == "0") {
            return
        }

        currentAlias = alias
        currentAdUnit = adUnit
        val mutableLiveData = MutableLiveData(InterAdWrapper(null))
        interPreloadMap[alias] = mutableLiveData

        val wrapper = interPreloadMap[alias]?.value
        if (Prefs(MyApplication.instance).premium || Prefs(MyApplication.instance).isRemoveAd) {
            wrapper?.state = -1
            interPreloadMap[alias]?.postValue(wrapper)
            return
        }

        val interAdId = adUnit
        wrapper?.apply {
            state = 0
            adId = interAdId
        }

        val now = System.currentTimeMillis()

        println("Interstitial condition splash wallpaper:  $countClickVideo")
        println("preloadInterAds wrapper splash wallpaper: ${context.javaClass.simpleName} and $wrapper")
        if (isTestDevice) {
            interPreloadMap.remove(alias)
            return
        }

        interPreloadMap[alias]?.postValue(wrapper)

        val placement =  context::class.java.simpleName
//        val mode = analyticsLogger.getCurrentModeByScreen(placement)
//        analyticsLogger.updateUserProperties(context, placement ,mode)
        val interId = if (BuildConfig.DEBUG) INTER_TEST_ID else interAdId
        InterstitialAd.load(
            context,
            interId,
            getAdRequest(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interAd: InterstitialAd) {
                    mInterstitialAd = interAd
                    val adapterClassName = interAd.responseInfo.mediationAdapterClassName ?: ""
                    interAd.onPaidEventListener = OnPaidEventListener { adValue ->
                        try {
                            MyApplication.initROAS(adValue.valueMicros, adValue.currencyCode)
                            val adRevenue = AdjustAdRevenue(AdjustConfig.AD_REVENUE_ADMOB)
                            val adReceivedAmount: Double = adValue.valueMicros / 1_000_000.0
                            val adCurrencyCode = adValue.currencyCode
                            adRevenue.setRevenue(
                                adReceivedAmount,
                                adCurrencyCode
                            )
                            Adjust.trackAdRevenue(adRevenue)
                            println("InterAds preloadInterDownload onAdLoaded: $interId")
                            println("InterAds preloadInterDownload onAdLoaded: ${adValue.valueMicros} $adReceivedAmount and $adCurrencyCode")
//                            analyticsLogger.logAdImpression(
//                                "inter",
//                                context,
//                                adapterClassName,
//                                adReceivedAmount,
//                                adCurrencyCode
//                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

//                    val loadingTime = System.currentTimeMillis() - now
//                    analyticsLogger.logAdRequest(
//                        "inter",
//                        context::class.java.simpleName,
//                        adapterClassName,
//                        1,
//                        loadingTime
//                    )
                    println("onAdLoaded: $interAd and $mInterstitialAd")
                    wrapper?.apply {
                        this.intersAd = interAd
                        this.state = 1
                    }
                    interPreloadMap[alias]?.postValue(wrapper)
                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    val loadingTime = System.currentTimeMillis() - now
                    val adapterClassName =
                        mInterstitialAd?.responseInfo?.mediationAdapterClassName ?: ""
//                    analyticsLogger.logAdRequest(
//                        "inter",
//                        context::class.java.simpleName,
//                        adapterClassName,
//                        0,
//                        loadingTime
//                    )
                    println("preloadInterDownload:$interId ${p0.message}")
                    mInterstitialAd = null
                    wrapper?.state = -1
                    interPreloadMap[alias]?.postValue(wrapper)
                }
            }
        )
    }

    private fun shouldLoadAd(alias: String, interValue: String, isTestDevice: Boolean): Boolean {
        if (interValue == "0" || isTestDevice) return false


        val isMoreThan40Seconds =
            (System.currentTimeMillis() - currentTimeMillis) / 1000 >= RemoteConfig.INTER_SPACE.toInt()
        return isMoreThan40Seconds && countClickVideo % interValue.toInt() == 0
    }

    fun showPreloadInter(
        activity: FragmentActivity,
        alias: String,
        onLoadDone: (() -> Unit)? = null,
        onLoadFailed: (() -> Unit)? = null,
    ): Boolean {
        println("showPreloadInter is here")
        if (!AdmobUtils.isNetworkConnected(activity)) {
            onLoadFailed?.invoke()
            return false
        }

        if (isTestDevice) {
            onLoadFailed?.invoke()
            return false
        }

        if (Prefs(MyApplication.instance).premium || Prefs(MyApplication.instance).isRemoveAd) {
            onLoadFailed?.invoke()
            return false
        }

        countClickVideo++

        println("showPreloadInter $alias ----- ${interPreloadMap[alias]}")
        val ob = interPreloadMap[alias]

        if (ob == null) {
            onLoadFailed?.invoke()
            return false
        }

        ob.observe(activity) { wrapper ->
            println("interPreloadMap[alias] 0: $wrapper")
            if (wrapper == null) {
                ob.removeObservers(activity)
                onLoadFailed?.invoke()
                return@observe
            }

            println("interPreloadMap[alias] 1: $alias AND $${wrapper.intersAd} ${wrapper.adId} and ${wrapper.state}")
            try {
                if (activity.isDestroyed || activity.isFinishing) {
                    ob.removeObservers(activity)
                    return@observe
                }
                wrapper.let {
                    if (it.state == 1) {
                        ob.removeObservers(activity)

                        it.intersAd?.let { ads ->
                            println("interPreloadMap[alias]: ${activity.isDestroyed} and ${activity.isFinishing}")
                            showSelectedAds(
                                activity,
                                wrapper,
                                { isLoading ->
                                    if (isLoading) {
                                        onLoadDone?.invoke()

                                        countClickVideo = 1
                                        currentTimeMillis = System.currentTimeMillis()
                                    } else {
                                        onLoadFailed?.invoke()
                                    }
                                },
                                needLoadAfterShow = true
                            )
                        }

                        if (it.intersAd == null) {
                            mInterstitialAd = null
                            onLoadFailed?.invoke()
                        }

                    } else if (it.state == -1) {
                        ob.removeObservers(activity)
                        mInterstitialAd = null
                        onLoadFailed?.invoke()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }


        return true
    }

    fun showPreloadInterDownload(
        activity: FragmentActivity,
        alias: String,
        onLoadDone: (() -> Unit)? = null,
        onLoadFailed: (() -> Unit)? = null,
    ): Boolean {
        println("showPreloadInter is here")
        if (!AdmobUtils.isNetworkConnected(activity)) {
            onLoadFailed?.invoke()
            return false
        }

        if (isTestDevice) {
            onLoadFailed?.invoke()
            return false
        }

        if (Prefs(MyApplication.instance).premium || Prefs(MyApplication.instance).isRemoveAd) {
            onLoadFailed?.invoke()
            return false
        }

        println("showPreloadInterDownload $alias ----- ${interPreloadMap[alias]}")
        val ob = interPreloadMap[alias]

        if (ob == null) {
            onLoadFailed?.invoke()
            return false
        }

        ob.observe(activity) { wrapper ->
            println("interDownloadMap[alias] 0: $wrapper")
            if (wrapper == null) {
                ob.removeObservers(activity)
                onLoadFailed?.invoke()
                return@observe
            }

            println("interDownloadMap[alias] 1: $alias AND $${wrapper.intersAd} ${wrapper.adId} and ${wrapper.state}")
            try {
                if (activity.isDestroyed || activity.isFinishing) {
                    ob.removeObservers(activity)
                    return@observe
                }
                wrapper.let {
                    if (it.state == 1) {
                        ob.removeObservers(activity)

                        it.intersAd?.let { ads ->
                            println("interDownloadMap[alias]: ${activity.isDestroyed} and ${activity.isFinishing}")
                            showSelectedAds(
                                activity,
                                wrapper,
                                { isLoading ->
                                    if (isLoading) {
                                        onLoadDone?.invoke()

                                    } else {
                                        onLoadFailed?.invoke()
                                    }
                                },
                                needLoadAfterShow = true
                            )
                        }

                        if (it.intersAd == null) {
                            mInterstitialAd = null
                            onLoadFailed?.invoke()
                        }

                    } else if (it.state == -1) {
                        ob.removeObservers(activity)
                        mInterstitialAd = null
                        onLoadFailed?.invoke()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }


        return true
    }

    interface Callback {
        fun callback()
    }
}