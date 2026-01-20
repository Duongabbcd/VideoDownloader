package com.ezt.priv.shortvideodownloader.ui.browse.proxy_utils

import android.content.Context

object ContextUtils {

    private var sApplicationContext: Context? = null

    fun initApplicationContext(appContext: Context?) {
        requireNotNull(appContext) { "Global application context set error" }
        sApplicationContext = appContext.applicationContext
    }

    fun getApplicationContext(): Context {
        return sApplicationContext
            ?: throw IllegalStateException("ContextUtils not initialized. Call initApplicationContext() first.")
    }
}
