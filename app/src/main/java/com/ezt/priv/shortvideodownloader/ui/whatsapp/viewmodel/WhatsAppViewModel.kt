package com.ezt.priv.shortvideodownloader.ui.whatsapp.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ezt.priv.shortvideodownloader.database.models.expand.non_table.STATUS_TYPE
import com.ezt.priv.shortvideodownloader.database.models.expand.non_table.WhatsAppStatus
import com.ezt.priv.shortvideodownloader.util.FileUtil.getDefaultStatusPath
import com.ezt.priv.shortvideodownloader.util.Utils
import java.io.File

class WhatsAppViewModel(
    private val application: Application
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

    fun getSavedStatus() {
        val statusDir = File(getDefaultStatusPath(application))

        if (!statusDir.exists() || !statusDir.isDirectory) {
            _savedWhatsAppStatus.postValue(emptyList())
            return
        }

        val allFiles = statusDir.listFiles()
            ?.filter {
                it.isFile && it.extension.lowercase() in listOf(
                    "mp4",
                    "jpeg",
                    "png",
                    "jpg"
                )
            }
            ?: emptyList()

        if (allFiles.isEmpty()) {
            _savedWhatsAppStatus.postValue(emptyList())
            return
        }

        val allStatuses = allFiles.map { file ->
            val ext = file.extension.lowercase()
            val isVideo = ext == "mp4"

            WhatsAppStatus(
                id = null,
                path = file.absolutePath,
                lastModifiedTime = file.lastModified(),
                duration = if (isVideo) Utils.getNormalMediaDuration(file.absolutePath) else 0L,
                type = if (isVideo) STATUS_TYPE.VIDEO else STATUS_TYPE.IMAGE,
                isArchived = false,
                fileName = file.name
            )
        }

        _savedWhatsAppStatus.postValue(allStatuses)
    }

    private fun getLastModifiedTime(context: Context, uri: Uri): Long {
        val projection = arrayOf(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
        var lastModified = 0L

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