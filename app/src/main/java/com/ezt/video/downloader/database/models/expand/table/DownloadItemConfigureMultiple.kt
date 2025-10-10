package com.ezt.video.downloader.database.models.expand.table

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ezt.video.downloader.database.viewmodel.DownloadViewModel

@Entity(tableName = "downloads")
data class DownloadItemConfigureMultiple(
    @PrimaryKey(autoGenerate = true)
    var id: Long,
    var url: String,
    var title: String,
    var thumb: String,
    var duration: String,
    var container: String,
    var format: Format,
    var allFormats: MutableList<Format>,
    var audioPreferences: AudioPreferences,
    var videoPreferences: VideoPreferences,
    @ColumnInfo(defaultValue = "Queued")
    var status: String,
    var type: DownloadViewModel.Type,
    var incognito: Boolean = false
)