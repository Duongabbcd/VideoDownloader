package com.ezt.video.downloader.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ezt.video.downloader.database.VideoDownloadDB
import com.ezt.video.downloader.database.repository.DownloadRepository
import com.ezt.video.downloader.util.NotificationUtil
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class PauseDownloadNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) {
        val result = goAsync()
        val id = intent.getIntExtra("itemID", 0)
        if (id != 0) {
            runCatching {
                val title = intent.getStringExtra("title")
                val notificationUtil = NotificationUtil(c)
                notificationUtil.cancelDownloadNotification(id)
                YoutubeDL.getInstance().destroyProcessById(id.toString())
                val dbManager = VideoDownloadDB.getInstance(c)
                CoroutineScope(Dispatchers.IO).launch{
                    try {
                        val item = dbManager.downloadDao.getDownloadById(id.toLong())
                        item.status = DownloadRepository.Status.Paused.toString()
                        dbManager.downloadDao.update(item)
                    }finally {
                        withContext(Dispatchers.Main){
                            notificationUtil.createResumeDownload(id, title)
                            result.finish()
                        }
                    }
                }
            }
        }
    }
}