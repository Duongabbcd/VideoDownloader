package com.ezt.priv.shortvideodownloader.ui.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.database.models.expand.table.DownloadItemConfigureMultiple
import com.ezt.priv.shortvideodownloader.database.viewmodel.DownloadViewModel
import com.ezt.priv.shortvideodownloader.databinding.DownloadCardBinding
import com.ezt.priv.shortvideodownloader.util.Extensions.loadThumbnail
import com.ezt.priv.shortvideodownloader.util.Extensions.popup
import com.ezt.priv.shortvideodownloader.util.FileUtil
import com.google.android.material.button.MaterialButton
import java.util.Locale

class ConfigureMultipleDownloadsAdapter(onItemClickListener: OnItemClickListener, activity: Activity) : ListAdapter<DownloadItemConfigureMultiple?, ConfigureMultipleDownloadsAdapter.ViewHolder>(
    AsyncDifferConfig.Builder(
    DIFF_CALLBACK
).build()) {
    private val onItemClickListener: OnItemClickListener
    private val activity: Activity
    private val sharedPreferences : SharedPreferences
    private var checkedItems: MutableSet<Long> = mutableSetOf()
    private var currentItems: Set<Long> = setOf()
    private var _isCheckingItems: Boolean = false
    private var inverted: Boolean = false

    init {
        this.onItemClickListener = onItemClickListener
        this.activity = activity
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
    }

    inner class ViewHolder(private val binding: DownloadCardBinding, private val activity: Activity, private val sharedPreferences: SharedPreferences,
                     private val onItemClickListener: OnItemClickListener
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DownloadItemConfigureMultiple?) {
            binding.apply {
                downloadCardView.popup()
                if (item == null) return
                downloadCardView.tag = item.id.toString()

                val uiHandler = Handler(Looper.getMainLooper())

                // THUMBNAIL ----------------------------------
                val hideThumb = sharedPreferences.getStringSet("hide_thumbnails", emptySet())!!.contains("home")
                uiHandler.post { downloadsImageView.loadThumbnail(hideThumb, item.thumb) }

                duration.text = item.duration

                // TITLE  ----------------------------------
                var titleStr = item.title
                if (titleStr.length > 100) {
                    titleStr = titleStr.substring(0, 40) + "..."
                }
                title.text = titleStr

                downloadType.isVisible = false

                // Format Note ----------------------------------
                if (item.format.format_note.isNotEmpty()){
                    formatNote.text = item.format.format_note.uppercase(Locale.getDefault())
                    formatNote.visibility = View.GONE
                }else{
                    formatNote.visibility = View.GONE
                }

                incognitoLabel.isVisible = item.incognito

                val codecText =
                    if (item.format.encoding != "") {
                        item.format.encoding?.uppercase() ?: ""
                    }else if (item.format.vcodec != "none" && item.format.vcodec != ""){
                        item.format.vcodec.uppercase()
                    } else {
                        item.format.acodec?.uppercase() ?: ""
                    }
                if (codecText == "" || codecText == "none" || codecText == "DEFAULT"){
                    codec.visibility = View.GONE
                }else{
                    codec.visibility = View.GONE
                    codec.text = codecText
                }

                container.isVisible = item.container.isNotBlank()
                container.text = item.container.uppercase()

                val fileSizeReadable = if(item.type != DownloadViewModel.Type.video){
                    FileUtil.convertFileSize(item.format.filesize)
                }else{
                    if (item.format.filesize < 10L) {
                        FileUtil.convertFileSize(0)
                    }else{
                        val preferredAudioFormatIDs = item.videoPreferences.audioFormatIDs
                        val audioFilesize = if (item.videoPreferences.removeAudio) {
                            0
                        }else{
                            item.allFormats
                                .filter { preferredAudioFormatIDs.contains(it.format_id) }
                                .sumOf { it.filesize }
                        }

                        FileUtil.convertFileSize(item.format.filesize + audioFilesize)
                    }

                }
                if (fileSizeReadable == "?") fileSize.visibility = View.GONE
                else {
                    fileSize.text = fileSizeReadable
                    fileSize.visibility = View.VISIBLE
                }

                // Type Icon Button
                if (actionButton.hasOnClickListeners()) actionButton.setOnClickListener(null)

                actionButton.setOnClickListener {
                    onItemClickListener.onButtonClick(item.id)
                }

                when(item.type) {
                    DownloadViewModel.Type.audio -> {
                        actionButton.setIconResource(R.drawable.ic_music)
                        actionButton.contentDescription = activity.getString(R.string.audio)
                    }
                    DownloadViewModel.Type.video -> {
                        actionButton.setIconResource(R.drawable.ic_video)
                        actionButton.contentDescription = activity.getString(R.string.video)
                    }
                    else -> {
                        actionButton.setIconResource(R.drawable.ic_terminal)
                        actionButton.contentDescription = activity.getString(R.string.command)
                    }
                }

                val checkbox = downloadCardView.findViewById<CheckBox>(R.id.checkBox)
                checkbox.isVisible = _isCheckingItems
                checkbox.isChecked = (checkedItems.contains(item.id) && !inverted) || (inverted && !checkedItems.contains(item.id))
                checkbox.setOnClickListener {
                    if (checkbox.isChecked) {
                        if (inverted) checkedItems.remove(item.id)
                        else checkedItems.add(item.id)
                        onItemClickListener.onCardChecked(item.id)
                    }else {
                        if (inverted) checkedItems.add(item.id)
                        else checkedItems.remove(item.id)
                        onItemClickListener.onCardUnChecked(item.id)
                    }
                }

                index.isVisible = _isCheckingItems
                index.text = (position + 1).toString()

                downloadCardView.setOnClickListener {
                    if (_isCheckingItems) {
                        checkbox.performClick()
                    }else {
                        onItemClickListener.onCardClick(item.id)
                    }
                }

                downloadCardView.setOnLongClickListener {
                    if (_isCheckingItems) {
                        checkbox.performClick()
                    }else{
                        onItemClickListener.onDelete(item.id)
                    }
                    true
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DownloadCardBinding.inflate(LayoutInflater.from(parent.context), parent,false)
        return ViewHolder(binding, activity, sharedPreferences, onItemClickListener)
    }

    fun isCheckingItems() : Boolean {
        return _isCheckingItems
    }

    fun getCheckedItemsOrNull(): List<Long>? {
        if (!_isCheckingItems) return null

        val res = if (inverted) {
            currentItems.filter { !checkedItems.contains(it) }
        }else {
            checkedItems
        }

        return res.toList().ifEmpty { null }
    }

    fun getCheckedItemsSize() : Int {
        return if (inverted){
            currentItems.size - checkedItems.size
        }else{
            checkedItems.size
        }
    }

    fun removeItemsFromCheckList(ids: List<Long>) {
        checkedItems.removeAll(ids.toSet())
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearCheckedItems() {
        _isCheckingItems = false
        inverted = false
        checkedItems = mutableSetOf()
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun selectItems(ids: List<Long>) {
        checkedItems = mutableSetOf()
        inverted = false
        checkedItems.addAll(ids)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun checkAll() {
        checkedItems = mutableSetOf()
        inverted = true
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun invertSelected() {
        inverted = !inverted
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun initCheckingItems(itemIDs: List<Long>) {
        currentItems = itemIDs.toSet()
        _isCheckingItems = true
        checkedItems = mutableSetOf()
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)

    }

    interface OnItemClickListener {
        fun onButtonClick(id: Long)
        fun onCardClick(id: Long)
        fun onCardChecked(id: Long)
        fun onCardUnChecked(id: Long)
        fun onDelete(id: Long)
    }

    companion object {
        private val DIFF_CALLBACK: DiffUtil.ItemCallback<DownloadItemConfigureMultiple> = object : DiffUtil.ItemCallback<DownloadItemConfigureMultiple>() {
            override fun areItemsTheSame(oldItem: DownloadItemConfigureMultiple, newItem: DownloadItemConfigureMultiple): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: DownloadItemConfigureMultiple, newItem: DownloadItemConfigureMultiple): Boolean {
                return oldItem.title == newItem.title &&
                    oldItem.type == newItem.type &&
                    oldItem.container == newItem.container &&
                    oldItem.videoPreferences == newItem.videoPreferences &&
                    oldItem.format == newItem.format &&
                    oldItem.incognito == newItem.incognito
            }
        }
    }
}