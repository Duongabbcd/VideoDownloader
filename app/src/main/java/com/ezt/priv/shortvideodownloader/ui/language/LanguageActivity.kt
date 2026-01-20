package com.ezt.priv.shortvideodownloader.ui.language

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.ezt.priv.shortvideodownloader.ads.RemoteConfig
import com.ezt.priv.shortvideodownloader.ads.type.NativeAds
import com.ezt.priv.shortvideodownloader.databinding.ActivityLanguageBinding
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.ui.BaseActivity2
import com.ezt.priv.shortvideodownloader.ui.home.MainActivity
import com.ezt.priv.shortvideodownloader.ui.language.adapter.Language
import com.ezt.priv.shortvideodownloader.ui.language.adapter.LanguageAdapter
import com.ezt.priv.shortvideodownloader.ui.more.settings.SettingsActivity
import com.ezt.priv.shortvideodownloader.util.Common
import com.ezt.priv.shortvideodownloader.util.Common.gone
import com.ezt.priv.shortvideodownloader.util.Common.visible
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LanguageActivity : BaseActivity2<ActivityLanguageBinding>(ActivityLanguageBinding::inflate) {
//    @Inject
//    lateinit var analyticsLogger: AnalyticsLogger

    private var adapter2: LanguageAdapter? = null
    private var start = false
    private var now = 0L

    private var displayCount = 0

    private lateinit var allLanguages: ArrayList<Language>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        allLanguages = GlobalConstant.getListLocation()
        now = System.currentTimeMillis()

        if (RemoteConfig.NATIVE_INTRO.contains("1") && RemoteConfig.ADS_DISABLE != "0") {
            NativeAds.preloadNativeAds(
                this@LanguageActivity, NativeAds.ALIAS_NATIVE_INTRO_1,
                NativeAds.NATIVE_INTRO_1
            )
        }

         if (RemoteConfig.NATIVE_INTRO.contains("2") && RemoteConfig.ADS_DISABLE != "0") {
            NativeAds.preloadNativeAds(
                this@LanguageActivity, NativeAds.ALIAS_NATIVE_INTRO_2,
                NativeAds.NATIVE_INTRO_2
            )
        }

        if (RemoteConfig.NATIVE_INTRO.contains("5") && RemoteConfig.ADS_DISABLE != "0") {
            NativeAds.preloadNativeAds(
                this@LanguageActivity, NativeAds.ALIAS_NATIVE_INTRO_5,
                NativeAds.NATIVE_INTRO_5
            )
        }

        if (RemoteConfig.NATIVE_INTRO_FULL != "0" && RemoteConfig.ADS_DISABLE != "0") {
            NativeAds.preloadNativeAds(
                this@LanguageActivity, NativeAds.ALIAS_NATIVE_FULLSCREEN,
                NativeAds.NATIVE_INTRO_FULLSCREEN
            )
        }

        start = intent.getBooleanExtra("fromSplash", false)
        binding.apply {
            rlNative.isVisible = RemoteConfig.ADS_DISABLE != "0"
            applyBtn.isVisible = !start
            showNative(
                this@LanguageActivity,
                NativeAds.ALIAS_NATIVE_LANGUAGE_1,
                binding.frNative,
                onLoadDone = {
                    Handler(Looper.getMainLooper()).postDelayed({
                        binding.mLoadingView.root.visibility = View.GONE
                    }, 1500) // Delay 1500 ms (1.5 seconds)
                },
                onLoadFailed = {
                    NativeAds.preloadNativeAds(this@LanguageActivity, NativeAds.ALIAS_NATIVE_LANGUAGE_1,
                        NativeAds.NATIVE_LANGUAGE_1)

                    showNative(this@LanguageActivity, NativeAds.ALIAS_NATIVE_LANGUAGE_1,
                        binding.frNative, onLoadDone = {
                            Handler(Looper.getMainLooper()).postDelayed({
                                binding.mLoadingView.root.visibility = View.GONE
                            }, 1500)
                        }, onLoadFailed = {
                            Handler(Looper.getMainLooper()).postDelayed({
                                binding.rlNative.visibility = View.GONE
                            }, 1500) // Delay 1500 ms (1.5 seconds)
                        }
                    )
                }
            )

            if (start) {
                backBtn.gone()
                selectedLanguage = ""
            } else {
                backBtn.visible()
                selectedLanguage = Common.getPreLanguage(this@LanguageActivity)
                selectedLanguageName = Common.getLang(this@LanguageActivity)
                binding.backBtn.setOnClickListener {
                    finish()
                }

            }
        }
        getLanguage()
        changeLanguageDone()
    }

    override fun onResume() {
        super.onResume()

//        if (!AppOpenManager.getInstance().isDismiss) {
//
//        }
    }

    private fun changeLanguageDone() {
        binding.applyBtn.setOnClickListener {
            if (selectedLanguage != "") {
                Common.setPreLanguage(this@LanguageActivity, selectedLanguage)
                Common.setPreLanguageflag(this@LanguageActivity, selectedLanguageFlag)
                Common.setLang(this@LanguageActivity, selectedLanguageName)
                Log.d(
                    TAG,
                    "changeLanguageDone: $selectedLanguage and $selectedLanguageName"
                )
                if (!start) {
//                    analyticsLogger.updateUserProperties(this, LANGUAGE_SCREEN, -1)
//                    val duration = System.currentTimeMillis() - now
//                    analyticsLogger.logScreenGo("setting_screen", LANGUAGE_SCREEN, duration)
                    startActivity(Intent(this@LanguageActivity, SettingsActivity::class.java))
                } else {
                    nextScreenByCondition()
                }
            } else {
                Toast.makeText(this, "Please select language", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun nextScreenByCondition() {
//        analyticsLogger.updateUserProperties(this, LANGUAGE_SCREEN, -1)
//        val duration = System.currentTimeMillis() - now
//        analyticsLogger.logScreenGo("intro_screen", LANGUAGE_SCREEN, duration)
        val intent = Intent(this@LanguageActivity, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }


    private var isFirstTime = true
    private fun getLanguage() {
        adapter2 = LanguageAdapter(object : LanguageAdapter.OnClickListener {
            override fun onClickListener(position: Int, language: Language) {
                displayCount++
                println("displayCount: $displayCount")
                if (language.key != selectedLanguage || language.name != selectedLanguageName) {
//                    analyticsLogger.updateUserProperties(
//                        this@LanguageActivity,
//                        "language_screen",
//                        -1
//                    )
//                    analyticsLogger.logButtonClick(
//                        buttonName = "Select ${language.name}",
//                        screenName = "language_screen"
//                    )
                    binding.applyBtn.visible()
                    if (displayCount <= 1) {
                        showNative(this@LanguageActivity, NativeAds.ALIAS_NATIVE_LANGUAGE_2,
                            binding.frNative, onLoadDone = {
                                Handler(Looper.getMainLooper()).postDelayed({
                                    binding.mLoadingView.root.visibility = View.GONE
                                }, 1500)
                            }, onLoadFailed = {
                                NativeAds.preloadNativeAds(this@LanguageActivity, NativeAds.ALIAS_NATIVE_LANGUAGE_2,
                                    NativeAds.NATIVE_LANGUAGE_2)
                                showNative(this@LanguageActivity, NativeAds.ALIAS_NATIVE_LANGUAGE_2,
                                    binding.frNative, onLoadDone = {
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            binding.mLoadingView.root.visibility = View.GONE
                                        }, 1500)
                                    }, onLoadFailed = {
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            binding.rlNative.visibility = View.GONE
                                        }, 1500) // Delay 1500 ms (1.5 seconds)
                                    }
                                )
                            }
                        )
                    }
                }
                selectedLanguage = language.key
                selectedLanguageFlag = language.img
                selectedLanguageName = language.name


                adapter2?.updatePosition(selectedLanguage)
                if (isFirstTime && start) {
                    isFirstTime = false
                }
            }

        })

        val selected = if (!start) Common.getPreLanguage(this@LanguageActivity) else ""
        adapter2?.updateData(allLanguages, selected)
        binding.allLanguages.layoutManager = LinearLayoutManager(this)
        binding.allLanguages.adapter = adapter2
    }

    override fun onBackPressed() {
        if (!start) {
            finish()
        } else {
            moveTaskToBack(true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        displayCount = 0
    }

    companion object {
        var selectedLanguage = ""
        var selectedLanguageName = ""
        var selectedLanguageFlag = -1

        val TAG = LanguageActivity::class.java.simpleName
        const val LANGUAGE_SCREEN = "language_screen"

        fun showNative(
            activity: FragmentActivity,
            alias: String,
            frNative: ViewGroup,
            fullScreen: Boolean = false,
            onLoadDone: (() -> Unit)? = null,
            onLoadFailed: (() -> Unit)? = null,
        ) {
            NativeAds.showPreloadNative(
                activity,
                alias,
                frNative,
                onLoadDone,
                onLoadFailed,
                layoutResId = if(fullScreen) R.layout.native_fullscreen else R.layout.native_large_screen
            )
        }

        fun showNative1(
            activity: FragmentActivity,
            alias: String,
            frNative: ViewGroup,
            fullScreen: Boolean = false,
            onLoadDone: (() -> Unit)? = null,
            onLoadFailed: (() -> Unit)? = null,
        ) {
            NativeAds.showPreloadNative(
                activity,
                alias,
                frNative,
                onLoadDone,
                onLoadFailed,
                layoutResId =  R.layout.native_small_screen
            )
        }
    }

}