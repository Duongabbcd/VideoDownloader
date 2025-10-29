package com.ezt.priv.shortvideodownloader.ui.browse.viewmodel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.viewModelScope
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ezt.priv.shortvideodownloader.ui.browse.BrowserServicesProvider
import com.ezt.priv.shortvideodownloader.ui.browse.detector.SingleLiveEvent
import com.ezt.priv.shortvideodownloader.ui.browse.qualifier.VideoInfo
import com.ezt.priv.shortvideodownloader.ui.browse.scheduler.BaseViewModel
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.collections.indexOfFirst
import kotlin.collections.plus
import kotlin.collections.toMutableList

//@OpenForTesting
class MainViewModel @Inject constructor(
) : BaseViewModel() {

    var browserServicesProvider: BrowserServicesProvider? = null

    val openedUrl = ObservableField<String?>()

    val openedText = ObservableField<String?>()

    val isBrowserCurrent = ObservableBoolean(false)

    val currentItem = ObservableField<Int>()

    val offScreenPageLimit = ObservableField(4)

    // pair - format:url
    val selectedFormatTitle = ObservableField<Pair<String, String>?>()

    val currentOriginal = ObservableField<String>()

    val downloadVideoEvent = SingleLiveEvent<VideoInfo>()

    val openDownloadedVideoEvent = SingleLiveEvent<String>()

    val openNavDrawerEvent = SingleLiveEvent<Unit?>()

    var bookmarksList: ObservableField<MutableList<PageInfo>> = ObservableField(mutableListOf())

    private val executorSingle = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    private val executorMoverSingle = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    override fun start() {
    }

    override fun stop() {
        executorSingle.cancel()
        executorMoverSingle.cancel()
    }


}


@Entity(tableName = "PageInfo")
data class PageInfo(
    @ColumnInfo(name = "id")
    var id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "isSystem")
    @SerializedName("isSystem")
    @Expose
    var isSystem: Boolean = true,

    @ColumnInfo(name = "name")
    @SerializedName("name")
    @Expose
    var name: String = "",

    @PrimaryKey
    @ColumnInfo(name = "link")
    @SerializedName("link")
    @Expose
    var link: String = "",

    @ColumnInfo(name = "icon")
    @SerializedName("icon")
    @Expose
    var icon: String = "",

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    var favicon: ByteArray? = null,

    @ColumnInfo(name = "order")
    @SerializedName("order")
    @Expose
    var order: Int = 0
) {
    // TODO use regex
    fun getTitleFiltered(): String {
        return name
            .replace("www.", "")
            .replace(".com", "")
            .replaceFirstChar { it.uppercase() }
    }

    fun faviconBitmap(): Bitmap? {
        if (favicon == null) {
            return null
        }
        return BitmapFactory.decodeByteArray(favicon, 0, favicon?.size ?: 0)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PageInfo

        if (!favicon.contentEquals(other.favicon)) return false

        return link == other.link
    }

    override fun hashCode(): Int {
        return 31 * link.hashCode() * favicon.contentHashCode()
    }
}
