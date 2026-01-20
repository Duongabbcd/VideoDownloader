package com.ezt.priv.shortvideodownloader.ui.whatsapp

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.ads.RemoteConfig
import com.ezt.priv.shortvideodownloader.ads.type.BannerAds.BANNER_HOME
import com.ezt.priv.shortvideodownloader.ads.type.InterAds
import com.ezt.priv.shortvideodownloader.database.models.expand.non_table.WhatsAppStatus
import com.ezt.priv.shortvideodownloader.databinding.ActivityWhatsappBinding
import com.ezt.priv.shortvideodownloader.ui.BaseActivity2
import com.ezt.priv.shortvideodownloader.ui.connection.InternetConnectionViewModel
import com.ezt.priv.shortvideodownloader.ui.home.MainActivity.Companion.loadBanner
import com.ezt.priv.shortvideodownloader.ui.welcome.WelcomeActivity
import com.ezt.priv.shortvideodownloader.ui.whatsapp.adapter.OnEditWhatsAppListener
import com.ezt.priv.shortvideodownloader.ui.whatsapp.adapter.StatusAdapter
import com.ezt.priv.shortvideodownloader.ui.whatsapp.bottomsheet.RequiredPermissionBottomSheet
import com.ezt.priv.shortvideodownloader.ui.whatsapp.viewmodel.WhatsAppViewModel
import com.ezt.priv.shortvideodownloader.util.Common.gone
import com.ezt.priv.shortvideodownloader.util.Common.visible
import com.ezt.priv.shortvideodownloader.util.FileUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import kotlin.getValue
import androidx.core.net.toUri
import androidx.core.content.edit

class WhatsAppActivity : BaseActivity2<ActivityWhatsappBinding>(ActivityWhatsappBinding::inflate), OnEditWhatsAppListener {
    private val REQUEST_CODE_OPEN_DIRECTORY = 1001
    private val PREF_KEY_STATUSES_URI = "statuses_folder_uri"

    private lateinit var prefs : SharedPreferences
    private lateinit var statusAdapter: StatusAdapter

    private lateinit var whatsAppViewModel: WhatsAppViewModel
    private val connectionViewModel: InternetConnectionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        whatsAppViewModel = ViewModelProvider(this)[WhatsAppViewModel::class.java]

        connectionViewModel.isConnectedLiveData.observe(this) { isConnected ->
//            checkInternetConnected(isConnected)
        }

        statusAdapter = StatusAdapter(this)
        InterAds.preloadInterAds(
            this@WhatsAppActivity,
            alias = InterAds.ALIAS_INTER_DOWNLOAD,
            adUnit = InterAds.INTER_AD1
        )

        binding.apply {
            appIcon.setOnClickListener {
                openWhatsApp(this@WhatsAppActivity)
            }

            allStatuses.adapter =statusAdapter
            allStatuses.layoutManager = GridLayoutManager(this@WhatsAppActivity, 2)

            openWhatsAppBtn.setOnClickListener {
                openWhatsApp(this@WhatsAppActivity)
            }

            backIcon.setOnClickListener {
                currentTab = 0
                InterAds.showPreloadInter(this@WhatsAppActivity, alias = InterAds.ALIAS_INTER_DOWNLOAD, {
                    finish()
                }, {
                    finish()
                })
            }

            imageStatus.setOnClickListener {
                displayByCondition(0)
            }

            videoStatus.setOnClickListener {
                displayByCondition(1)
            }

            savedStatus.setOnClickListener {
                displayByCondition(2)
            }

            whatsAppViewModel.imageWhatsAppStatus.observe(this@WhatsAppActivity) { input ->
                println("imageWhatsAppStatus 0: $currentTab")
                noResultLayout.root.gone()
                if(currentTab != 0) {
                    return@observe
                }
                if(input.isEmpty()) {
                    emptyLayout.visible()
                    noResultLayout.root.gone()
                    allStatuses.gone()
                } else {
                    emptyLayout.gone()
                    allStatuses.visible()
                    println("imageWhatsAppStatus: $input")
                    statusAdapter.submitList(input)
                }
            }

            whatsAppViewModel.videoWhatsAppStatus.observe(this@WhatsAppActivity) { input ->
                println("videoWhatsAppStatus 0: $currentTab")
                noResultLayout.root.gone()
                if(currentTab != 1) {
                    return@observe
                }
                if(input.isEmpty()) {
                    emptyLayout.visible()
                    noResultLayout.root.gone()
                    allStatuses.gone()
                } else {
                    emptyLayout.gone()
                    allStatuses.visible()
                    println("videoWhatsAppStatus 1: $input")
                    statusAdapter.submitList(input)
                }
            }

            whatsAppViewModel.savedWhatsAppStatus.observe(this@WhatsAppActivity) { input ->
                println("savedWhatsAppStatus: $input")
                if(currentTab != 2) {
                    return@observe
                }
                if(input.isEmpty()) {
                    noResultLayout.root.visible()
                    emptyLayout.gone()
                    allStatuses.gone()
                } else {
                    emptyLayout.gone()
                    noResultLayout.root.gone()
                    allStatuses.visible()
                    statusAdapter.submitList(input, true)
                }
            }
        }
    }

    private fun checkInternetConnected(isConnected: Boolean) {
        if (!isConnected) {
            binding.origin.gone()
            binding.noInternet.root.visible()
            binding.noInternet.tryAgain.setOnClickListener {
                val connected = connectionViewModel.isConnectedLiveData.value == true
                if (connected) {
                    binding.origin.visible()
                    binding.noInternet.root.visibility = View.VISIBLE
                    // Maybe reload your data
                } else {
                    Toast.makeText(
                        this,
                        R.string.no_connection,
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }
        } else {
            binding.origin.visible()
            binding.noInternet.root.gone()
        }
    }


    private fun displayByCondition(position: Int) {
        currentTab = position
        when(position) {
            0 -> {
                updateTextColor(binding.imageStatus, listOf(binding.videoStatus, binding.savedStatus))
                whatsAppViewModel.getImageStatus(this@WhatsAppActivity, getSavedUri())
            }

            1 -> {
                updateTextColor(binding.videoStatus, listOf(binding.imageStatus, binding.savedStatus))
                whatsAppViewModel.getVideoStatus(this@WhatsAppActivity, getSavedUri())
            }

            2 -> {
                updateTextColor(binding.savedStatus, listOf(binding.imageStatus, binding.videoStatus))
                whatsAppViewModel.getSavedStatus()
            }
            else -> {
                updateTextColor(binding.imageStatus, listOf(binding.videoStatus, binding.savedStatus))
                whatsAppViewModel.getImageStatus(this@WhatsAppActivity, getSavedUri())
            }
        }
    }

    private fun updateTextColor(
        selectedView: TextView,
        unselectedViews:List<TextView>
    ) {
        selectedView.setTextColor(resources.getColor(R.color.black_green))
        unselectedViews.onEach {
            it.setTextColor(resources.getColor(R.color.text_color))
        }
    }

    // Handle result from folder picker
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OPEN_DIRECTORY && resultCode == RESULT_OK) {
            val treeUri = data?.data
            if (treeUri != null) {
                contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                saveUri(treeUri)
                Toast.makeText(
                    this,
                    resources.getString(R.string.folder_selected),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    resources.getString(R.string.folder_not_selected),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }



    private fun openStatusFolderPicker() {
        WelcomeActivity.isFromService = true
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Try to hint the correct folder; user may still need to navigate manually
//            val targetPath = "WhatsApp/Media/.Statuses"
//            val encodedPath = Uri.encode(targetPath)
            val initialUri =
                "content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fmedia/document/primary%3AAndroid%2Fmedia%2Fcom.whatsapp%2FWhatsApp%2FMedia%2F.Statuses".toUri()
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri)
        }

        startActivityForResult(intent, REQUEST_CODE_OPEN_DIRECTORY)
    }

    private fun saveUri(uri: Uri) {
        prefs.edit { putString(PREF_KEY_STATUSES_URI, uri.toString()) }
    }

    private fun getSavedUri(): Uri? =
        prefs.getString(PREF_KEY_STATUSES_URI, null)?.toUri()

    private fun hasAccess(): Boolean {
        val uri = getSavedUri() ?: return false
        return contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isReadPermission
        }
    }

    override fun onResume() {
        super.onResume()
        // Check for saved URI permission

        Log.d(TAG,
            "Banner Conditions: ${RemoteConfig.BANNER_ALL_2} and ${RemoteConfig.ADS_DISABLE}"
        )
        if (RemoteConfig.BANNER_ALL_2 == "0" || RemoteConfig.ADS_DISABLE == "0") {
            binding.frBanner.root.gone()
        } else {
            loadBanner(this, BANNER_HOME)
        }

        displayByCondition(currentTab)
        if (!hasAccess() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // If no access, prompt the user to pick the status folder
            val requestPermissionDialog = RequiredPermissionBottomSheet(this@WhatsAppActivity){
                openStatusFolderPicker()
            }
            requestPermissionDialog.show()

        }
    }





    override fun onDownloadListener(status: WhatsAppStatus) {
        val path = status.path.toUri()
        FileUtil.copyStatusFile(this@WhatsAppActivity, path )
        Toast.makeText(this@WhatsAppActivity, resources.getString(R.string.download_successfully), Toast.LENGTH_SHORT).show()
    }

    override fun onDeleteListener(status: WhatsAppStatus) {
        println("onDeleteListener: ${status.path}")
        val file = File(status.path)
        createDeleteFileDialog(file)
    }

    private fun createDeleteFileDialog(file: File) {
        val dialog = MaterialAlertDialogBuilder(this)
        dialog.setTitle(getString(R.string.delete_file_too))
        dialog.setMessage(getString(R.string.delete_desc))
        dialog.setOnCancelListener { }
        dialog.setNegativeButton(getString(R.string.exit_dialog)) { dialog : DialogInterface?, _: Int ->  dialog?.dismiss()  }
        dialog.setPositiveButton(getString(R.string.ok)) { dialog : DialogInterface?, _: Int ->
            if (file.exists()) {
                val deleted = file.delete()
                Log.d("FileDelete", "Deleted: $deleted")
                whatsAppViewModel.getSavedStatus()
            } else {
                Log.d("FileDelete", "File not found")
            }
            dialog?.dismiss()
        }
        dialog.show()
    }

    companion object {
        private val TAG = WhatsAppActivity::class.java.simpleName
        var currentTab = 0

        fun openWhatsApp(ctx: Context){
            val pm = ctx.packageManager
            val possiblePackages = listOf("com.whatsapp", "com.whatsapp.w4b")

            for (pkg in possiblePackages) {
                try {
                    // This will throw if the app is not installed
                    pm.getPackageInfo(pkg, 0)
                    val launchIntent = pm.getLaunchIntentForPackage(pkg)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        ctx.startActivity(launchIntent)
                        return  // success
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                    // Continue to next package
                }
            }

            // If none were found
            Toast.makeText(
                ctx,
                ctx.resources.getString(R.string.installed_app_required),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}