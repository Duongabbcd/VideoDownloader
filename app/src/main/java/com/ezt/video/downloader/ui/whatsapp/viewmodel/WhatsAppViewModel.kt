package com.ezt.video.downloader.ui.whatsapp.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ezt.video.downloader.database.models.expand.non_table.STATUS_TYPE
import com.ezt.video.downloader.database.models.expand.non_table.WhatsAppStatus
import com.ezt.video.downloader.util.Utils
import java.io.File
import java.nio.file.Files.getLastModifiedTime

class WhatsAppViewModel(
    application: Application
) : AndroidViewModel(application) {

    private var _imageWhatsAppStatus = MutableLiveData<List<WhatsAppStatus>>()
    val imageWhatsAppStatus: LiveData<List<WhatsAppStatus>> = _imageWhatsAppStatus

    private var _videoWhatsAppStatus = MutableLiveData<List<WhatsAppStatus>>()
    val videoWhatsAppStatus: LiveData<List<WhatsAppStatus>> = _videoWhatsAppStatus


    private var _savedWhatsAppStatus = MutableLiveData<List<WhatsAppStatus>>()
    val savedWhatsAppStatus: LiveData<List<WhatsAppStatus>> = _savedWhatsAppStatus


    fun getImageStatus(context: Context, statusFolderUri: Uri?) {
        if (statusFolderUri == null) {
            println("getImageStatus 0: URI is null")
            _imageWhatsAppStatus.postValue(emptyList())
            return
        }

        val documentFile = DocumentFile.fromTreeUri(context, statusFolderUri)

        if (documentFile == null || !documentFile.exists() || !documentFile.isDirectory) {
            println("getImageStatus 0: DocumentFile is invalid")
            _imageWhatsAppStatus.postValue(emptyList())
            return
        }

        val allFiles = documentFile.listFiles()
            ?.filter { file ->
                file.isFile &&
                        !file.name.isNullOrBlank() &&
                        (file.name!!.endsWith(".jpg", true)
                                || file.name!!.endsWith(".jpeg", true)
                                || file.name!!.endsWith(".png", true))
            } ?: emptyList()

        if (allFiles.isEmpty()) {
            println("getImageStatus 1: No image files found")
            _imageWhatsAppStatus.postValue(emptyList())
            return
        }

        println("getImageStatus 2: Found ${allFiles.size} images")

        val allImages = allFiles.map { file ->
            WhatsAppStatus(
                id = null,
                path = file.uri.toString(), // Use content URI
                lastModifiedTime = getLastModifiedTime(context, file.uri),
                duration = 0L,
                type = STATUS_TYPE.IMAGE,
                isArchived = false,
                fileName = file.name ?: ""
            )
        }
        println("getImageStatus 3: Found $allImages")
        _imageWhatsAppStatus.postValue(allImages)
    }


    fun getVideoStatus(context: Context, statusFolderUri: Uri?) {
        if (statusFolderUri == null) {
            println("getVideoStatus 0: URI is null")
            _videoWhatsAppStatus.postValue(emptyList())
            return
        }

        val documentFile = DocumentFile.fromTreeUri(context, statusFolderUri)

        if (documentFile == null || !documentFile.exists() || !documentFile.isDirectory) {
            println("getVideoStatus 0: DocumentFile is invalid")
            _videoWhatsAppStatus.postValue(emptyList())
            return
        }

        val allFiles = documentFile.listFiles()
            ?.filter { file ->
                file.isFile &&
                        !file.name.isNullOrBlank() &&
                        (file.name!!.endsWith(".mp4", true))
            } ?: emptyList()

        if (allFiles.isEmpty()) {
            println("getVideoStatus 1: No image files found")
            _videoWhatsAppStatus.postValue(emptyList())
            return
        }

        println("getVideoStatus 2: Found ${allFiles.size} images")

        val allImages = allFiles.map { file ->
            WhatsAppStatus(
                id = null,
                path = file.uri.toString(), // Use content URI
                lastModifiedTime =  getLastModifiedTime(context, file.uri),
                duration =Utils.getMediaDuration(
                    context, file.uri
                ),
                type = STATUS_TYPE.VIDEO,
                isArchived = false,
                fileName = file.name ?: ""
            )
        }
        println("getVideoStatus 3: Found $allImages")
        _videoWhatsAppStatus.postValue(allImages)

    }

    fun getSavedStatus(context: Context, folderUri: Uri?) {
        if (folderUri == null) {
            println("getSavedStatus: folderUri is null")
            _savedWhatsAppStatus.postValue(emptyList())
            return
        }

        val documentFile = DocumentFile.fromTreeUri(context, folderUri)

        if (documentFile == null || !documentFile.exists() || !documentFile.isDirectory) {
            println("getSavedStatus: invalid DocumentFile")
            _savedWhatsAppStatus.postValue(emptyList())
            return
        }

        val supportedExts = listOf("jpg", "jpeg", "png", "mp4")

        val allFiles = documentFile.listFiles()
            .filter { file ->
                file.isFile &&
                        !file.name.isNullOrBlank() &&
                        supportedExts.any { ext -> file.name!!.lowercase().endsWith(ext) }
            }

        if (allFiles.isEmpty()) {
            _savedWhatsAppStatus.postValue(emptyList())
            return
        }

        val result = allFiles.map { file ->
            val isVideo = file.name!!.endsWith(".mp4", ignoreCase = true)
            WhatsAppStatus(
                id = null,
                path = file.uri.toString(),
                lastModifiedTime = 0L, // SAF doesn't give lastModified
                duration = if (isVideo) Utils.getMediaDuration(context, file.uri) else 0L,
                type = if (isVideo) STATUS_TYPE.VIDEO else STATUS_TYPE.IMAGE,
                isArchived = false,
                fileName = file.name ?: ""
            )
        }

        _savedWhatsAppStatus.postValue(result)
    }



    private fun getLastModifiedTime(context: Context, uri: Uri): Long {
        val projection = arrayOf(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
        var lastModified: Long = 0L

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                if (columnIndex != -1) {
                    lastModified = cursor.getLong(columnIndex)
                }
            }
        }
        return lastModified
    }
}