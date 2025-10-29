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
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.ads.RemoteConfig
import com.ezt.priv.shortvideodownloader.ads.type.BannerAds.BANNER_HOME
import com.ezt.priv.shortvideodownloader.ads.type.InterAds
import com.ezt.priv.shortvideodownloader.database.models.expand.non_table.WhatsAppStatus
import com.ezt.priv.shortvideodownloader.databinding.ActivityWhatsappBinding
import com.ezt.priv.shortvideodownloader.ui.BaseActivity2
import com.ezt.priv.shortvideodownloader.ui.home.MainActivity
import com.ezt.priv.shortvideodownloader.ui.home.MainActivity.Companion.loadBanner
import com.ezt.priv.shortvideodownloader.ui.social.FacebookInfoActivity
import com.ezt.priv.shortvideodownloader.ui.tab.TabActivity
import com.ezt.priv.shortvideodownloader.ui.whatsapp.adapter.OnEditWhatsAppListener
import com.ezt.priv.shortvideodownloader.ui.whatsapp.adapter.StatusAdapter
import com.ezt.priv.shortvideodownloader.ui.whatsapp.viewmodel.WhatsAppViewModel
import com.ezt.priv.shortvideodownloader.util.Common.gone
import com.ezt.priv.shortvideodownloader.util.Common.visible
import com.ezt.priv.shortvideodownloader.util.FileUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class WhatsAppActivity : BaseActivity2<ActivityWhatsappBinding>(ActivityWhatsappBinding::inflate), OnEditWhatsAppListener {
    private val REQUEST_CODE_OPEN_DIRECTORY = 1001
    private val PREF_KEY_STATUSES_URI = "statuses_folder_uri"

    private var currentTab = 0

    private lateinit var prefs : SharedPreferences
    private lateinit var statusAdapter: StatusAdapter

    private lateinit var whatsAppViewModel: WhatsAppViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        whatsAppViewModel = ViewModelProvider(this)[WhatsAppViewModel::class.java]

        statusAdapter = StatusAdapter(this)
        InterAds.preloadInterAds(
            this@WhatsAppActivity,
            alias = InterAds.ALIAS_INTER_DOWNLOAD,
            adUnit = InterAds.INTER_AD1
        )

        displayByCondition(0)
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
                    allStatuses.visible()
                    statusAdapter.submitList(input, true)
                }
            }
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
        if (resultCode == RESULT_OK && data?.data != null) {
            val uri = data.data!!

            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            when (requestCode) {
                REQUEST_CODE_OPEN_DIRECTORY -> {
                    println("Picked WhatsApp statuses folder: $uri")
                    saveUri(uri) // Save to PREF_KEY_STATUSES_URI
                }
            }
        }
    }



    private fun openStatusFolderPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Encode the path with slashes replaced by %2F
            val encodedPath = Uri.encode("Android/media/com.whatsapp/WhatsApp/Media")
            val initialUri = Uri.parse("content://com.android.externalstorage.documents/tree/primary:$encodedPath")
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri)
        }

        startActivityForResult(intent, REQUEST_CODE_OPEN_DIRECTORY)
    }


    fun saveUri(uri: Uri) {
        contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        prefs.edit().putString(PREF_KEY_STATUSES_URI, uri.toString()).apply()
    }

    fun getSavedUri(): Uri? =
        prefs.getString(PREF_KEY_STATUSES_URI, null)?.let { Uri.parse(it) }


    fun hasAccess(): Boolean {
        val uri = getSavedUri() ?: return false
        return contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isReadPermission
        }
    }

    override fun onResume() {
        super.onResume()
        // Check for saved URI permission

        Log.d(TAG,
            "Banner Conditions: ${RemoteConfig.BANNER_ALL_2} and ${RemoteConfig.ADS_DISABLE_2}"
        )
        if (RemoteConfig.BANNER_ALL_2 == "0" || RemoteConfig.ADS_DISABLE_2 == "0") {
            binding.frBanner.root.gone()
        } else {
            loadBanner(this, BANNER_HOME)
        }

        if (!hasAccess() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // If no access, prompt the user to pick the status folder
            openStatusFolderPicker()
        }
    }

    override fun onDownloadListener(status: WhatsAppStatus) {
       val path = Uri.parse(status.path)
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
                    // Continue to next package
                }
            }

            // If none were found
            Toast.makeText(ctx, "WhatsApp is not installed", Toast.LENGTH_SHORT).show()
        }
    }
}