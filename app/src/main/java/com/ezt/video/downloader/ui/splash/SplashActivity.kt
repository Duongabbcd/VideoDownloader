package com.ezt.video.downloader.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import com.ezt.video.downloader.MyApplication
import com.ezt.video.downloader.ads.AdmobUtils.isNetworkConnected
import com.ezt.video.downloader.ads.AdsManager
import com.ezt.video.downloader.ads.FireBaseConfig
import com.ezt.video.downloader.ads.RemoteConfig
import com.ezt.video.downloader.ads.helper.GDPRRequestable
import com.ezt.video.downloader.databinding.ActivitySplashBinding
import com.ezt.video.downloader.ui.BaseActivity2
import com.ezt.video.downloader.util.Common
import com.ezt.video.downloader.util.Common.inVisible
import com.google.android.ump.FormError
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.system.exitProcess
import com.ezt.video.downloader.R
import com.ezt.video.downloader.ads.AdsManager.isTestDevice
import com.ezt.video.downloader.ads.RemoteConfig.NATIVE_FULL_SPLASH_070625
import com.ezt.video.downloader.ads.RemoteConfig.REMOTE_SPLASH_070625
import com.ezt.video.downloader.ads.type.InterAds
import com.ezt.video.downloader.ads.type.NativeAds
import com.ezt.video.downloader.ads.type.OpenAds
import com.ezt.video.downloader.ui.BaseActivity3
import com.ezt.video.downloader.ui.home.MainActivity
import com.ezt.video.downloader.ui.language.LanguageActivity
import com.ezt.video.downloader.util.Common.visible

@AndroidEntryPoint
class SplashActivity : BaseActivity3<ActivitySplashBinding>(ActivitySplashBinding::inflate) {
    private var isMobileAdsInitializeCalled = AtomicBoolean(false)
    private var isInitAds = AtomicBoolean(false)

    private var handler = Handler(Looper.getMainLooper())
    private var runnable = Runnable {
        if (showAds) {
//            initAdmob()
        }
    }
    private var showAds = true
    private var isFinish = false
    private var isLoadAdsDone = false

    private var isFirstStep = false

    private var now = 0L

//    @Inject
//    lateinit var analyticsLogger: AnalyticsLogger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isLoadAdsDone = false
        now = System.currentTimeMillis()
        isFirstStep = Common.getCountOpenApp(this@SplashActivity) == 0
        MyApplication.screenName = "splash_screen"

        initView()
    }

//    private fun initGDPR() {
//        if (isNetworkConnected(this)) {
//            MyApplication.trackingEvent("check_GDPR")
//            GDPRRequestable.getGdprRequestable(this)
//                .setOnRequestGDPRCompleted(object : GDPRRequestable.RequestGDPRCompleted {
//                    override fun onRequestGDPRCompleted(formError: FormError?) {
//                        println("onRequestGDPRCompleted: $formError")
//
//                    }
//                })
//            GDPRRequestable.getGdprRequestable(this).requestGDPR()
//        } else {
//
//        }
//
//    }


    private fun initAds() {
        if (RemoteConfig.AD_OPEN_APP == "0") {
            showAds()
            return
        }

        if (RemoteConfig.ADS_DISABLE_2 == "0") {
            showBanner()
            return
        }

        if (isFirstStep) {
            if (RemoteConfig.NATIVE_LANGUAGE.contains("1")) {
                NativeAds.preloadNativeAds(
                    this@SplashActivity,
                    alias = NativeAds.ALIAS_NATIVE_LANGUAGE_1,
                    adId = NativeAds.NATIVE_LANGUAGE_1
                )
            }

            if (RemoteConfig.NATIVE_LANGUAGE.contains("2")) {
                NativeAds.preloadNativeAds(
                    this@SplashActivity, NativeAds.ALIAS_NATIVE_LANGUAGE_2,
                    NativeAds.NATIVE_LANGUAGE_2
                )
            }

            println("RemoteConfig: ${RemoteConfig.AD_OPEN_APP}")
            when {
                RemoteConfig.AD_OPEN_APP.contains("0") -> {
                    return
                }

                RemoteConfig.AD_OPEN_APP.contains("1") -> {
                    println("RemoteConfig AD_OPEN_APP: ${RemoteConfig.AD_OPEN_APP}")
                    InterAds.preloadInterDownload(
                        this@SplashActivity, InterAds.ALIAS_INTER_SPLASH,
                        InterAds.INTER_SPLASH
                    )
                }

                RemoteConfig.AD_OPEN_APP.contains("2") -> {
                    OpenAds.initOpenAds(this) {
                        isLoadAdsDone = true
                        handler.removeCallbacks(runnable)
                    }
                }

                else -> {
                    return
                }
            }
            showBanner()
        } else {
            OpenAds.initOpenAds(this) {
                isLoadAdsDone = true
                handler.removeCallbacks(runnable)
                showBanner()
            }
        }
    }

    private fun showAdsIfAppRunning() {
        println("showAdsIfAppRunning: $isDestroyed and $isFirstStep and ${RemoteConfig.AD_OPEN_APP} and ${OpenAds.isCanShowOpenAds()}")
        if (!isDestroyed) {
            if (isFirstStep) {
                when {
                    RemoteConfig.AD_OPEN_APP.contains("0") -> nextScreen()
                    RemoteConfig.AD_OPEN_APP.contains("1") -> {
                        InterAds.showPreloadInterDownload(
                            this,
                            alias = InterAds.ALIAS_INTER_SPLASH,
                            onLoadDone = { nextScreen() },
                            onLoadFailed = {
                                nextScreen()
                            })
                    }

                    RemoteConfig.AD_OPEN_APP.contains("2") -> {
                        if (OpenAds.isCanShowOpenAds()) {
                            binding.fullScreen.visible()
                            OpenAds.showOpenAds(this) {
                                nextScreen()
                            }
                        } else {
                            nextScreen()
                        }
                    }

                    else -> nextScreen()
                }

            } else {
                if (OpenAds.isCanShowOpenAds()) {
                    binding.fullScreen.visible()
                    OpenAds.showOpenAds(this) {
                        nextScreen()
                    }
                } else {
                    nextScreen()
                }
            }

        }
    }

    override fun initView() {
        if ((!isTaskRoot && intent.hasCategory(Intent.CATEGORY_LAUNCHER)) && intent.action != null && intent.action === Intent.ACTION_MAIN) {
            finish()
            return
        }

        handler.postDelayed(runnable, 20000)
        val x = 0
//        isNetworkConnected(this)
        if (x > 1) {
            FireBaseConfig.initRemoteConfig(
                R.xml.remote_config_default,
                object : FireBaseConfig.CompleteListener {
                    override fun onComplete() {
//                        RemoteConfig.AD_OPEN_APP = FireBaseConfig.getValue("AD_OPEN_APP")
//                        RemoteConfig.ADS_DISABLE_2 = FireBaseConfig.getValue("ADS_DISABLE_2")
//                        RemoteConfig.BANNER_ALL_2 = FireBaseConfig.getValue("BANNER_ALL_2")
//
//                        RemoteConfig.INTER_DOWNLOAD_2 = FireBaseConfig.getValue("INTER_DOWNLOAD_2")
//                        RemoteConfig.INTER_CALLSCREEN_2 =
//                            FireBaseConfig.getValue("INTER_CALLSCREEN_2")
//                        RemoteConfig.INTER_RINGTONE_2 = FireBaseConfig.getValue("INTER_RINGTONE_2")
//                        RemoteConfig.INTER_SPACE_TIME_2 =
//                            FireBaseConfig.getValue("INTER_SPACE_TIME_2")
//                        RemoteConfig.INTER_WALLPAPER_2 =
//                            FireBaseConfig.getValue("INTER_WALLPAPER_2")
//
//                        RemoteConfig.NATIVE_FAVOURITE = FireBaseConfig.getValue("NATIVE_FAVOURITE")
//                        RemoteConfig.NATIVE_INTRO = FireBaseConfig.getValue("NATIVE_INTRO")
//                        RemoteConfig.NATIVE_LANGUAGE = FireBaseConfig.getValue("NATIVE_LANGUAGE")
//                        RemoteConfig.NATIVE_PERMISSION =
//                            FireBaseConfig.getValue("NATIVE_PERMISSION")
//
//                        RemoteConfig.OPEN_AD_RETURN_APP =
//                            FireBaseConfig.getValue("OPEN_AD_RETURN_APP")
//
//                        RemoteConfig.REWARD_RINGTONE = FireBaseConfig.getValue("REWARD_RINGTONE")
//                        RemoteConfig.REWARD_SLIDE = FireBaseConfig.getValue("REWARD_SLIDE")
//                        RemoteConfig.REWARD_VIDEO = FireBaseConfig.getValue("REWARD_VIDEO")
//
//                        RemoteConfig.isForcedToUpdate =
//                            FireBaseConfig.getValue("isForcedToUpdate")
//                        RemoteConfig.totalFreeRingtones =
//                            FireBaseConfig.getValue("totalFreeRingtones")
//                        RemoteConfig.totalFreeWallpapers =
//                            FireBaseConfig.getValue("totalFreeWallpapers")

//                        AdsManager.countClickRingtone = 0
//                        AdsManager.countClickWallpaper = 0
//                        AdsManager.countClickCallscreen = 0


                        if (isInitAds.get()) {
                            return
                        }
                        isInitAds.set(true)

                        println("RemoteConfig.ADS_DISABLE: ${RemoteConfig.ADS_DISABLE_2}")
                        if (RemoteConfig.ADS_DISABLE_2 != "0") {
//                            initAds()
                            setupCMP()
                        } else {
                            binding.tvStart.visible()
                            Handler(Looper.getMainLooper()).postDelayed({
                                nextScreen()
                            }, 3000)
                        }
                    }
                })
        } else {
            binding.tvStart.visible()
            Handler(Looper.getMainLooper()).postDelayed({
                nextScreen()
            }, 3000)
        }
    }

    private fun nextScreen() {
        val countOpen = Common.getCountOpenApp(this)
        println("countOpen: $countOpen")

        val duration = System.currentTimeMillis() - now
        Common.setPreLanguage(this, "en")

        //Previous: countOpen <= 1
        if (countOpen == 0) {
//            analyticsLogger.updateUserProperties(
//                this@SplashActivity,
//                "splash_screen",
//                -1
//            )

//            analyticsLogger.logScreenGo("language_screen", "splash_screen", duration)

            val intent = Intent(this@SplashActivity, LanguageActivity::class.java)
            intent.putExtra("fromSplash", true)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        } else {

            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            intent.putExtra("fromSplash", true)
            startActivity(intent)

//            val isWritingGranted = hasLegacyWritePermission(this@SplashActivity)
//            val isSystemModified = RingtoneHelper.hasWriteSettingsPermission(this@SplashActivity)
//
//            if (!isWritingGranted || !isSystemModified) {
//                analyticsLogger.updateUserProperties(
//                    this@SplashActivity,
//                    "splash_screen",
//                    -1
//                )
//                analyticsLogger.logScreenGo("permission_screen", "splash_screen", duration)
//                val intent = Intent(this@SplashActivity, PermissionActivity::class.java)
//                intent.putExtra("fromSplash", true)
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
//                startActivity(intent)
//            } else {
//                analyticsLogger.updateUserProperties(
//                    this@SplashActivity,
//                    "splash_screen",
//                    -1
//                )
//                analyticsLogger.logScreenGo("main_screen", "splash_screen", duration)
//                val intent = Intent(this@SplashActivity, MainActivity::class.java)
//                intent.putExtra("fromSplash", true)
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
//                startActivity(intent)
//            }

        }


    }

    private fun setupCMP() {
        initializeMobileAdsSdk()
    }

    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.get()) {
            return
        }
        isMobileAdsInitializeCalled.set(true)
    }

    private fun showBanner() {
        Log.d("ADS_SPLASH_070625", "showBanner: ${RemoteConfig.ADS_SPLASH_070625}")
        binding.frBanner.visibility = View.GONE
        binding.tvStart.visibility = View.GONE
        showAds()
    }

    private fun showAds() {
        println("showAds: $REMOTE_SPLASH_070625")
//        binding.loading.visible()
        binding.tvStart.visible()
        showAdsIfAppRunning()

    }


    override fun onBackPressed() {
        super.onBackPressed()
        exitProcess(0)
    }
}