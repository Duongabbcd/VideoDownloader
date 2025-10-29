package com.ezt.priv.shortvideodownloader.database.models.expand.non_table

import com.ezt.priv.shortvideodownloader.database.models.main.CommandTemplate
import com.ezt.priv.shortvideodownloader.database.models.main.TemplateShortcut
import kotlinx.serialization.Serializable

@Serializable
data class CommandTemplateExport(
    val templates: List<CommandTemplate>,
    val shortcuts: List<TemplateShortcut>
)
