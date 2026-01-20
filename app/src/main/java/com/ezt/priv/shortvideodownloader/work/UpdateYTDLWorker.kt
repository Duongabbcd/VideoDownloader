package com.ezt.priv.shortvideodownloader.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ezt.priv.shortvideodownloader.util.UpdateUtil

class UpdateYTDLWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        UpdateUtil(context).updateYoutubeDL()
        return Result.success()
    }

}