package com.ezt.video.downloader.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ezt.video.downloader.util.UpdateUtil

class UpdateYTDLWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        UpdateUtil(context).updateYoutubeDL()
        return Result.success()
    }

}