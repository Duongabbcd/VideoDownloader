package com.ezt.video.downloader.work

import android.content.Context
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.ezt.video.downloader.MyApplication
import com.ezt.video.downloader.database.VideoDownloadDB
import com.ezt.video.downloader.database.repository.DownloadRepository
import com.ezt.video.downloader.util.FileUtil
import com.ezt.video.downloader.util.NotificationUtil
import java.io.File

class CleanUpLeftoverDownloads(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val notificationUtil = NotificationUtil(MyApplication.instance)
        val id = System.currentTimeMillis().toInt()

        val notification = notificationUtil.createDeletingLeftoverDownloadsNotification()
        if (Build.VERSION.SDK_INT >= 33) {
            setForegroundAsync(ForegroundInfo(id, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC))
        }else{
            setForegroundAsync(ForegroundInfo(id, notification))
        }

        val dbManager = VideoDownloadDB.getInstance(context)
        val downloadRepo = DownloadRepository(dbManager.downloadDao)
        downloadRepo.deleteCancelled()
        downloadRepo.deleteErrored()

        val activeDownloadCount = downloadRepo.getActiveDownloadsCount()
        if (activeDownloadCount == 0){
            File(FileUtil.getCachePath(context)).deleteRecursively()
        }

        return Result.success()
    }

}