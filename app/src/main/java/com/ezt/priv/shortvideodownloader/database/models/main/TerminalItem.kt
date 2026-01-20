package com.ezt.priv.shortvideodownloader.database.models.main

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "terminalDownloads")
data class TerminalItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val command: String,
    val log: String? = null
)
