package com.ezt.priv.shortvideodownloader.util

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Spanned
import android.util.TypedValue
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.text.HtmlCompat
import androidx.core.text.parseAsHtml
import com.google.android.material.color.DynamicColors
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.ui.home.MainActivity

object ThemeUtil {

    private val activities = mutableListOf<Activity>()

    fun init(app: Application) {
        app.registerActivityLifecycleCallbacks(object: Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(p0: Activity, p1: Bundle?) {
                activities.add(p0)
            }

            override fun onActivityStarted(p0: Activity) {

            }

            override fun onActivityResumed(p0: Activity) {

            }

            override fun onActivityPaused(p0: Activity) {

            }

            override fun onActivityStopped(p0: Activity) {

            }

            override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {

            }

            override fun onActivityDestroyed(p0: Activity) {
                activities.remove(p0)
            }
        })

    }

    sealed class AppIcon(
        @DrawableRes val iconResource: Int,
        val activityAlias: String
    ) {
        object Default : AppIcon(R.mipmap.ic_launcher, "Default")
        object Light : AppIcon(R.mipmap.ic_launcher, "LightIcon")
        object Dark : AppIcon(R.mipmap.ic_launcher, "DarkIcon")
    }

    private val availableIcons = listOf(
        AppIcon.Default,
        AppIcon.Light,
        AppIcon.Dark
    )

    fun recreateMain() {
        activities.firstOrNull { it.javaClass == MainActivity::class.java }?.recreate()
    }

    fun recreateAllActivities() {
        activities.forEach {
            it.recreate()
        }
    }

    fun updateThemes() {
        activities.forEach {
            updateTheme(it)
            it.recreate()
        }
    }

    fun updateTheme(activity: Activity) {
        return
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)

        //update accent
        when (sharedPreferences.getString("theme_accent","blue")) {
            "Default" -> {
                DynamicColors.applyToActivityIfAvailable(activity)
                activity.setTheme(R.style.BaseTheme)
            }
            "blue" -> activity.setTheme(R.style.Theme_Blue)
            "red" -> activity.setTheme(R.style.Theme_Red)
            "green" -> activity.setTheme(R.style.Theme_Green)
            "purple" -> activity.setTheme(R.style.Theme_Purple)
            "yellow" -> activity.setTheme(R.style.Theme_Yellow)
            "orange" -> activity.setTheme(R.style.Theme_Orange)
            "monochrome" -> activity.setTheme(R.style.Theme_Monochrome)
        }

        //high contrast theme
        if (sharedPreferences.getBoolean("high_contrast",false)) {
            activity.theme.applyStyle(R.style.Pure, true)
        }

        //disable old icons
        for (appIcon in availableIcons) {
            val activityClass = "${activity.packageName}.${appIcon.activityAlias}"

            // remove old icons
            activity.packageManager.setComponentEnabledSetting(
                ComponentName(activity.packageName, activityClass),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }

        when (sharedPreferences.getString("video_downloader", "System")!!) {
            "System" -> {
                //set dynamic icon
                activity.packageManager.setComponentEnabledSetting(
                    ComponentName(activity.packageName, "${activity.packageName}.Default"),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )

                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
            "Light" -> {
                //set light icon
                activity.packageManager.setComponentEnabledSetting(
                    ComponentName(activity.packageName, "${activity.packageName}.LightIcon"),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )

                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            "Dark" -> {
                //set dark icon
                activity.packageManager.setComponentEnabledSetting(
                    ComponentName(activity.packageName, "${activity.packageName}.DarkIcon"),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )

                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            else -> {
                //set dynamic icon
                activity.packageManager.setComponentEnabledSetting(
                    ComponentName(activity.packageName, "${activity.packageName}.Default"),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )

                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }

    }

    fun getThemeColor(context: Context, colorCode: Int): Int {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val accent = sharedPreferences.getString("theme_accent", "blue")
        return if (accent == "blue"){
            "d43c3b".toInt(16)
        }else{
            val value = TypedValue()
            context.theme.resolveAttribute(colorCode, value, true)
            value.data
        }

    }

    /**
     * Get the styled app name
     */
    fun getStyledAppName(context: Context): Spanned {
        val colorPrimary = getThemeColor(context, androidx.appcompat.R.attr.colorPrimaryDark)
        val hexColor = "#%06X".format(0xFFFFFF and colorPrimary)
//        return "<span  style='color:$hexColor';>Video</span>Downloader"
//            .parseAsHtml(HtmlCompat.FROM_HTML_MODE_COMPACT)

        return "<span  style='color:$hexColor';></span>"
            .parseAsHtml(HtmlCompat.FROM_HTML_MODE_COMPACT)
    }
}