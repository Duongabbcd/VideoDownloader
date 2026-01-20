package com.ezt.priv.shortvideodownloader.ads

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig

object FireBaseConfig {

    fun initRemoteConfig(layout: Int, completeListener: CompleteListener) {
        val mFirebaseRemoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings: com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings =
            com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build()
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings)
        mFirebaseRemoteConfig.setDefaultsAsync(layout)

        mFirebaseRemoteConfig.addOnConfigUpdateListener(object :
            com.google.firebase.remoteconfig.ConfigUpdateListener {
            override fun onUpdate(configUpdate: com.google.firebase.remoteconfig.ConfigUpdate) {
                mFirebaseRemoteConfig.activate().addOnCompleteListener {
                    completeListener.onComplete()
                }
            }

            override fun onError(error: com.google.firebase.remoteconfig.FirebaseRemoteConfigException) {
            }
        })

        mFirebaseRemoteConfig.fetchAndActivate().addOnCompleteListener {
            if (it.isSuccessful) {
                Handler(Looper.getMainLooper()).postDelayed({
                    completeListener.onComplete()
                }, 2000)
            }
        }
    }

    interface CompleteListener {
        fun onComplete()
    }

    fun getValue(key: String): String {
        val mFirebaseRemoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        Log.d("==FireBaseConfig==", "getValue: $key ${mFirebaseRemoteConfig.getString(key)}")
        return mFirebaseRemoteConfig.getString(key)
    }
}