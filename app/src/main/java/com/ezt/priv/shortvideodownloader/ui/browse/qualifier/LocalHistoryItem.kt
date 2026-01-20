package com.ezt.priv.shortvideodownloader.ui.browse.qualifier

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(tableName = "LocalHistoryItem")
data class LocalHistoryItem(
    @PrimaryKey
    var id: String = UUID.randomUUID().toString(),
    var title: String? = null,
    var url: String,
    var datetime: Long = Date().time,

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    var favicon: ByteArray? = null
) {

    fun faviconBitmap(): Bitmap? {
        return BitmapFactory.decodeByteArray(favicon, 0, favicon?.size ?: 0)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LocalHistoryItem

        if (id != other.id) return false
        if (url != other.url) return false
        if (datetime != other.datetime) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + datetime.hashCode()
        return result
    }
}