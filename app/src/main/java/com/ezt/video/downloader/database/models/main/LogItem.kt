package com.ezt.video.downloader.database.models.main

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ezt.video.downloader.database.models.expand.table.Format
import com.ezt.video.downloader.database.viewmodel.DownloadViewModel

@Entity(tableName = "logs")
data class LogItem(
    @PrimaryKey(autoGenerate = true)
    var id: Long,
    var title: String,
    var content: String,
    var format: Format,
    var downloadType: DownloadViewModel.Type,
    var downloadTime: Long,
)
