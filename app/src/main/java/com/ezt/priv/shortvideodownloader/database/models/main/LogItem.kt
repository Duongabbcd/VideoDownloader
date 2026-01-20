package com.ezt.priv.shortvideodownloader.database.models.main

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ezt.priv.shortvideodownloader.database.models.expand.table.Format
import com.ezt.priv.shortvideodownloader.database.viewmodel.DownloadViewModel

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
