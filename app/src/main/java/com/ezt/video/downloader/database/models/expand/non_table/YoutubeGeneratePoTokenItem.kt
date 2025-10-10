package com.ezt.video.downloader.database.models.expand.non_table

data class YoutubeGeneratePoTokenItem(
    var enabled: Boolean,
    var clients: MutableList<String>,
    var poTokens: MutableList<YoutubePoTokenItem>,
    var visitorData: String,
    var useVisitorData: Boolean
)