package com.ezt.priv.shortvideodownloader.database.models.expand.non_table

import java.io.Serializable

enum class STATUS_TYPE {
    IMAGE,
    VIDEO,
    NOTHING
}

data class WhatsAppStatus(
    var id: Int? = null,
    var path: String = "",
    var lastModifiedTime: Long = 0L,
    var duration: Long = 0L,
    var type: STATUS_TYPE = STATUS_TYPE.IMAGE,
    var isArchived: Boolean = false,
    var fileName: String = ""
): Serializable

