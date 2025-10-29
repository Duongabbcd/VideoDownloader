package com.ezt.priv.shortvideodownloader.ads.helper

import android.content.Context
import android.content.SharedPreferences

class Prefs(private val context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(
            "Bonus_Preferences",
            Context.MODE_PRIVATE
        )
    private val editor: SharedPreferences.Editor = sharedPreferences.edit()

    fun setInt(key: String?, value: Int) {
        editor.putInt(key, value)
        editor.apply()
    }

    fun setString(key: String?, value: String?) {
        editor.putString(key, value)
        editor.apply()
    }

    fun setBoolean(key: String?, value: Boolean) {
        editor.putBoolean(key, value)
        editor.apply()
    }

    fun getBoolean(key: String?, def: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, def)
    }

    fun getInt(key: String?, def: Int): Int {
        return sharedPreferences.getInt(key, def)
    }


    fun getString(key: String?, def: String?): String? {
        return sharedPreferences.getString(key, def)
    }

    var premium: Boolean
        get() {
            return sharedPreferences.getBoolean("Premium", false)
        }
        set(value) {
            editor.putBoolean("Premium", value)
            editor.apply()
        }

    var isRemoveAd: Boolean
        get() = getBoolean("RemoveAd", false)
        set(value) {
            editor.putBoolean("RemoveAd", value)
            editor.apply()
        }

    fun canDownload(): Boolean {
        return getBoolean("canDownload", false)
    }

    fun setCanDownload(value: Boolean) {
        editor.putBoolean("canDownload", value)
        editor.apply()
    }
}