package com.ezt.priv.shortvideodownloader.database.models.expand.non_table

data class YoutubePlayerClientItem(
    var playerClient: String,
    var poTokens: MutableList<YoutubePoTokenItem>,
    var enabled: Boolean = true,
    var useOnlyPoToken: Boolean = false,
    var urlRegex: MutableList<String> = mutableListOf()
)

data class YoutubePoTokenItem(
    var context: String,
    var token: String
)