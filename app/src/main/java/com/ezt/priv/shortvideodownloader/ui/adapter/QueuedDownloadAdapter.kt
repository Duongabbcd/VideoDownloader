package com.ezt.priv.shortvideodownloader.ui.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.database.models.expand.table.DownloadItemSimple
import com.ezt.priv.shortvideodownloader.database.viewmodel.DownloadViewModel
import com.ezt.priv.shortvideodownloader.databinding.QueuedDownloadCardBinding
import com.ezt.priv.shortvideodownloader.util.Extensions.loadThumbnail
import com.ezt.priv.shortvideodownloader.util.FileUtil
import com.ezt.priv.shortvideodownloader.util.UiUtil
import com.google.android.material.card.MaterialCardView

class QueuedDownloadAdapter(onItemClickListener: OnItemClickListener, activity: Activity, private var itemTouchHelper: ItemTouchHelper) : PagingDataAdapter<DownloadItemSimple, QueuedDownloadAdapter.ViewHolder>(
    DIFF_CALLBACK
) {
    private val onItemClickListener: OnItemClickListener
    private val activity: Activity
    val checkedItems: MutableSet<Long> = mutableSetOf()
    var inverted: Boolean
    var showDragHandle: Boolean
    private val sharedPreferences: SharedPreferences

    init {
        this.onItemClickListener = onItemClickListener
        this.activity = activity
        this.inverted = false
        this.showDragHandle = false
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
    }

    inner class ViewHolder(private val binding: QueuedDownloadCardBinding,
                           private val startDrag: (RecyclerView.ViewHolder) -> Unit) : RecyclerView.ViewHolder(binding.root) {
        val cardView: MaterialCardView
        init {
            cardView = itemView.findViewById(R.id.download_card_view)
        }

        fun bind(item: DownloadItemSimple?, showDragHandle: Boolean) {
            binding.apply {
                if (item == null) return
                downloadCardView.tag = item.id.toString()

                val uiHandler = Handler(Looper.getMainLooper())

                //DRAG HANDLE
                dragView.isVisible = showDragHandle
               dragView.setOnTouchListener { view, motionEvent ->
                    view.performClick()
                    if (motionEvent.actionMasked == MotionEvent.ACTION_DOWN){
                        startDrag(this@ViewHolder)
                    }
                    true
                }

                // THUMBNAIL ----------------------------------
                val hideThumb = sharedPreferences.getStringSet("hide_thumbnails", emptySet())!!.contains("queue")
                uiHandler.post { downloadsImageView.loadThumbnail(hideThumb, item.thumb) }

                duration.text = item.duration
                duration.isVisible = item.duration != "-1"


                // TITLE  ----------------------------------
                var titleStr = item.title.ifEmpty { item.playlistTitle.ifEmpty { item.url } }
                if (titleStr.length > 100) {
                    titleStr = titleStr.substring(0, 40) + "..."
                }
                title.text = titleStr

                //DOWNLOAD TYPE -----------------------------
                when(item.type){
                    DownloadViewModel.Type.audio -> downloadType.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        R.drawable.ic_music_formatcard, 0,0,0
                    )
                    DownloadViewModel.Type.video -> downloadType.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        R.drawable.ic_video_formatcard, 0,0,0
                    )
                    else -> downloadType.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        R.drawable.ic_terminal_formatcard, 0,0,0
                    )
                }


                if (item.format.format_note == "?" || item.format.format_note == "") formatNote!!.visibility =
                    View.GONE
                else formatNote!!.text = item.format.format_note.uppercase()

                val codecText =
                    if (item.format.encoding != "") {
                        item.format.encoding?.uppercase() ?: ""
                    }else if (item.format.vcodec != "none" && item.format.vcodec != ""){
                        item.format.vcodec.uppercase()
                    } else {
                        item.format.acodec?.uppercase() ?: ""
                    }
                if (codecText == "" || codecText == "none"){
                    codec.visibility = View.GONE
                }else{
                    codec.visibility = View.VISIBLE
                    codec.text = codecText
                }

                val fileSizeReadable = FileUtil.convertFileSize(item.format.filesize)
                if (fileSizeReadable == "?") fileSize.visibility = View.GONE
                else fileSize.text = fileSizeReadable

                options.setOnClickListener {
                    val popup = PopupMenu(activity, it)
                    popup.menuInflater.inflate(R.menu.queued_download_menu, popup.menu)
                    if (Build.VERSION.SDK_INT > 27) popup.menu.setGroupDividerEnabled(true)

                    popup.setOnMenuItemClickListener { m ->
                        when(m.itemId){
                            R.id.cancel -> {
                                onItemClickListener.onQueuedCancelClick(item.id)
                                popup.dismiss()
                            }
                            R.id.move_top -> {
                                onItemClickListener.onMoveQueuedItemToTop(item.id)
                                popup.dismiss()
                            }
                            R.id.move_bottom -> {
                                onItemClickListener.onMoveQueuedItemToBottom(item.id)
                                popup.dismiss()
                            }
                            R.id.copy_url -> {
                                UiUtil.copyLinkToClipBoard(activity, item.url)
                                popup.dismiss()
                            }
                        }
                        true
                    }

                    popup.show()

                }

                if ((checkedItems.contains(item.id) && !inverted) || (!checkedItems.contains(item.id) && inverted)) {
                    downloadCardView.isChecked = true
                    downloadCardView.strokeWidth = 5
                } else {
                    downloadCardView.isChecked = false
                    downloadCardView.strokeWidth = 0
                }
                downloadCardView.findViewById<View>(R.id.card_content).setOnClickListener {
                    if (checkedItems.size > 0 || inverted) {
                        checkCard(downloadCardView, item.id, position)
                    } else {
                        onItemClickListener.onQueuedCardClick(item.id)
                    }
                }

                downloadCardView.findViewById<View>(R.id.card_content).setOnLongClickListener {
                    checkCard(downloadCardView, item.id, position)
                    true
                }

            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = QueuedDownloadCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, startDrag)
    }

    private val startDrag: (RecyclerView.ViewHolder) -> Unit = {
        itemTouchHelper.startDrag(it)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, showDragHandle)
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

    @SuppressLint("NotifyDataSetChanged")
    fun toggleShowDragHandle(){
        showDragHandle = !showDragHandle
        notifyDataSetChanged()
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
        onItemClickListener.onQueuedCardSelect(card.isChecked, position)
    }

    interface OnItemClickListener {
        fun onQueuedCancelClick(itemID: Long)
        fun onMoveQueuedItemToTop(itemID: Long)
        fun onMoveQueuedItemToBottom(itemID: Long)
        fun onQueuedCardClick(itemID: Long)
        fun onQueuedCardSelect(isChecked: Boolean, position: Int)
    }

    companion object {
        private val DIFF_CALLBACK: DiffUtil.ItemCallback<DownloadItemSimple> = object : DiffUtil.ItemCallback<DownloadItemSimple>() {
            override fun areItemsTheSame(oldItem: DownloadItemSimple, newItem: DownloadItemSimple): Boolean {
                val ranged = arrayListOf(oldItem.id, newItem.id)
                return ranged[0] == ranged[1]
            }

            override fun areContentsTheSame(oldItem: DownloadItemSimple, newItem: DownloadItemSimple): Boolean {
                return oldItem.id == newItem.id && oldItem.title == newItem.title && oldItem.author == newItem.author && oldItem.thumb == newItem.thumb
            }
        }
    }
}