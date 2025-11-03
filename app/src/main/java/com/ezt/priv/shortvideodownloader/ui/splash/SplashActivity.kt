package com.ezt.priv.shortvideodownloader.ui.splash

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import com.ezt.priv.shortvideodownloader.MyApplication
import com.ezt.priv.shortvideodownloader.ads.AdmobUtils.isNetworkConnected
import com.ezt.priv.shortvideodownloader.ads.FireBaseConfig
import com.ezt.priv.shortvideodownloader.ads.RemoteConfig
import com.ezt.priv.shortvideodownloader.databinding.ActivitySplashBinding
import com.ezt.priv.shortvideodownloader.util.Common
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.ads.RemoteConfig.REMOTE_SPLASH_070625
import com.ezt.priv.shortvideodownloader.ads.type.InterAds
import com.ezt.priv.shortvideodownloader.ads.type.NativeAds
import com.ezt.priv.shortvideodownloader.ads.type.OpenAds
import com.ezt.priv.shortvideodownloader.ui.BaseActivity3
import com.ezt.priv.shortvideodownloader.ui.home.MainActivity
import com.ezt.priv.shortvideodownloader.ui.language.LanguageActivity
import com.ezt.priv.shortvideodownloader.util.Common.visible

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isLoadAdsDone = false
        now = System.currentTimeMillis()
        isFirstStep = Common.getCountOpenApp(this@SplashActivity) == 0
        MyApplication.screenName = "splash_screen"

        val clipboard = this@SplashActivity.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("", ""))

        initView()
    }

    private fun initAds() {
        println("RemoteConfig.ADS_DISABLE: ${RemoteConfig.ADS_DISABLE} and ${RemoteConfig.AD_OPEN_APP}")
        if (RemoteConfig.AD_OPEN_APP == "0") {
            showAds()
            return
        }

        if (RemoteConfig.ADS_DISABLE == "0") {
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
            if(RemoteConfig.AD_OPEN_APP.contains("1")) {
                println("RemoteConfig AD_OPEN_APP: ${RemoteConfig.AD_OPEN_APP}")
                InterAds.preloadInterDownload(
                    this@SplashActivity, InterAds.ALIAS_INTER_SPLASH,
                    InterAds.INTER_SPLASH
                )
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
                InterAds.showPreloadInterDownload(
                    this,
                    alias = InterAds.ALIAS_INTER_SPLASH,
                    onLoadDone = { nextScreen() },
                    onLoadFailed = {
                        nextScreen()
                    })
            }

        }
    }

    override fun initView() {
        if ((!isTaskRoot && intent.hasCategory(Intent.CATEGORY_LAUNCHER)) &&
            intent.action != null && intent.action === Intent.ACTION_MAIN) {
            finish()
            return
        }

        handler.postDelayed(runnable, 20000)

        if (isNetworkConnected(this)) {
            // Initialize Remote Config and wait for completion
            FireBaseConfig.initRemoteConfig(R.xml.remote_config_default) { success ->
                if (success) {
                    // Always read the latest remote values here
                    RemoteConfig.ADS_DISABLE = FireBaseConfig.getValue("ADS_DISABLE").asString()
                    RemoteConfig.AD_OPEN_APP = FireBaseConfig.getValue("AD_OPEN_APP").asString()
                    RemoteConfig.AD_RETURN_APP = FireBaseConfig.getValue("AD_RETURN_APP").asString()
                    RemoteConfig.BANNER_ALL_2 = FireBaseConfig.getValue("BANNER_ALL").asString()
                    RemoteConfig.INTER_ALL = FireBaseConfig.getValue("INTER_ALL").asString()
                    RemoteConfig.INTER_SPACE = FireBaseConfig.getValue("INTER_SPACE").asString()
                    RemoteConfig.NATIVE_HOME = FireBaseConfig.getValue("NATIVE_HOME").asString()
                    RemoteConfig.NATIVE_INTRO = FireBaseConfig.getValue("NATIVE_INTRO").asString()
                    RemoteConfig.NATIVE_LANGUAGE = FireBaseConfig.getValue("NATIVE_LANGUAGE").asString()
                    RemoteConfig.NATIVE_INTRO_FULL = FireBaseConfig.getValue("NATIVE_INTRO_FULL").asString()
                    RemoteConfig.NATIVE_PREVIEW = FireBaseConfig.getValue("NATIVE_PREVIEW").asString()
                    RemoteConfig.isForcedToUpdate = FireBaseConfig.getValue("isForcedToUpdate").asString()

                    Log.d("Splash", "ADS_DISABLE = ${RemoteConfig.ADS_DISABLE}")

                    // Initialize ads or move to next screen
                    initAdsOrNextScreen()
                } else {
                    // Failed to fetch, fallback to defaults
                    Log.d("Splash", "Using default RemoteConfig values")
                    RemoteConfig.ADS_DISABLE = "1" // default value
                    initAdsOrNextScreen()
                }
            }
        } else {
            // No network, use defaults
            initAdsOrNextScreen()
        }
    }

    private fun initAdsOrNextScreen() {
        if (RemoteConfig.ADS_DISABLE != "0") {
            initAds()
            setupCMP()
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

            val intent = Intent(this@SplashActivity, LanguageActivity::class.java)
            intent.putExtra("fromSplash", true)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        } else {

            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            intent.putExtra("fromSplash", true)
            startActivity(intent)

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