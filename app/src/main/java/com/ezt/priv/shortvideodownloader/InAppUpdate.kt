package com.ezt.priv.shortvideodownloader

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

class InAppUpdate(
    activity: AppCompatActivity,
    forceUpdate: Boolean,
    private val installUpdatedListener: InstallUpdatedListener,
) :
    InstallStateUpdatedListener {
    private val prefs = activity.getSharedPreferences("in_app_update_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val PREF_KEY_FLEXIBLE_CANCEL_VERSION = "flexible_cancel_version"
    }


    private var appUpdateManager: AppUpdateManager
    private val MY_REQUEST_CODE = 500
    private var parentActivity: Activity = activity

    private var currentType = AppUpdateType.FLEXIBLE

    init {
        appUpdateManager = AppUpdateManagerFactory.create(parentActivity)

        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            println("info: ${info.updateAvailability()}")
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                val updateType =
                    if (forceUpdate) AppUpdateType.IMMEDIATE else AppUpdateType.FLEXIBLE
                showUpdateDialog(info, updateType)
            } else {
                Log.d(
                    "InAppUpdate",
                    "There is no newer version to update: ${info.updateAvailability()}"
                )
            }
        }.addOnFailureListener { exception ->
            Log.e("InAppUpdate", "Failed to check update: ${exception.localizedMessage}")
//            installUpdatedListener.onUpdateFailure()
        }

        appUpdateManager.registerListener(this)
    }

    private fun startUpdate(info: AppUpdateInfo, type: Int) {
        appUpdateManager.startUpdateFlowForResult(info, type, parentActivity, MY_REQUEST_CODE)
        currentType = type
    }

    fun onResume() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (currentType == AppUpdateType.FLEXIBLE) {
                if (info.installStatus() == InstallStatus.DOWNLOADED)
                    installUpdatedListener.onDownloadCompleted()
            } else if (currentType == AppUpdateType.IMMEDIATE) {
                if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    startUpdate(info, AppUpdateType.IMMEDIATE)
                }
            }
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == MY_REQUEST_CODE) {
            if (resultCode != AppCompatActivity.RESULT_OK) {
                installUpdatedListener.onUpdateCancel()
            }
        }
    }

    private fun flexibleUpdateDownloadCompleted() {
        // Clear cancel flag after update downloaded and ready to install
        prefs.edit().remove(PREF_KEY_FLEXIBLE_CANCEL_VERSION).apply()
        installUpdatedListener.onUpdateNextAction()
    }

    fun onDestroy() {
        appUpdateManager.unregisterListener(this)
    }

    override fun onStateUpdate(state: InstallState) {
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            flexibleUpdateDownloadCompleted()
        }
    }

    private fun showUpdateDialog(info: AppUpdateInfo, updateType: Int) {
        val currentVersionCode = info.availableVersionCode()
        println("showUpdateDialog: $info and $updateType")
        if (updateType == AppUpdateType.FLEXIBLE) {
            val canceledVersion = prefs.getInt(PREF_KEY_FLEXIBLE_CANCEL_VERSION, -1)
            if (canceledVersion == currentVersionCode) {
                // User already canceled for this version → skip dialog and continue
                installUpdatedListener.onUpdateNextAction()
                return
            }
        }

        AlertDialog.Builder(parentActivity)
            .setTitle("Update Available")
            .setMessage("A new version of this app is available. Would you like to update now?")
            .setPositiveButton("Update") { dialog, _ ->
                dialog.dismiss()
                if (updateType == AppUpdateType.FLEXIBLE) {
                    // Clear cancel flag because user confirmed update now
                    prefs.edit().remove(PREF_KEY_FLEXIBLE_CANCEL_VERSION).apply()
                }

                startUpdate(info, updateType)
                currentType = updateType
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                prefs.edit().putInt(PREF_KEY_FLEXIBLE_CANCEL_VERSION, currentVersionCode).apply()
                installUpdatedListener.onUpdateCancel()
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }
}

interface InstallUpdatedListener {
    fun onUpdateNextAction()
    fun onDownloadCompleted()
    fun onUpdateCancel()
    fun onUpdateFailure()
}