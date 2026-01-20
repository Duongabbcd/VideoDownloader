package com.ezt.priv.shortvideodownloader.util

import android.content.Context
import com.ezt.priv.shortvideodownloader.database.VideoDownloadDB
import com.ezt.priv.shortvideodownloader.database.models.expand.table.Format
import com.ezt.priv.shortvideodownloader.database.models.main.LogItem
import com.ezt.priv.shortvideodownloader.database.viewmodel.DownloadViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

class CrashListener(private val context: Context) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(p0: Thread, p1: Throwable) {
        CoroutineScope(SupervisorJob()).launch(Dispatchers.IO) {
            createLog("${p1.message}\n\n${p1.stackTrace.joinToString("\n")}")
        }
    }

    private suspend fun createLog(message: String){
        kotlin.runCatching {
            val db = VideoDownloadDB.getInstance(context)
            val dao = db.logDao
            dao.insert(LogItem(
                id = 0L,
                title = "APP CRASH",
                content = message,
                format = Format("", "", "", "", "", 0, "", "", "", "", "", ""),
                downloadType = DownloadViewModel.Type.command,
                downloadTime = System.currentTimeMillis()
            ))
        }
        exitProcess(0)
    }

    fun registerExceptionHandler(){
        Thread.setDefaultUncaughtExceptionHandler(this)
    }
}