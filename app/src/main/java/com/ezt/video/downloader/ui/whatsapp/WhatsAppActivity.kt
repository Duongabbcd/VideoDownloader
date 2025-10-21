package com.ezt.video.downloader.ui.whatsapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.ezt.video.downloader.databinding.ActivityWhatsappBinding
import com.ezt.video.downloader.ui.BaseActivity2

class WhatsAppActivity : BaseActivity2<ActivityWhatsappBinding>(ActivityWhatsappBinding::inflate) {
    private val REQUEST_CODE_OPEN_FOLDER = 1001
    private val PREF_KEY_STATUSES_URI = "statuses_folder_uri"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Check for saved URI permission
        val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val uriString = sharedPrefs.getString(PREF_KEY_STATUSES_URI, null)

        if (uriString != null) {
            val uri = Uri.parse(uriString)
            if (isUriPermissionGranted(uri)) {
                accessStatusesFolder(uri)
            } else {
                openFolderPicker()
            }
        } else {
            openFolderPicker()
        }

        binding.apply {
            backIcon.setOnClickListener {
                finish()
            }
        }
    }


    // Open the SAF folder picker
    private fun openFolderPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        startActivityForResult(intent, REQUEST_CODE_OPEN_FOLDER)
    }

    // Check if we still have access to the folder
    private fun isUriPermissionGranted(uri: Uri): Boolean {
        val permissions = contentResolver.persistedUriPermissions
        return permissions.any { it.uri == uri && it.isReadPermission }
    }

    // Handle result from folder picker
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_OPEN_FOLDER && resultCode == Activity.RESULT_OK) {
            val treeUri = data?.data ?: return

            contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            // Save the URI
            val sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit().putString(PREF_KEY_STATUSES_URI, treeUri.toString()).apply()

            accessStatusesFolder(treeUri)
        }
    }

    // Access the folder and list files
    private fun accessStatusesFolder(uri: Uri) {
        val pickedDir = DocumentFile.fromTreeUri(this, uri)

        if (pickedDir != null && pickedDir.exists()) {
            val statusFiles = pickedDir.listFiles()
            for (file in statusFiles) {
                Log.d("Statuses", "File: ${file.name}")
            }
        } else {
            Log.e("Statuses", "Folder not accessible.")
        }
    }

    companion object {

    }
}