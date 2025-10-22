package com.ezt.video.downloader.ui.whatsapp

import android.content.Context
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
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.ezt.video.downloader.R
import com.ezt.video.downloader.database.viewmodel.ResultViewModel
import com.ezt.video.downloader.databinding.ActivityWhatsappBinding
import com.ezt.video.downloader.ui.BaseActivity2
import com.ezt.video.downloader.ui.whatsapp.adapter.StatusAdapter
import com.ezt.video.downloader.ui.whatsapp.viewmodel.WhatsAppViewModel
import com.ezt.video.downloader.util.Common.gone
import com.ezt.video.downloader.util.Common.visible

class WhatsAppActivity : BaseActivity2<ActivityWhatsappBinding>(ActivityWhatsappBinding::inflate) {
    private val REQUEST_CODE_OPEN_DIRECTORY = 1001
    private val REQUEST_CODE_SAVED_STATUSES = 1002
    private val PREF_KEY_STATUSES_URI = "statuses_folder_uri"
    private val PREF_KEY_SAVED_STATUSES_URI = "saved_statuses_folder_uri"

    private var currentTab = 0

    private lateinit var prefs : SharedPreferences
    private lateinit var statusAdapter: StatusAdapter
//    private lateinit var statusAdapter1: StatusAdapter
//    private lateinit var statusAdapter2: StatusAdapter

    private lateinit var whatsAppViewModel: WhatsAppViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        whatsAppViewModel = ViewModelProvider(this)[WhatsAppViewModel::class.java]

        statusAdapter = StatusAdapter { status ->
            println("StatusAdapter: $status")
        }

//        statusAdapter1 = StatusAdapter { status ->
//            println("StatusAdapter: $status")
//        }
//        statusAdapter2 = StatusAdapter { status ->
//            println("StatusAdapter: $status")
//        }

        binding.apply {
            allStatuses.adapter =statusAdapter
            allStatuses.layoutManager = GridLayoutManager(this@WhatsAppActivity, 2)

//            allStatuses1.adapter =statusAdapter1
//            allStatuses1.layoutManager = GridLayoutManager(this@WhatsAppActivity, 2)
//
//            allStatuses2.adapter =statusAdapter2
//            allStatuses2.layoutManager = GridLayoutManager(this@WhatsAppActivity, 2)


            openWhatsAppBtn.setOnClickListener {
                openWhatsApp(this@WhatsAppActivity)
            }

            backIcon.setOnClickListener {
                finish()
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
                    allStatuses.gone()
                } else {
                    emptyLayout.gone()
                    allStatuses.visible()
                    println("videoWhatsAppStatus 1: $input")
                    statusAdapter.submitList(input)
                }
            }

            whatsAppViewModel.savedWhatsAppStatus.observe(this@WhatsAppActivity) { input ->
                if(currentTab != 2) {
                    return@observe
                }
                if(input.isEmpty()) {
                    emptyLayout.visible()
                    allStatuses.gone()
                } else {
                    emptyLayout.gone()
                    allStatuses.visible()
                    statusAdapter.submitList(input)
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
                whatsAppViewModel.getSavedStatus(this@WhatsAppActivity, getLocalStatusUri())
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

                REQUEST_CODE_SAVED_STATUSES -> {
                    println("Picked Saved statuses folder: $uri")
                    saveSavedStatusesUri(uri) // Save to PREF_KEY_SAVED_STATUSES_URI
                }
            }
        }
    }

    fun saveSavedStatusesUri(uri: Uri) {
        contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        prefs.edit().putString(PREF_KEY_SAVED_STATUSES_URI, uri.toString()).apply()
    }

    fun hasSavedStatusesAccess(): Boolean {
        val uri = getLocalStatusUri() ?: return false
        return contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isReadPermission
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

    private fun openSavedStatusFolderPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val encodedPath = Uri.encode("Download/VideoDownloader/Status")
            val initialUri = Uri.parse("content://com.android.externalstorage.documents/tree/primary:$encodedPath")
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri)
        }

        startActivityForResult(intent, REQUEST_CODE_SAVED_STATUSES)
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

    fun getLocalStatusUri(): Uri? =
        prefs.getString(PREF_KEY_SAVED_STATUSES_URI, null)?.let { Uri.parse(it) }

    fun hasAccess(): Boolean {
        val uri = getSavedUri() ?: return false
        return contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isReadPermission
        }
    }

    override fun onResume() {
        super.onResume()
        // Check for saved URI permission
        if (!hasAccess() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // If no access, prompt the user to pick the status folder
            openStatusFolderPicker()
        }
        if (!hasSavedStatusesAccess() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            openSavedStatusFolderPicker()
        }
        displayByCondition(0)
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