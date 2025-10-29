package com.ezt.priv.shortvideodownloader.ui.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.SharedPreferences
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.database.models.main.HistoryItem
import com.ezt.priv.shortvideodownloader.database.viewmodel.DownloadViewModel
import com.ezt.priv.shortvideodownloader.databinding.HistoryCardBinding
import com.ezt.priv.shortvideodownloader.databinding.HistoryCardMultipleBinding
import com.ezt.priv.shortvideodownloader.util.Common.gone
import com.ezt.priv.shortvideodownloader.util.Extensions.loadThumbnail
import com.ezt.priv.shortvideodownloader.util.Extensions.popup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class HistoryPaginatedAdapter(onItemClickListener: OnItemClickListener, activity: Activity) : PagingDataAdapter<HistoryItem, HistoryPaginatedAdapter.ViewHolder>(
    DIFF_CALLBACK
) {
    val checkedItems: MutableSet<Long>
    var inverted: Boolean
    private val onItemClickListener: OnItemClickListener
    private val activity: Activity
    private val sharedPreferences: SharedPreferences

    init {
        checkedItems = mutableSetOf()
        this.inverted = false
        this.onItemClickListener = onItemClickListener
        this.activity = activity
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
    }

    sealed class ViewHolder(binding: ViewBinding) : RecyclerView.ViewHolder(binding.root) {
        class Single(val binding: HistoryCardBinding) : ViewHolder(binding)
        class Multiple(val binding: HistoryCardMultipleBinding) : ViewHolder(binding)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            0 -> {
                val binding = HistoryCardBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                ViewHolder.Single(binding)
            }
            else -> {
                val binding = HistoryCardMultipleBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                ViewHolder.Multiple(binding)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position)?.downloadPath?.size == 1) 0 else 1
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position) ?: return

        when (holder) {
            is ViewHolder.Single -> bindCommon(holder.binding.downloadsCardView, holder.binding, item, position)
            is ViewHolder.Multiple -> bindCommon(holder.binding.downloadsCardView, holder.binding, item, position)
        }

    }


    private fun <T : ViewBinding> bindCommon(
        card: MaterialCardView,
        binding: T,
        item: HistoryItem,
        position: Int
    ) {
        card.tag = item.id.toString()
        card.popup()


        val uiHandler = Handler(Looper.getMainLooper())
        val thumbnail = card.findViewById<ImageView>(R.id.downloads_image_view)
        val noImage = card.findViewById<ImageView>(R.id.noImage)

        // THUMBNAIL ----------------------------------
        val hideThumb = sharedPreferences.getStringSet("hide_thumbnails", emptySet())!!.contains("downloads")
        uiHandler.post { val check = thumbnail.loadThumbnail(hideThumb, item!!.thumb)
            noImage.isVisible = !check
        }

        // TITLE  ----------------------------------
        val itemTitle = card.findViewById<TextView>(R.id.downloads_title)
        var title = item!!.title.ifEmpty { item.url }
        if (title.length > 100) {
            title = title.substring(0, 40) + "..."
        }
        itemTitle.text = title

        // Bottom Info ----------------------------------
        val author = card.findViewById<TextView>(R.id.downloads_info_bottom)
        author.text = item.author

        val length = card.findViewById<TextView>(R.id.length)
        length.text = if(item.downloadPath.size == 1 && item.duration != "-1") item.duration else ""


        // TIME DOWNLOADED  ----------------------------------
        val datetime = card.findViewById<TextView>(R.id.downloads_info_time)
        datetime.text = SimpleDateFormat(android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "ddMMMyyyy - HHmm"), Locale.getDefault()).format(item.time * 1000L)

        // BUTTON ----------------------------------
        val btn = card.findViewById<FloatingActionButton>(R.id.downloads_download_button_type)
        var filesPresent = true

        //IS IN THE FILE SYSTEM?
        if (item.downloadPath.all { !File(it).exists() && it.isNotBlank()}) {
            filesPresent = false
            thumbnail.colorFilter = ColorMatrixColorFilter(object : ColorMatrix() {
                init {
                    setSaturation(0f)
                }
            })
            thumbnail.alpha = 0.7f
            btn.backgroundTintList = MaterialColors.getColorStateList(activity, R.attr.colorSurface, ContextCompat.getColorStateList(activity, android.R.color.transparent)!!)
        }else{
            thumbnail.alpha = 1f
            thumbnail.colorFilter = null
            btn.backgroundTintList = MaterialColors.getColorStateList(activity, R.attr.colorPrimaryContainer, ContextCompat.getColorStateList(activity, android.R.color.transparent)!!)
        }

        if (item.type == DownloadViewModel.Type.audio) {
            if (filesPresent) btn.setImageResource(R.drawable.ic_music_downloaded) else {
                btn.setImageResource(R.drawable.ic_music)
            }
        } else if (item.type == DownloadViewModel.Type.video) {
            if (filesPresent) btn.setImageResource(R.drawable.ic_video_downloaded) else btn.setImageResource(R.drawable.ic_video)
        }else{
            if (filesPresent) btn.setImageResource(R.drawable.ic_terminal) else btn.setImageResource(R.drawable.baseline_code_off_24)
        }
        if (btn.hasOnClickListeners()) btn.setOnClickListener(null)
        btn.isClickable = filesPresent
        btn.gone()

        if ((checkedItems.contains(item.id) && !inverted) || (!checkedItems.contains(item.id) && inverted)) {
            card.isChecked = true
            card.strokeWidth = 5
        } else {
            card.isChecked = false
            card.strokeWidth = 0
        }
        val finalFilePresent = filesPresent
        card.setOnLongClickListener {
            checkCard(card, item.id, position)
            true
        }
        card.setOnClickListener {
            if (checkedItems.size > 0 || inverted) {
                checkCard(card, item.id, position)
            } else {
                onItemClickListener.onCardClick(item.id, finalFilePresent)
            }
        }

        btn.setOnClickListener {
            onItemClickListener.onButtonClick(item.id, finalFilePresent)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearCheckedItems() {
        inverted = false
        checkedItems.clear()
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun checkAll() {
        checkedItems.clear()
        inverted = true
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun checkMultipleItems(list: List<Long>){
        checkedItems.clear()
        inverted = false
        checkedItems.addAll(list)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun invertSelected() {
        inverted = !inverted
        notifyDataSetChanged()
    }

    fun getSelectedObjectsCount(totalSize: Int) : Int{
        return if (inverted){
            totalSize - checkedItems.size
        }else{
            checkedItems.size
        }
    }

    private fun checkCard(card: MaterialCardView, itemID: Long, position: Int) {
        if (card.isChecked) {
            card.strokeWidth = 0
            if (inverted) checkedItems.add(itemID)
            else checkedItems.remove(itemID)
        } else {
            card.strokeWidth = 5
            if (inverted) checkedItems.remove(itemID)
            else checkedItems.add(itemID)
        }
        card.isChecked = !card.isChecked
        onItemClickListener.onCardSelect(card.isChecked, position)
    }

    interface OnItemClickListener {
        fun onButtonClick(itemID: Long, filePresent: Boolean)
        fun onCardClick(itemID: Long, filePresent: Boolean)
        fun onCardSelect(isChecked: Boolean, position: Int)
    }

    companion object {
        private val DIFF_CALLBACK: DiffUtil.ItemCallback<HistoryItem> = object : DiffUtil.ItemCallback<HistoryItem>() {
            override fun areItemsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
                val ranged = arrayListOf(oldItem.id, newItem.id)
                return ranged[0] == ranged[1]
            }

            override fun areContentsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
                return oldItem.time == newItem.time
            }
        }
    }
}