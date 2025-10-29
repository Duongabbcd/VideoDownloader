package com.ezt.priv.shortvideodownloader.ui.more.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.databinding.ActivitySettingsBinding
import com.ezt.priv.shortvideodownloader.ui.BaseActivity2
import com.ezt.priv.shortvideodownloader.ui.home.MainActivity
import com.ezt.priv.shortvideodownloader.ui.language.LanguageActivity
import com.ezt.priv.shortvideodownloader.ui.more.guidance.GuidanceActivity
import com.ezt.priv.shortvideodownloader.util.Common

class SettingsActivity : BaseActivity2<ActivitySettingsBinding>(ActivitySettingsBinding::inflate) {
    var context: Context? = null
    private  var isViewCreated = false
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = baseContext
        setContentView(binding.root)
        isViewCreated = true
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.frame_layout) as NavHostFragment
        val navController = navHostFragment.findNavController()

        val listener = NavController.OnDestinationChangedListener { controller, destination, arguments ->
            if (destination.id == R.id.mainSettingsFragment){
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
            }else{
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
        }
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