package com.ezt.priv.shortvideodownloader.ui.more.settings

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.addCallback
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.database.viewmodel.CookieViewModel
import com.ezt.priv.shortvideodownloader.databinding.ActivitySettingsBinding
import com.ezt.priv.shortvideodownloader.ui.BaseActivity2
import com.ezt.priv.shortvideodownloader.ui.home.MainActivity
import com.ezt.priv.shortvideodownloader.ui.language.LanguageActivity
import com.ezt.priv.shortvideodownloader.ui.more.guidance.GuidanceActivity
import com.ezt.priv.shortvideodownloader.ui.more.settings.feedback.ShowRateDialog
import com.ezt.priv.shortvideodownloader.ui.tab.viewmodel.TabViewModel
import com.ezt.priv.shortvideodownloader.util.Common
import com.ezt.priv.shortvideodownloader.util.Common.openPrivacy
import com.ezt.priv.shortvideodownloader.util.Common.openUrl
import com.ezt.priv.shortvideodownloader.util.FileUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class SettingsActivity : BaseActivity2<ActivitySettingsBinding>(ActivitySettingsBinding::inflate) {
    var context: Context? = null
    private var isViewCreated = false
    private lateinit var cookiesViewModel: CookieViewModel
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = baseContext
        setContentView(binding.root)
        cookiesViewModel = ViewModelProvider(this)[CookieViewModel::class.java]

        isViewCreated = true
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.frame_layout) as NavHostFragment
        val navController = navHostFragment.findNavController()

        val listener =
            NavController.OnDestinationChangedListener { controller, destination, arguments ->
                if (destination.id == R.id.mainSettingsFragment) {
                    changeTopAppbarTitle(getString(R.string.settings))
                }
            }

        navController.addOnDestinationChangedListener(listener)
//        binding.settingsToolbar.setNavigationOnClickListener {
//            onBackPressedDispatcher.onBackPressed()
//        }

        onBackPressedDispatcher.addCallback(this@SettingsActivity) {
            if (navController.currentDestination?.id == R.id.mainSettingsFragment) {
                navController.popBackStack()
                finishAndRemoveTask()
            } else {
                navController.navigateUp()
            }
        }

        if (savedInstanceState == null) navController.navigate(R.id.mainSettingsFragment)

        binding.apply {
            val lang = Common.getPreLanguage(this@SettingsActivity)
            languageName.text = displayLanguageInItsName(lang)

            guidanceBtn.setOnClickListener {
                startActivity(Intent(this@SettingsActivity, GuidanceActivity::class.java))
            }

            languageOption.setOnClickListener {
                startActivity(Intent(this@SettingsActivity, LanguageActivity::class.java).apply {
                    putExtra("fromSplash", false)
                })
            }

            backBtn.setOnClickListener {
                startActivity(Intent(this@SettingsActivity, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                })
            }

            var isAllowedWifiOnly = Common.getAllowWifiDownloadOnly(this@SettingsActivity)
            println("isAllowedWifiOnly: $isAllowedWifiOnly")
            if (isAllowedWifiOnly) {
                wifiSwitch.setImageResource(R.drawable.icon_switch_on)
            } else {
                wifiSwitch.setImageResource(R.drawable.icon_switch_off)
            }

            wifiSwitch.setOnClickListener {
                isAllowedWifiOnly = !isAllowedWifiOnly
                if (isAllowedWifiOnly) {
                    Common.setAllowWifiDownloadOnly(this@SettingsActivity, true)
                    wifiSwitch.setImageResource(R.drawable.icon_switch_on)
                } else {
                    Common.setAllowWifiDownloadOnly(this@SettingsActivity, false)
                    wifiSwitch.setImageResource(R.drawable.icon_switch_off)
                }

            }

            clearCache.setOnClickListener {
                val deleteDialog = MaterialAlertDialogBuilder(this@SettingsActivity)
                    .setTitle(getString(R.string.confirm_delete_history))
                    .setMessage(getString(R.string.clear_cache))
                    .setNegativeButton(getString(R.string.cancel)) { dialogInterface, _ ->
                        dialogInterface.cancel()
                    }
                    .setPositiveButton(getString(R.string.ok)) { _, _ ->
                        try {
                            val cacheDir = this@SettingsActivity.cacheDir
                            if (cacheDir != null && cacheDir.isDirectory) {
                                cacheDir.deleteRecursively()
                            }
                            Toast.makeText(
                                this@SettingsActivity,
                                resources.getString(R.string.cache_1),
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(
                                this@SettingsActivity,
                                resources.getString(R.string.cache_2),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .create()
                deleteDialog.show()   // ✅ Correct place

            }

            clearBrowserHistory.setOnClickListener {
                val deleteDialog = MaterialAlertDialogBuilder(this@SettingsActivity)
                    .setTitle(getString(R.string.confirm_delete_history))
                    .setMessage(getString(R.string.clear_browser_history))
                    .setNegativeButton(getString(R.string.cancel)) { dialogInterface, _ ->
                        dialogInterface.cancel()
                    }
                    .setPositiveButton(getString(R.string.ok)) { _, _ ->
                        TabViewModel.addNewTab(this@SettingsActivity, listOf())
                        Toast.makeText(
                            this@SettingsActivity,
                            resources.getString(R.string.clear_all_browser),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .create()
                deleteDialog.show()   // ✅ Correct place

            }

            clearCookies.setOnClickListener {
                val deleteDialog = MaterialAlertDialogBuilder(this@SettingsActivity)
                    .setTitle(getString(R.string.confirm_delete_history))
                    .setMessage(getString(R.string.confirm_delete_cookies_desc))
                    .setNegativeButton(getString(R.string.cancel)) { dialogInterface, _ ->
                        dialogInterface.cancel()
                    }
                    .setPositiveButton(getString(R.string.ok)) { _, _ ->
                        cookiesViewModel.deleteAll()
                        kotlin.runCatching {
                            FileUtil.getCookieFile(this@SettingsActivity, true) {
                                File(it).writeText("")
                            }

                            Toast.makeText(
                                this@SettingsActivity,
                                getString(R.string.cache_3),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .create()

                deleteDialog.show()   // ✅ Correct place
            }


            feedbackOption.setOnClickListener {
                val dialog = ShowRateDialog(this@SettingsActivity)
                dialog.show()
            }

            shareOption.setOnClickListener {
                sharePlayStoreLink(this@SettingsActivity)
            }

            policyOption.setOnClickListener {
                this@SettingsActivity.openPrivacy()
            }

        }
    }

    fun sharePlayStoreLink(context: Context) {
        val shareText =
            "Check out this app: https://play.google.com/store/apps/details?id=com.ezt.priv.shortvideodownloader"

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        val chooser = Intent.createChooser(intent, "Share via")
        context.startActivity(chooser)
    }


    private fun displayLanguageInItsName(string: String): String {
        return when (string) {
            "en" -> "English"
            "ar" -> "العربية"
            "bn" -> "বাংলা"
            "de" -> "Deutsch"
            "es" -> "Español"
            "fr" -> "Français"
            "hi" -> "हिन्दी"
            "in" -> "Bahasa"
            "pt" -> "Português"
            "it" -> "Italiano"
            "ru" -> "Русский"
            "ko" -> "한국어"
            else -> "English"
        }

    }

    fun changeTopAppbarTitle(text: String) {
        if (isViewCreated) {
            //do nothing
        }
    }
}