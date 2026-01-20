package com.ezt.priv.shortvideodownloader.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.RingtoneManager
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.Log
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import kotlin.math.abs
import com.ezt.priv.shortvideodownloader.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

object Utils {

    fun checkTiramisu(input: String = ""): Boolean {
        return input.contains(
            "com.android.externalstorage.documents/tree/primary%3AAndroid%2Fmedia%2Fcom.whatsapp%2FWhatsApp%2FMedia%2F.Statuses",
            true
        )
    }

    fun getValidDocumentUri(mixedUriString: String): Uri {
        val mixedUri = Uri.parse(mixedUriString)

        // Extract tree URI part (folder)
        val treeUri = Uri.parse(
            "content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fmedia%2Fcom.whatsapp%2FWhatsApp%2FMedia%2F.Statuses"
        )

        // Extract document ID (file path part) from mixedUri last segment after "document/"
        val documentId = mixedUri.lastPathSegment?.substringAfter("document/") ?: return Uri.EMPTY

        // The documentId will look like: "primary:Android/media/com.whatsapp/WhatsApp/Media/.Statuses/525ff64a210a4fc4a75d8da543c9d562.mp4"
        // Build valid document URI using tree URI and document ID
        val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
        return documentUri
    }


    fun Context.hideKeyBoard(editText: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(editText.windowToken, 0)
    }

    fun Context.showKeyboard(editText: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    /**
     * Hides keyboard and shows bottom sheet dialog with proper timing to prevent floating issue
     */
    fun Context.hideKeyboardAndShowBottomSheet(editText: View, showBottomSheet: () -> Unit) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(editText.windowToken, 0)

        // Delay showing the bottom sheet to allow keyboard to fully hide
        editText.postDelayed({
            showBottomSheet()
        }, 100)
    }

    fun Context.getColorFromAttr(attr: Int): Int {
        val typedValue = TypedValue()
        val theme: Resources.Theme = theme
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    fun getBitmapFromUri(uri: Uri, context: Context): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun getAlbumArt(context: Context, filePath: String): Bitmap? {
        val uri = Uri.parse(filePath)
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)

            val art = retriever.embeddedPicture
            println("art is null: ${art == null}")

            if (art != null) {
                return BitmapFactory.decodeByteArray(art, 0, art.size).also {
                    println("getAlbumArt: ${it == null}")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            retriever.release()
        }
        return null
    }

    fun getAlbumArtBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val retriever = MediaMetadataRetriever()
            val fd = context.contentResolver.openFileDescriptor(uri, "r")?.fileDescriptor
            retriever.setDataSource(fd)
            val art = retriever.embeddedPicture
            retriever.release()
            if (art != null) BitmapFactory.decodeByteArray(art, 0, art.size) else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getAudioDuration(url: String): Long {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(url, HashMap())
        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        retriever.release()
        return durationStr?.toLongOrNull() ?: 0L
    }

    @SuppressLint("DefaultLocale")
    fun formatDuration(duration: Long): String {
        println("formatDuration: $duration")
        val minutes =duration / 60
        val seconds = duration % 60
        val result = String.format("%02d:%02d", minutes, seconds)
        return result
    }

    fun View.setOnSwipeListener(listener: GestureDetector.SimpleOnGestureListener) {
        val gestureDetector = GestureDetector(context, listener)
        this.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }


    fun Int.formatWithComma(): String {
        return NumberFormat.getNumberInstance(Locale.US).format(this)
    }


    fun getMP3Metadata(uri: Uri, context: Context): Pair<String, String> {
        val retriever = MediaMetadataRetriever()
//        var title = context.resources.getString(R.string.unknown_song)
        var title =  ""
//        var artist = context.resources.getString(R.string.unknown_artist)
        var artist = ""

        try {
            retriever.setDataSource(context, uri)
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: title
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: artist
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            retriever.release()
        }

        return Pair(title, artist)
    }

    fun blurBitmap(context: Context, bitmap: Bitmap, radius: Float = 40f): Bitmap {
        val inputBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.width, bitmap.height, false)
        val outputBitmap = Bitmap.createBitmap(inputBitmap)

        val renderScript = RenderScript.create(context)
        val input = Allocation.createFromBitmap(renderScript, inputBitmap)
        val output = Allocation.createTyped(renderScript, input.type)
        val script = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))

        script.setRadius(radius)
        script.setInput(input)
        script.forEach(output)
        output.copyTo(outputBitmap)
        renderScript.destroy()

        return outputBitmap
    }

    fun getFirstRingtone(context: Context, type: Int): RingtoneItem? {
        val ringtoneManager = RingtoneManager(context)
        ringtoneManager.setType(type)

        val cursor = ringtoneManager.cursor
        var result: RingtoneItem? = null

        if (cursor.moveToFirst()) {
            val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
            val uri = ringtoneManager.getRingtoneUri(cursor.position)
            result = RingtoneItem(title, uri)
        }
        cursor.close()
        return result
    }

    fun getCacheSize(context: Context): String {
        val cacheDir = context.cacheDir
        val sizeInBytes = getFolderSize(cacheDir)
        return formatSize(sizeInBytes)
    }

    fun getFolderSize(dir: File): Long {
        var size: Long = 0
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) getFolderSize(file) else file.length()
        }
        return size
    }

    @SuppressLint("DefaultLocale")
    fun formatSize(size: Long): String {
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024

        return when {
            size >= gb -> String.format("%.2f GB", size / gb)
            size >= mb -> String.format("%.2f MB", size / mb)
            size >= kb -> String.format("%.2f KB", size / kb)
            else -> "$size Bytes"
        }
    }

    suspend fun downloadCallScreenFile(
        context: Context,
        fileUrl: String,
        folderName: String = "background"
    ): DownloadResult? =
        withContext(Dispatchers.IO) {
            val client = OkHttpClient()

            val backgroundDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                folderName
            )

            clearFolder(backgroundDir)
            backgroundDir.mkdirs()

            val extension = fileUrl.substringAfterLast('.', "").lowercase()
            val backgroundFileName = when (extension) {
                "mp4" -> "background.mp4"
                "png", "jpg", "jpeg" -> "background.png"
                else -> "$folderName.json"
            }


            val backgroundFile = File(backgroundDir, backgroundFileName)

            val backgroundDownloaded = downloadFile(client, fileUrl, backgroundFile)
            if (!backgroundDownloaded) return@withContext null

            DownloadResult(
                backgroundPath = backgroundFile.absolutePath
            )
        }

    private fun clearFolder(folder: File) {
        if (folder.exists()) {
            folder.listFiles()?.forEach { it.delete() }
        }
    }

    private fun downloadFile(client: OkHttpClient, url: String, targetFile: File): Boolean {
        return try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.byteStream().use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input?.copyTo(output)
                    }
                }
                Log.d("DownloadFile", "Downloaded: ${targetFile.absolutePath}")
                true
            } else {
                Log.e("DownloadFile", "Failed: $url")
                false
            }.also { response.close() }
        } catch (e: Exception) {
            Log.e("DownloadFile", "Error downloading file", e)
            false
        }
    }

    fun String.formatTime(): String {
        val instant = Instant.parse(this)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneOffset.UTC) // optional, but use if you want UTC consistency

        val formatted = formatter.format(instant)
        return formatted
    }

    @SuppressLint("DefaultLocale")
    fun formatDurationTime(ms: Long): String {
        val minutes = (ms / 1000) / 60
        val seconds = (ms / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    fun getRelativeTime(lastModified: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - lastModified
        println("getRelativeTime: $now $lastModified and $diff")
        val seconds = abs(diff / 1000)
        val minutes =  abs(seconds / 60)
        val hours = abs(minutes / 60)
        val days = abs(hours / 24)

        return when {
            seconds < 60 -> "just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> {
                val date = java.text.SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH)
                date.format(java.util.Date(lastModified))
            }
        }
    }

    fun getMediaDuration(context: Context, uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationStr?.toLong() ?: 0L
        } catch (e: Exception) {
            0L
        } finally {
            retriever.release()
        }
    }

    fun getNormalMediaDuration(filePath: String): Long {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val durationStr =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            return durationStr?.toLong() ?: 0L
        } catch (e: Exception) {
            e.printStackTrace()
            return 0L
        }
    }
}

data class DownloadResult(
    val backgroundPath: String
)

open class SwipeGestureListener(val activity: Activity) : GestureDetector.SimpleOnGestureListener() {
    private val SWIPE_THRESHOLD = 100
    private val SWIPE_VELOCITY_THRESHOLD = 100

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        println("SwipeGestureListener - onFling called: vx=$velocityX, vy=$velocityY")
        try {
            if (e1 == null || e2 == null) return false

            val diffY = e2.y - e1.y
            val diffX = e2.x - e1.x

            // 👉 Horizontal swipe detection
            if (abs(diffX) > abs(diffY)) {
                if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        onSwipeRight()
                    } else {
                        onSwipeLeft()
                    }
                    return true
                }
            }
            // 👇 Optional: vertical swipe detection (like you had)
            else if (abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffY > 0) {
                    activity.finish()
                    activity.overridePendingTransition(
                        R.anim.fade_in_quick, R.anim.exit_to_top
                    )
                }
                return true
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return false
    }

    open fun onSwipeLeft() {
        println("Swiped LEFT")
    }

    open fun onSwipeRight() {
        println("Swiped RIGHT")
    }
}

data class RingtoneItem(
    val title: String,
    val uri: Uri
)