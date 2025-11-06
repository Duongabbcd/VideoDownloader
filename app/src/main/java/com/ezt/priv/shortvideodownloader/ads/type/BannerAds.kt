package com.ezt.priv.shortvideodownloader.ads.type

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAdRevenue
import com.adjust.sdk.AdjustConfig
import com.ezt.priv.shortvideodownloader.BuildConfig
import com.ezt.priv.shortvideodownloader.MyApplication
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.ads.RemoteConfig
import com.ezt.priv.shortvideodownloader.ads.helper.Prefs
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.ads.mediation.admob.AdMobAdapter

@SuppressLint("StaticFieldLeak")
object BannerAds {

    private const val BANNER_TEST_ID = "ca-app-pub-3940256099942544/9214589741"
     const val BANNER_HOME = "ca-app-pub-3607148519095421/6960782750"

    private val BANNER_ID_DEFAULT = BANNER_HOME
    private const val BANNER_ID_COLLAPSIBLE = "ca-app-pub-3607148519095421/8428394206"
    private const val BANNER_HOME_COLLAPSIBLE = "ca-app-pub-3607148519095421/8428394206"

    private var isInitBanner = false
//    val analyticsLogger: AnalyticsLogger
//        get() = EntryPointAccessors.fromApplication(
//            MyApplication.instance,
//            AnalyticsLoggerEntryPoint::class.java
//        ).analyticsLogger()



    fun getAdSize(activity: Activity): AdSize {
        val display = activity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        val adWidth = (outMetrics.widthPixels / outMetrics.density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
            activity,
            adWidth
        )
    }


    private fun getAdRequest(): AdRequest {
        return AdRequest.Builder().build()
    }

    fun initBannerAdsHome(ctx: Activity) {
        initBannerAds(ctx, BANNER_HOME_COLLAPSIBLE) {}
    }

    fun initBannerAds(
        ctx: Activity,
        adUnitId: String = BANNER_ID_DEFAULT,
        onDisplayListener: (Boolean) -> Unit
    ) {
        if (RemoteConfig.ADS_DISABLE == "0" || RemoteConfig.BANNER_ALL_2 == "0") {
            onDisplayListener(false)
            return
        }
        try {
            val now = System.currentTimeMillis()
            val adBanner: ViewGroup? = ctx.findViewById(R.id.frBanner)
            val prefs = Prefs(MyApplication.instance)
            println("initBannerAds: ${prefs.premium} and ${prefs.isRemoveAd}")
            if (prefs.premium || prefs.isRemoveAd) {
                adBanner?.visibility = View.GONE
                onDisplayListener(false)
                return
            }

            val adViewContainer: LinearLayout = ctx.findViewById(R.id.adView_container)
            if (adViewContainer == null) {
                onDisplayListener(false)
                return
            }
            val mAdViewBanner = AdView(ctx)
            mAdViewBanner.setAdSize(getAdSize(ctx))
            mAdViewBanner.adUnitId = if (BuildConfig.DEBUG) BANNER_TEST_ID else adUnitId

            val adRequest: AdRequest = if (false) {
                val extras = Bundle().apply { putString("collapsible", "bottom") }
                AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter::class.java, extras).build()
            } else {
                AdRequest.Builder().build()
            }

            println("mAdViewBanner: $mAdViewBanner")

            adViewContainer.removeAllViews()
            adViewContainer.addView(mAdViewBanner)
            mAdViewBanner.loadAd(adRequest)

            val placement =  ctx::class.java.simpleName
//            val mode = analyticsLogger.getCurrentModeByScreen(placement)
//            analyticsLogger.updateUserProperties(ctx, placement ,mode)

            mAdViewBanner.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    val adapterClassName = mAdViewBanner.responseInfo?.mediationAdapterClassName.toString()
//                    analyticsLogger.updateUserProperties(ctx, placement ,mode)
                    mAdViewBanner.setOnPaidEventListener { adValue ->
                        try {
                            println("BannerAds adapterClassName: $adapterClassName")
                            MyApplication.initROAS(adValue.valueMicros, adValue.currencyCode)
                            val adRevenue = AdjustAdRevenue(AdjustConfig.AD_REVENUE_ADMOB)
                            val adReceivedAmount: Double = adValue.valueMicros / 1_000_000.0
                            val adCurrencyCode = adValue.currencyCode

                            adRevenue.setRevenue(
                                adReceivedAmount,
                                adCurrencyCode
                            )


                            Adjust.trackAdRevenue(adRevenue)
                            println("BannerAds innitBannerAds onAdLoaded: ${adValue.valueMicros} $adReceivedAmount and $adCurrencyCode")
//                            analyticsLogger.logAdImpression(
//                                "banner",
//                                ctx,
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
//                        "banner",
//                        placement ,
//                        adapterClassName,
//                        1,
//                        loadingTime
//                    )

                    adViewContainer.visibility = View.VISIBLE
//                    hideBannerLoading(ctx, false)
                    onDisplayListener(true)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    val adapterClassName = mAdViewBanner.responseInfo?.mediationAdapterClassName.toString()
                    adViewContainer.visibility = View.GONE
                    println("BannerAds onAdFailedToLoad: $loadAdError")
                    val loadingTime = System.currentTimeMillis() - now
//                    analyticsLogger.logAdRequest(
//                        "banner",
//                        ctx::class.java.simpleName,
//                        adapterClassName,
//                        0,
//                        loadingTime
//                    )
                    onDisplayListener(false)
                }

                override fun onAdClicked() {
                    adViewContainer.visibility = View.GONE
                    println("onAdClicked: is here")
                    hideBannerLoading(ctx, true)
                }
            }

            hideBannerLoading(ctx, false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun initBannerAds(ctx: Activity, adBanner: ViewGroup?, adUnitId: String = BANNER_ID_DEFAULT) {
        try {
            val prefs = Prefs(MyApplication.instance)
            if (prefs.premium || prefs.isRemoveAd) {
                adBanner?.visibility = View.GONE
                return
            }

            if (adBanner == null) {
                return
            }

            val adViewContainer: LinearLayout =
                adBanner.findViewById(R.id.adView_container) ?: return
            val mAdViewBanner = AdView(ctx)
            mAdViewBanner.setAdSize(getAdSize(ctx))
            mAdViewBanner.adUnitId = if (BuildConfig.DEBUG) BANNER_TEST_ID else adUnitId

            val adRequest = if (false) {
                val extras = Bundle().apply { putString("collapsible", "bottom") }
                AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter::class.java, extras).build()
            } else {
                AdRequest.Builder().build()
            }


            adViewContainer.removeAllViews()
            adViewContainer.addView(mAdViewBanner)
            mAdViewBanner.loadAd(adRequest)
            mAdViewBanner.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    mAdViewBanner.setOnPaidEventListener { adValue ->
                        try {
                            MyApplication.initROAS(adValue.valueMicros, adValue.currencyCode)
                            val adRevenue = AdjustAdRevenue(AdjustConfig.AD_REVENUE_ADMOB)
                            adRevenue.setRevenue(
                                adValue.valueMicros / 1_000_000.0,
                                adValue.currencyCode
                            )
                            Adjust.trackAdRevenue(adRevenue)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    adViewContainer.visibility = View.VISIBLE
                    hideBannerLoading(adBanner, false)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    adViewContainer.visibility = View.GONE
                    hideBannerLoading(adBanner, true)
                }

                override fun onAdClicked() {
                    adViewContainer.visibility = View.GONE
                    hideBannerLoading(adBanner, true)
                }
            }

            hideBannerLoading(adBanner, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hideBannerLoading(bannerAd: ViewGroup, bl: Boolean) {
        try {
            val tvBannerLoading: com.facebook.shimmer.ShimmerFrameLayout =
                bannerAd.findViewById(R.id.shimmer_layout)
            val visibility = if (bl) View.GONE else View.VISIBLE
            tvBannerLoading.visibility = visibility
            bannerAd.findViewById<View>(R.id.view_d).visibility = visibility
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hideBannerLoading(ctx: Activity, bl: Boolean) {
        try {
            val tvBannerLoading: com.facebook.shimmer.ShimmerFrameLayout =
                ctx.findViewById(R.id.shimmer_layout)
            val visibility = if (bl) View.GONE else View.VISIBLE
            tvBannerLoading.visibility = visibility
            ctx.findViewById<View>(R.id.view_d).visibility = visibility
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getNetworkName(adapterClassName: String, isUpperCase: Boolean = false): String {
        val result = when {
            adapterClassName.contains("Admob", ignoreCase = true) -> "admob"
            adapterClassName.contains("Facebook", ignoreCase = true) -> "facebook"
            adapterClassName.contains("Unity", ignoreCase = true) -> "unity"
            adapterClassName.contains("AppLovin", ignoreCase = true) -> "applovin"
            adapterClassName.contains("IronSource", ignoreCase = true) -> "ironsource"
            adapterClassName.contains("Vungle", ignoreCase = true) -> "vungle"
            adapterClassName.contains("Chartboost", ignoreCase = true) -> "chartboost"
            else -> "unknown"
        }

        return if (isUpperCase) result.toUpperCase() else result
    }
}

//@EntryPoint
//@InstallIn(SingletonComponent::class)
//interface AnalyticsLoggerEntryPoint {
//    fun analyticsLogger(): AnalyticsLogger
//}