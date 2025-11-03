package com.ezt.priv.shortvideodownloader.ads.type

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Looper
import android.util.Log
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAdRevenue
import com.adjust.sdk.AdjustConfig
import com.ezt.priv.shortvideodownloader.BuildConfig
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.MyApplication
import com.ezt.priv.shortvideodownloader.ads.RemoteConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object RewardAds {

    private const val REWARDED_INTER_TEST_ID = "ca-app-pub-3940256099942544/5224354917"
    private const val REWARDED_INTER_ID_DEFAULT = "ca-app-pub-3940256099942544/5224354917"
    private val TAG = RewardAds::class.java.canonicalName

    private var mRewardAds: RewardedAd? = null
    private var isLoading = false
    private var isShowing = false

    var mLoadingDialog: Dialog? = null


    private fun getAdRequest(): AdRequest = AdRequest.Builder().build()


    private fun isCanShowAds(): Boolean {
        Log.d(TAG, "isLoading $isLoading and isShowing $isShowing")
//        if (isLoading || isShowing) return false
        Log.e(TAG, "mInterstitialAd == null")
        return mRewardAds != null
    }



    fun isShowing(): Boolean = isShowing

    private fun createLoadingDialog(context: Activity): Dialog {
        return Dialog(context).apply {
            setContentView(R.layout.ads_dialog_loading)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setCancelable(false)
        }
    }

    fun dismissAdsDialog() {
        mLoadingDialog?.dismiss()
    }

    fun showAds(activity: Activity, callback: RewardCallback) {
        val now = System.currentTimeMillis()
        val ad = mRewardAds
        if (ad != null) {
            // Ad is ready — show immediately
            ad.show(activity) { rewardItem ->
                Log.d("RewardAds", "Reward earned: ${rewardItem.amount} ${rewardItem.type}")
                callback.onEarnedReward()
            }
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    Log.d("RewardAds", "onAdShowedFullScreenContent")
                    callback.onAdShowed()
                }

                override fun onAdDismissedFullScreenContent() {
                    Log.d("RewardAds", "onAdDismissedFullScreenContent")
                    val placement =  activity::class.java.simpleName
//                    val mode = analyticsLogger.getCurrentModeByScreen(placement)
//
//                    analyticsLogger.updateUserProperties(activity, placement ,mode)

                    callback.onAdDismiss()

                    mRewardAds = null // Release reference

                    initRewardAds(activity) // Preload next ad
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.d("RewardAds", "onAdFailedToShowFullScreenContent")
                    mRewardAds = null
                    callback.onAdFailedToShow()
                }
            }
        } else {
            // Ad not ready — try loading, then show
            Log.d("RewardAds", "Ad not ready, loading first...")
            initRewardAds(activity)

            // Small delay to give loading a chance; ideally you’d chain the load callback
            android.os.Handler(Looper.getMainLooper()).postDelayed({
                if (mRewardAds != null) {
                    showAds(activity, callback) // Try again
                } else {
                    callback.onAdFailedToShow()
                }
            }, 1500)
        }
    }

    fun initRewardAds(context: Activity) {
        if (RemoteConfig.ADS_DISABLE == "0") return
        if (mRewardAds != null || isLoading || !isCanLoadAds()) return
        val now = System.currentTimeMillis()
        isLoading = true

        val placement =  context::class.java.simpleName
//        val mode = analyticsLogger.getCurrentModeByScreen(placement)
//        analyticsLogger.updateUserProperties(context, placement ,mode)

        RewardedAd.load(
            context,
            if (BuildConfig.DEBUG) REWARDED_INTER_TEST_ID else REWARDED_INTER_ID_DEFAULT,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d("RewardAds", "Load OK")
                    mRewardAds = ad
                    val adapterClassName = mRewardAds?.responseInfo?.mediationAdapterClassName ?: ""
                    ad.setOnPaidEventListener { adValue ->
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
                            println("RewardAds initRewardAds onAdLoaded: ${adValue.valueMicros} $adReceivedAmount and $adCurrencyCode")
//                            analyticsLogger.logAdImpression(
//                                "reward",
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
//                        "reward",
//                        context::class.java.simpleName,
//                        adapterClassName,
//                        1,
//                        loadingTime
//                    )

                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.d("RewardAds", "onAdFailedToLoad: $error")
                    val loadingTime = System.currentTimeMillis() - now
                    val adapterClassName = mRewardAds?.responseInfo?.mediationAdapterClassName ?: ""
//                    analyticsLogger.logAdRequest(
//                        "reward",
//                        context::class.java.simpleName,
//                        adapterClassName,
//                        0,
//                        loadingTime
//                    )
                    mRewardAds = null
                    isLoading = false
                }
            }
        )
    }

    private fun isCanLoadAds(): Boolean {
        // your logic here
        return true
    }

    interface RewardCallback {
        fun onAdShowed()
        fun onAdDismiss()
        fun onAdFailedToShow()
        fun onEarnedReward()
        fun onPremium()
    }
}