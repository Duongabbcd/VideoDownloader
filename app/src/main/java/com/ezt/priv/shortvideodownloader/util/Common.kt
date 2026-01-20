package com.ezt.priv.shortvideodownloader.util

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.paging.PagingData
import java.util.Locale
import com.ezt.priv.shortvideodownloader.R
import androidx.core.content.edit

object Common {
    fun View.visible() {
        visibility = View.VISIBLE
    }

    fun View.inVisible() {
        visibility = View.INVISIBLE
    }

    fun View.gone() {
        visibility = View.GONE
    }

    fun Context.toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    fun getLang(mContext: Context): String {
        val preferences =
            mContext.getSharedPreferences(mContext.packageName, MODE_PRIVATE)
        return preferences.getString("KEY_LANG", "English") ?: "English"
    }

    fun setLang(context: Context, open: String?) {
        val preferences =
            context.getSharedPreferences(context.packageName, MODE_PRIVATE)
        preferences.edit().putString("KEY_LANG", open).apply()
    }

//    fun getPreLanguageflag(mContext: Context): Int {
//        val preferences = mContext.getSharedPreferences(mContext.packageName, MODE_PRIVATE)
//        return preferences.getInt("KEY_FLAG", R.drawable.english)
//    }

    fun setPreLanguageflag(context: Context, flag: Int) {
        val preferences = context.getSharedPreferences(context.packageName, MODE_PRIVATE)
        preferences.edit().putInt("KEY_FLAG", flag).apply()
    }

    fun getPreLanguage(mContext: Context): String {
        val preferences = mContext.getSharedPreferences(mContext.packageName, MODE_PRIVATE)
        return preferences.getString("KEY_LANGUAGE", "en").toString()
    }

    fun setPreLanguage(context: Context, language: String?) {
        if (TextUtils.isEmpty(language)) return
        val preferences = context.getSharedPreferences(context.packageName, MODE_PRIVATE)
        preferences.edit().putString("KEY_LANGUAGE", language).apply()
    }


    fun setLocale(context: Context, lang: String?) {
        val myLocale = lang?.let { Locale(it) }
        val res = context.resources
        val dm = res.displayMetrics
        val conf = res.configuration
        conf.setLocale(myLocale)
        res.updateConfiguration(conf, dm)
    }

//    fun showRate(context: Activity) {
//        val ratingDialog1 = RatingDialog.Builder(context).session(1).date(1).ignoreRated(false)
//            .setNameApp(context.resources.getString(R.string.app_name))
//            .setIcon(R.drawable.icon_app_round)
//            .setEmail("linhnguyen.ezt@gmail.com")
//            .isShowButtonLater(true).isClickLaterDismiss(true)
//            .setTextButtonLater("Maybe Later").setOnlickMaybeLate {
//                AppOpenManager.getInstance().enableAppResumeWithActivity(MainActivity::class.java)
//            }.setOnlickRate {
//                AppOpenManager.getInstance().disableAppResumeWithActivity(MainActivity::class.java)
//            }.setDeviceInfo(
//                BuildConfig.VERSION_NAME,
//                Build.VERSION.SDK_INT.toString(),
//                Build.MANUFACTURER + "_" + Build.MODEL
//            ).ratingButtonColor(Color.parseColor("#004BBB"))
//            .build()
//        ratingDialog1.setCanceledOnTouchOutside(false)
//        ratingDialog1.show()
//    }

    fun Context.openUrl(url: String) {
        runCatching {
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).also {
                this.startActivity(it)
            }
        }
    }

    fun Context.openPrivacy() {
        openUrl("https://sites.google.com/view/shortvideodownloadersaver/privacy-policy")
    }

    fun Context.composeEmail(recipient: String, subject: String) {

        val emailIntent = Intent(Intent.ACTION_SENDTO)
        emailIntent.data = Uri.parse("mailto:")
        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject)

        try {
            this.startActivity(Intent.createChooser(emailIntent, "Send Email"))
        } catch (e: ActivityNotFoundException) {
            // Handle case where no email app is available
        }
    }

    fun Context.rateApp() {
        val applicationID = this.packageName
        val playStoreUri = Uri.parse("market://details?id=$applicationID")

        val rateIntent = Intent(Intent.ACTION_VIEW, playStoreUri)
        rateIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)

        try {
            this.startActivity(rateIntent)
        } catch (e: ActivityNotFoundException) {
            val webPlayStoreUri =
                Uri.parse("https://play.google.com/store/apps/details?id=$applicationID")
            val webRateIntent = Intent(Intent.ACTION_VIEW, webPlayStoreUri)
            this.startActivity(webRateIntent)
        }
    }


    fun getCountOpenApp(mContext: Context): Int {
        val preferences = mContext.getSharedPreferences(
            mContext.packageName,
            MODE_PRIVATE
        )
        return preferences.getInt("KEY_CountOpenApp", 0)
    }

    fun setCountOpenApp(context: Context, flag: Int) {
        val preferences = context.getSharedPreferences(
            context.packageName,
            MODE_PRIVATE
        )
        preferences.edit().putInt("KEY_CountOpenApp", flag).apply()
    }

    fun setAllFavouriteWallpaper(context: Context, list: List<Int> = emptyList()) {
        val preferences = context.getSharedPreferences(
            context.packageName,
            MODE_PRIVATE
        )
        preferences.edit().putString("KEY_Fav_Wall", list.joinToString(",")).apply()
    }


    fun setAllFavouriteGenres(context: Context, list: List<Int> = emptyList()) {
        val preferences = context.getSharedPreferences(
            context.packageName,
            MODE_PRIVATE
        )
        preferences.edit().putString("KEY_Fav_Gen", list.joinToString(",")).apply()
    }


    fun getAllFavouriteGenres(mContext: Context): List<Int> {
        val preferences = mContext.getSharedPreferences(
            mContext.packageName,
            MODE_PRIVATE
        )
        val savedList = preferences.getString("KEY_Fav_Gen", null)
        return savedList?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList()
    }

    fun getAllFavouriteWallpaper(mContext: Context): List<Int?> {
        val preferences = mContext.getSharedPreferences(
            mContext.packageName,
            MODE_PRIVATE
        )
        val savedList = preferences.getString("KEY_Fav_Wall", null)
        return savedList?.split(",")?.mapNotNull { it.toIntOrNull() } ?: listOf()
    }

    fun getAllFreeRingtones(mContext: Context): List<String> {
        val preferences = mContext.getSharedPreferences(
            mContext.packageName,
            MODE_PRIVATE
        )
        val savedList = preferences.getString("KEY_Free_Ringtones", null)
        return savedList?.split(",")?.mapNotNull { it } ?: listOf()
    }

    fun setAllFreeRingtones(context: Context, list: List<String> = emptyList()) {
        val preferences = context.getSharedPreferences(
            context.packageName,
            MODE_PRIVATE
        )
        preferences.edit().putString("KEY_Free_Ringtones", list.joinToString(",")).apply()
    }

    fun getAllFreeWallpapers(mContext: Context): List<Int> {
        val preferences = mContext.getSharedPreferences(
            mContext.packageName,
            MODE_PRIVATE
        )
        val savedList = preferences.getString("KEY_Free_Wallpapers", null)
        return savedList?.split(",")?.mapNotNull { it.toIntOrNull() } ?: listOf()
    }

    fun setAllFreeWallpapers(context: Context, list: List<Int> = emptyList()) {
        val preferences = context.getSharedPreferences(
            context.packageName,
            MODE_PRIVATE
        )
        preferences.edit().putString("KEY_Free_Wallpapers", list.joinToString(",")).apply()
    }

    fun getAllNewRingtones(mContext: Context): List<Int> {
        val preferences = mContext.getSharedPreferences(
            mContext.packageName,
            MODE_PRIVATE
        )
        val savedList = preferences.getString("KEY_Fixed_New", null)
        return savedList?.split(",")?.mapNotNull { it.toIntOrNull() } ?: listOf()
    }

    fun setAllNewRingtones(context: Context, list: List<Int> = emptyList()) {
        val preferences = context.getSharedPreferences(
            context.packageName,
            MODE_PRIVATE
        )
        preferences.edit().putString("KEY_Fixed_New", list.joinToString(",")).apply()
    }

    fun getAllTrendingRingtones(mContext: Context): List<Int> {
        val preferences = mContext.getSharedPreferences(
            mContext.packageName,
            MODE_PRIVATE
        )
        val savedList = preferences.getString("KEY_Fixed_Trend", null)
        return savedList?.split(",")?.mapNotNull { it.toIntOrNull() } ?: listOf()
    }

    fun setAllWeeklyTrendingRingtones(context: Context, list: List<Int> = emptyList()) {
        val preferences = context.getSharedPreferences(
            context.packageName,
            MODE_PRIVATE
        )
        preferences.edit().putString("KEY_Fixed_Trend", list.joinToString(",")).apply()
    }

    fun getAllEditorChoices(mContext: Context): List<Int> {
        val preferences = mContext.getSharedPreferences(
            mContext.packageName,
            MODE_PRIVATE
        )
        val savedList = preferences.getString("KEY_Fixed_Choice", null)
        return savedList?.split(",")?.mapNotNull { it.toIntOrNull() } ?: listOf()
    }

    fun setAllEditorChoices(context: Context, list: List<Int> = emptyList()) {
        val preferences = context.getSharedPreferences(
            context.packageName,
            MODE_PRIVATE
        )
        preferences.edit().putString("KEY_Fixed_Choice", list.joinToString(",")).apply()
    }

    fun setTheme(context: Context, selectedTheme: Int) {
        val preferences = context.getSharedPreferences(
            context.packageName,
            MODE_PRIVATE
        )
        preferences.edit().putInt("KEY_UsedTheme", selectedTheme).apply()
    }


    fun showDialogGoToSetting(context: Context, onClickListener: (Boolean) -> Unit) {
        val alertDialog = AlertDialog.Builder(context).create()
        alertDialog.setTitle(R.string.title_grant_Permission)
        alertDialog.setMessage(context.getString(R.string.message_grant_Permission))
        alertDialog.setCancelable(true)
        alertDialog.setButton(
            DialogInterface.BUTTON_POSITIVE, context.getString(R.string.goto_setting)
        ) { _: DialogInterface?, _: Int ->
            onClickListener(true)
            alertDialog.dismiss()
        }

        // Negative button: Cancel
        alertDialog.setButton(
            DialogInterface.BUTTON_NEGATIVE, context.getString(R.string.cancel)
        ) { _: DialogInterface?, _: Int ->
            onClickListener(false)
            alertDialog.dismiss()
        }

        // Handle cancel (e.g., tapped outside or pressed back)
        alertDialog.setOnCancelListener {
            onClickListener(false)
        }

        alertDialog.show ()

        // Set background to white
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.WHITE))

        // Set positive button text color to black
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.BLACK)
        // Set title and message text color to black
        val titleId = context.resources.getIdentifier("alertTitle", "id", "android")
        val messageId = android.R.id.message

        alertDialog.findViewById<TextView>(titleId)?.setTextColor(Color.BLACK)
        alertDialog.findViewById<TextView>(messageId)?.setTextColor(Color.BLACK)
    }

    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

//    fun generateJwt(deviceId: String, secret: String): String {
//        val algorithm = Algorithm.HMAC256(secret)
//        val data = mapOf("client_id" to deviceId, "type" to 1)
//        return JWT.create()
//            .withClaim("data", data)
//            .sign(algorithm)
//    }

    fun setSortOrder(context: Context, sortOrder: String)  {
        val preferences = context.getSharedPreferences(context.packageName, MODE_PRIVATE)
        preferences.edit().putString("KEY_SortOrder", sortOrder).apply()
    }

    fun getSortOrder(context: Context) : String {
        val preferences = context.getSharedPreferences(context.packageName, MODE_PRIVATE)
        return preferences.getString("KEY_SortOrder", "name+asc").toString()
    }

    fun setSortWppOrder(context: Context, sortOrder: String) {
        val preferences = context.getSharedPreferences(context.packageName, MODE_PRIVATE)
        preferences.edit().putString("KEY_Wpp_SortOrder", sortOrder).apply()
    }

    fun setNotificationEnable(context: Context, isEnable: Boolean) {
        val preferences = context.getSharedPreferences(context.packageName, MODE_PRIVATE)
        preferences.edit().putBoolean("KEY_NOTIF_ENABLE", isEnable).apply()
    }

    private val isTiramisuOrAbove by lazy {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    fun getNotificationEnable(context: Context): Boolean {
        val preferences = context.getSharedPreferences(context.packageName, MODE_PRIVATE)
        val defaultValue = isTiramisuOrAbove
        return preferences.getBoolean("KEY_NOTIF_ENABLE", defaultValue)
    }


    fun setFavRingtone(context: Context, favRingtones: List<Int>) {
        val prefs = context.getSharedPreferences(context.packageName, MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString("Key_FavRingtone", favRingtones.joinToString(","))
        editor.apply()
    }


    fun getFavRingtone(context: Context): List<Int> {
        val preferences = context.getSharedPreferences(context.packageName, MODE_PRIVATE)
        val origin = preferences.getString("Key_FavRingtone", "") ?: ""
        return if (origin.isNotEmpty()) {
            origin.split(",").map { it.toInt() }
        } else {
            emptyList()
        }
    }

    fun setAllowWifiDownloadOnly(context: Context, isAllowed: Boolean = false) {
        println("setAllowWifiDownloadOnly: $isAllowed")
        val prefs = context.getSharedPreferences(context.packageName, MODE_PRIVATE)
        prefs.edit {
            putBoolean("Key_AllowWifiDownloadOnly", isAllowed)
        }
    }

    fun getAllowWifiDownloadOnly(context: Context) : Boolean {
        val preferences = context.getSharedPreferences(context.packageName, MODE_PRIVATE)
        val origin = preferences.getBoolean("Key_AllowWifiDownloadOnly", false)
        return origin
    }

}