package com.ezt.priv.shortvideodownloader.database.models.expand.non_table

import com.ezt.priv.shortvideodownloader.database.models.main.HistoryItem
import com.ezt.priv.shortvideodownloader.database.models.main.DownloadItem
import com.ezt.priv.shortvideodownloader.database.models.main.CookieItem
import com.ezt.priv.shortvideodownloader.database.models.main.CommandTemplate
import com.ezt.priv.shortvideodownloader.database.models.main.TemplateShortcut
import com.ezt.priv.shortvideodownloader.database.models.main.SearchHistoryItem
import com.ezt.priv.shortvideodownloader.database.models.observeSources.ObserveSourcesItem

data class RestoreAppDataItem(
    var settings : List<BackupSettingsItem>? = null,
    var downloads: List<HistoryItem>? = null,
    var queued: List<DownloadItem>? = null,
    var scheduled: List<DownloadItem>? = null,
    var cancelled: List<DownloadItem>? = null,
    var errored: List<DownloadItem>? = null,
    var saved: List<DownloadItem>? = null,
    var cookies: List<CookieItem>? = null,
    var templates: List<CommandTemplate>? = null,
    var shortcuts: List<TemplateShortcut>? = null,
    var searchHistory: List<SearchHistoryItem>? = null,
    var observeSources: List<ObserveSourcesItem>? = null,
)

data class BackupSettingsItem(
    var key: String,
    var value: String,
    var type: String?
)