package com.ezt.priv.shortvideodownloader.ads

import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue

object FireBaseConfig {

    fun initRemoteConfig(
        layout: Int,
        onComplete: (success: Boolean) -> Unit
    ) {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(0) // always fetch latest
            .build()

        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(layout)

        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("RemoteConfig", "Fetch & Activate successful")
                onComplete(true)
            } else {
                Log.e("RemoteConfig", "Fetch failed", task.exception)
                onComplete(false)
            }
        }
    }

    fun getValue(key: String): FirebaseRemoteConfigValue {
        val value: FirebaseRemoteConfigValue = FirebaseRemoteConfig.getInstance().getValue(key)
        Log.d("FireBaseConfig", "getValue: $key = ${value.asString()} from source ${value.source}")
        return value
    }

}

