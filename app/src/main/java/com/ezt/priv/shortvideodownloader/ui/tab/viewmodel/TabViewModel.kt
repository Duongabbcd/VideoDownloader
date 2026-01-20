package com.ezt.priv.shortvideodownloader.ui.tab.viewmodel

import android.app.Application
import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.core.content.edit

class TabViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val contexts by lazy {
        application.applicationContext
    }
    private var _tabs = MutableLiveData<List<String>>()
    val tabs: LiveData<List<String>> = _tabs

    fun displayAllCurrentTabs() {
        _tabs.value = getAllTabs(contexts)
    }

    fun editAllCurrentTabs(tabs: List<String>) {
        addNewTab(contexts, tabs)
        displayAllCurrentTabs()
    }



    companion object {
        fun getAllTabs(context: Context): List<String> {
            val preferences = context.getSharedPreferences(context.packageName, MODE_PRIVATE)
            val json = preferences.getString("url_list", null)
            return if (json != null) {
                val type = object : TypeToken<List<String>>() {}.type
                Gson().fromJson(json, type)
            } else {
                emptyList()
            }
        }

        fun addNewTab(context: Context, tabs: List<String>) {
            val prefs = context.getSharedPreferences(context.packageName, MODE_PRIVATE)
            prefs.edit {
                clear()
                val json = Gson().toJson(tabs)
                putString("url_list", json)
            }
        }
    }
}