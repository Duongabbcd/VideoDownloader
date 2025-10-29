package com.ezt.priv.shortvideodownloader.ads.helper

import android.app.Activity
import android.util.Log
import com.ezt.priv.shortvideodownloader.BuildConfig
import com.ezt.priv.shortvideodownloader.MyApplication
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform

class GDPRRequestable private constructor(private val context: Activity) {

    lateinit var consentInformation: ConsentInformation

    companion object {
        var consentForm: ConsentForm? = null
        var gdprRequestable: GDPRRequestable? = null
        const val YOUR_TEST_DEVICE_ID = "76EBDA3E724A42DB7FB855C18F969450"

        fun getGdprRequestable(activity: Activity): GDPRRequestable {
            return gdprRequestable ?: GDPRRequestable(activity).also { gdprRequestable = it }
        }
    }

    interface RequestGDPRCompleted {
        fun onRequestGDPRCompleted(formError: FormError?)
    }

    private var onRequestGDPRCompleted: RequestGDPRCompleted? = null

    fun setOnRequestGDPRCompleted(listener: RequestGDPRCompleted) {
        this.onRequestGDPRCompleted = listener
    }

    fun requestGDPR() {
        val consentDebugSettingsBuilder = ConsentDebugSettings.Builder(context)
        if (BuildConfig.DEBUG) {
            consentDebugSettingsBuilder
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                .addTestDeviceHashedId(YOUR_TEST_DEVICE_ID)
        }

        val consentRequestParameters = ConsentRequestParameters.Builder()
            .setConsentDebugSettings(consentDebugSettingsBuilder.build())
            .setTagForUnderAgeOfConsent(false)
            .build()

        consentInformation = UserMessagingPlatform.getConsentInformation(context)
        consentInformation.requestConsentInfoUpdate(
            context,
            consentRequestParameters,
            {
                if (consentInformation.isConsentFormAvailable) {
                    loadForm()
                } else {
                    onRequestGDPRCompleted?.onRequestGDPRCompleted(null)
                }
            },
            { formError ->
                Log.e("gdpr===", "onConsentInfoUpdateFailure: ${formError.message}")
                onRequestGDPRCompleted?.onRequestGDPRCompleted(formError)
            }
        )
    }

    private fun loadForm() {
        UserMessagingPlatform.loadConsentForm(
            context,
            { loadedForm ->
                consentForm = loadedForm
                when (consentInformation.consentStatus) {
                    ConsentInformation.ConsentStatus.REQUIRED -> {
                        MyApplication.trackingEvent("show_GDPR")
                        consentForm?.show(context) { formError ->
                            if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.OBTAINED) {
                                onRequestGDPRCompleted?.onRequestGDPRCompleted(null)
                                MyApplication.trackingEvent("show_GDPR_OK")
                            }
                        }
                    }

                    ConsentInformation.ConsentStatus.OBTAINED -> {
                        onRequestGDPRCompleted?.onRequestGDPRCompleted(null)
                        MyApplication.trackingEvent("show_GDPR_OK2")
                    }

                    else -> Unit
                }
            },
            { formError ->
                onRequestGDPRCompleted?.onRequestGDPRCompleted(formError)
            }
        )
    }
}
