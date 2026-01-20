package com.ezt.priv.shortvideodownloader.ads.type

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import com.ezt.priv.shortvideodownloader.R
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAdRevenue
import com.adjust.sdk.AdjustConfig
import com.ezt.priv.shortvideodownloader.BuildConfig
import com.ezt.priv.shortvideodownloader.MyApplication
import com.ezt.priv.shortvideodownloader.ads.RemoteConfig
import com.ezt.priv.shortvideodownloader.ads.helper.Prefs
import com.ezt.priv.shortvideodownloader.ui.intro.IntroActivityNew
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoController
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

object NativeAds {

    private const val ADMOB_AD_UNIT_ID_TEST = "ca-app-pub-3940256099942544/2247696110"
    private const val ADMOB_AD_UNIT_ID_TEST_WITH_VIDEO = "ca-app-pub-3940256099942544/1044960115"

    const val NATIVE_LANGUAGE_1 = "ca-app-pub-3607148519095421/3959214898"
    const val NATIVE_LANGUAGE_2 = "ca-app-pub-3607148519095421/7238493666"

    const val NATIVE_INTRO_1 = "ca-app-pub-3607148519095421/1333051552"
    const val NATIVE_INTRO_2 = "ca-app-pub-3607148519095421/6282125901"
    const val NATIVE_INTRO_5 = "ca-app-pub-3607148519095421/7595207573"
    const val NATIVE_INTRO_FULLSCREEN = "ca-app-pub-3607148519095421/4097325002"
    const val NATIVE_HOME = "ca-app-pub-3607148519095421/7610136559"
    const val NATIVE_PREVIEW = "ca-app-pub-3607148519095421/5854201750"

    const val ALIAS_NATIVE_LANGUAGE_1 = "ads_native_language_1"
    const val ALIAS_NATIVE_LANGUAGE_2 = "ads_native_language_2"

    const val ALIAS_NATIVE_INTRO_1 = "ads_native_intro_1"
    const val ALIAS_NATIVE_INTRO_2 = "ads_native_intro_2"
    const val ALIAS_NATIVE_INTRO_5 = "ads_native_intro_5"
    const val ALIAS_NATIVE_FULLSCREEN = "ads_native_full_screen"
    const val ALIAS_NATIVE_HOME = "ads_native_home_screen"
    const val ALIAS_NATIVE_PREVIEW= "ads_native_preview_screen"


    const val ALIAS_INTRO = "ads_native_language"

    var preloadMap = mutableMapOf<String, MutableLiveData<NativeAdWrapper>>()

    class NativeAdWrapper(var nativeAd: NativeAd?) {
        var state: Int = -1
    }


    interface CallBackNativeAds {
        fun onLoaded()
        fun onError()
    }

    interface CallBackFullScreenAds {
        fun onNextClick()
    }

    private fun populateNativeAdView(
        nativeAd: NativeAd,
        adView: NativeAdView,
        isDefault: Boolean = false
    ) {
        adView.mediaView = adView.findViewById(R.id.ad_media)
        adView.mediaView?.mediaContent = nativeAd.mediaContent
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.tvActionBtnTitle)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)

        (adView.headlineView as? TextView)?.text = nativeAd.headline

        if (!isDefault) {
            adView.callToActionView?.backgroundTintList =
                ColorStateList.valueOf(Color.parseColor("#00C377"))
        } else {
            adView.callToActionView?.backgroundTintList =
                ColorStateList.valueOf(Color.parseColor("#00C377"))
        }

        if (nativeAd.body == null) {
            adView.bodyView?.visibility = View.INVISIBLE
        } else {
            adView.bodyView?.visibility = View.VISIBLE
            (adView.bodyView as? TextView)?.text = nativeAd.body
        }

        if (nativeAd.callToAction == null) {
            adView.callToActionView?.visibility = View.INVISIBLE
        } else {
            adView.callToActionView?.visibility = View.VISIBLE
        }

        nativeAd.icon?.let {
            (adView.iconView as? ImageView)?.setImageDrawable(it.drawable)
            adView.iconView?.visibility = View.VISIBLE
        } ?: run {
            (adView.iconView as? ImageView)?.setImageResource(R.drawable.icon_loading_fail)
            adView.iconView?.visibility = View.VISIBLE
        }

        adView.setNativeAd(nativeAd)

        nativeAd.mediaContent?.videoController?.let { vc ->
            if (vc.hasVideoContent()) {
                vc.videoLifecycleCallbacks = object : VideoController.VideoLifecycleCallbacks() {
                }
            }
        }
    }

    fun initNativeAds(
        activity: Activity,
        frameLayout: FrameLayout,
        callBackNativeAds: CallBackNativeAds,
        adUnitId: String = NATIVE_LANGUAGE_1,
    ) {
        if (Prefs(MyApplication.instance).premium ||
            Prefs(MyApplication.instance).isRemoveAd
        ) {
            callBackNativeAds.onLoaded()
            return
        }

        val adLoader =
            AdLoader.Builder(activity, if (BuildConfig.DEBUG) ADMOB_AD_UNIT_ID_TEST else adUnitId)
                .forNativeAd { nativeAd ->
                    callBackNativeAds.onLoaded()
                    if (activity.isDestroyed || activity.isFinishing || activity.isChangingConfigurations) {
                        nativeAd.destroy()
                        return@forNativeAd
                    }

                    nativeAd.setOnPaidEventListener { adValue ->
                        try {
                            MyApplication.initROAS(adValue.valueMicros, adValue.currencyCode)
                            val adRevenue = AdjustAdRevenue(AdjustConfig.AD_REVENUE_ADMOB).apply {
                                setRevenue(adValue.valueMicros / 1_000_000.0, adValue.currencyCode)
                            }
                            Adjust.trackAdRevenue(adRevenue)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    val layoutId = R.layout.native_large_screen

                    val materialCardView =
                        activity.layoutInflater.inflate(layoutId, null) as FrameLayout
                    val adView = materialCardView.findViewById<NativeAdView>(R.id.uniform)
                    Handler(Looper.getMainLooper()).postDelayed({
                        populateNativeAdView(nativeAd, adView)
                    }, 1500) // Delay 1500 ms (1.5 seconds)

                    frameLayout.removeAllViews()
                    frameLayout.addView(materialCardView)
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        callBackNativeAds.onError()
                    }
                })
                .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    fun preloadNativeAds(activity: Activity, alias: String, adId: String = NATIVE_LANGUAGE_1) {
        if (RemoteConfig.ADS_DISABLE == "0") {
            return
        }
        println("preloadNativeAds: $alias and $adId")
        val mutableLiveData = MutableLiveData(NativeAdWrapper(null))
        preloadMap[alias] = mutableLiveData

        val wrapper = preloadMap[alias]?.value
        if (Prefs(MyApplication.instance).premium || Prefs(MyApplication.instance).isRemoveAd) {
            wrapper?.state = -1
            preloadMap[alias]?.postValue(wrapper)
            return
        }

        wrapper?.state = 0
        preloadMap[alias]?.postValue(wrapper)

        val now = System.currentTimeMillis()
        var adapterClassName = ""

        val placement =  activity::class.java.simpleName
//        val mode = analyticsLogger.getCurrentModeByScreen(placement)
//        analyticsLogger.updateUserProperties(activity, placement ,mode)
        val adLoader =
            AdLoader.Builder(activity, if (BuildConfig.DEBUG) ADMOB_AD_UNIT_ID_TEST else adId)
                .forNativeAd { nativeAd ->
                    println("preloadNativeAds 123: ${nativeAd.mediaContent?.hasVideoContent()}")
                    adapterClassName = nativeAd.responseInfo?.mediationAdapterClassName ?: ""
                    nativeAd.setOnPaidEventListener { adValue ->
                        try {
                            MyApplication.initROAS(adValue.valueMicros, adValue.currencyCode)
                            val adReceivedAmount: Double = adValue.valueMicros / 1_000_000.0
                            val adCurrencyCode = adValue.currencyCode

                            val adRevenue = AdjustAdRevenue(AdjustConfig.AD_REVENUE_ADMOB).apply {
                                setRevenue(adReceivedAmount, adCurrencyCode)
                            }
                            Adjust.trackAdRevenue(adRevenue)
                            println("NativeAds preloadNativeAds onAdLoaded: ${adValue.valueMicros} $adReceivedAmount and $adCurrencyCode")
//                            analyticsLogger.logAdImpression(
//                                "native",
//                                activity,
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
//                        "native",
//                        activity::class.java.simpleName,
//                        adapterClassName,
//                        1,
//                        loadingTime
//                    )


                    wrapper?.apply {
                        this.nativeAd = nativeAd
                        this.state = 1
                    }
                    println("preloadNativeAds: ${wrapper?.state} and $alias")
                    preloadMap[alias]?.postValue(wrapper)
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        println("NativeAds onAdFailedToLoad: $error")
                        if(alias == ALIAS_NATIVE_FULLSCREEN) {
                            IntroActivityNew.isFailedOfFullScreen = true
                        }

                        val loadingTime = System.currentTimeMillis() - now
//                        analyticsLogger.logAdRequest(
//                            "reward",
//                            activity::class.java.simpleName,
//                            adapterClassName,
//                            0,
//                            loadingTime
//                        )

                        wrapper?.state = -1
                        preloadMap[alias]?.postValue(wrapper)
                    }
                })
                .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    fun showPreloadNative(
        activity: FragmentActivity,
        alias: String,
        nativeView: ViewGroup,
        onLoadDone: (() -> Unit)? = null,
        onLoadFailed: (() -> Unit)? = null,
        layoutResId: Int = R.layout.native_large_screen,
        isSmaller: Boolean = false
    ): Boolean {

        if (Prefs(MyApplication.instance).premium || Prefs(MyApplication.instance).isRemoveAd) {
            nativeView.visibility = View.GONE
            return false
        }

        val ob = preloadMap[alias]

        if (ob == null) {
            onLoadFailed?.invoke()
            return false
        }

        ob.observe(activity) { wrapper ->
            try {
                if (activity.isDestroyed || activity.isFinishing) {
                    ob.removeObservers(activity)
                    return@observe
                }
                wrapper?.let {
                    println("showPreloadNative:$alias ${wrapper.state} and ${wrapper.nativeAd}")
                    if (it.state == 1) {
                        ob.removeObservers(activity)

                        it.nativeAd?.let { ads ->
                            val materialCardView =
                                activity.layoutInflater.inflate(layoutResId, null) as FrameLayout
                            // Convert 16dp to pixels
                            val marginInPx = TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                10f,
                                materialCardView.context.resources.displayMetrics
                            ).toInt()

                            val layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                leftMargin = marginInPx
                                rightMargin = marginInPx
                            }

                            if (isSmaller) {
                                materialCardView.layoutParams = layoutParams
                            }


                            val adView = materialCardView.findViewById<NativeAdView>(R.id.uniform)
                            populateNativeAdView(
                                ads,
                                adView,
                                isDefault = alias == ALIAS_NATIVE_LANGUAGE_1
                            )
                            nativeView.removeAllViews()
                            nativeView.addView(materialCardView)
                            onLoadDone?.invoke()
                        }

                        if (it.nativeAd == null) {
                            onLoadFailed?.invoke()
                        }

                    } else if (it.state == -1) {
                        ob.removeObservers(activity)
                        onLoadFailed?.invoke()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }


        return true
    }


}