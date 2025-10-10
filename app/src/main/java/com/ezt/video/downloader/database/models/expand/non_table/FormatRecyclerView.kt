package com.ezt.video.downloader.database.models.expand.non_table

import com.ezt.video.downloader.database.models.expand.table.Format

data class FormatRecyclerView(
    var label: String? = null,
    var format: Format? = null,
)