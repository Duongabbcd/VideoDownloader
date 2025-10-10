package com.ezt.video.downloader.database.models.expand.non_table

import com.ezt.video.downloader.database.models.main.CommandTemplate
import com.ezt.video.downloader.database.models.main.TemplateShortcut
import kotlinx.serialization.Serializable

@Serializable
data class CommandTemplateExport(
    val templates: List<CommandTemplate>,
    val shortcuts: List<TemplateShortcut>
)
