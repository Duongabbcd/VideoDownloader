package com.ezt.priv.shortvideodownloader.ui.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.database.models.expand.table.DownloadItemSimple
import com.ezt.priv.shortvideodownloader.database.repository.DownloadRepository
import com.ezt.priv.shortvideodownloader.database.viewmodel.DownloadViewModel
import com.ezt.priv.shortvideodownloader.databinding.DownloadCardBinding
import com.ezt.priv.shortvideodownloader.util.Common.gone
import com.ezt.priv.shortvideodownloader.util.Extensions.loadThumbnail
import com.ezt.priv.shortvideodownloader.util.Extensions.popup
import com.ezt.priv.shortvideodownloader.util.FileUtil
import com.google.android.material.card.MaterialCardView

class GenericDownloadAdapter(
    private val onItemClickListener: OnItemClickListener,
    private val activity: Activity
) : PagingDataAdapter<DownloadItemSimple, GenericDownloadAdapter.ViewHolder>(
    DIFF_CALLBACK
) {
    val checkedItems: MutableSet<Long> = mutableSetOf()
    var inverted: Boolean
    private val sharedPreferences: SharedPreferences

    init {
        this.inverted = false
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
    }

    inner class ViewHolder(private val binding: DownloadCardBinding) : RecyclerView.ViewHolder(binding.root) {
       fun bind(item: DownloadItemSimple?) {
           binding.apply {
               downloadCardView.popup()

               if (item == null) return
               downloadCardView.tag = item.id.toString()

               val uiHandler = Handler(Looper.getMainLooper())

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

               incognitoLabel.isVisible = item.incognito

               if (item.format.format_note == "?" || item.format.format_note == "") formatNote.visibility =
                   View.GONE
               else formatNote.text = item.format.format_note.uppercase()

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

               // ACTION BUTTON ----------------------------------
               if (actionButton.hasOnClickListeners()) actionButton.setOnClickListener(null)

               when(item.status){
                   DownloadRepository.Status.Cancelled.toString() -> {
                       actionButton.setIconResource(R.drawable.ic_refresh)
                       actionButton.contentDescription = activity.getString(R.string.download)
                   }
                   DownloadRepository.Status.Saved.toString() -> {
                       actionButton.setIconResource(R.drawable.ic_downloads)
                       actionButton.contentDescription = activity.getString(R.string.download)
                   }
                   DownloadRepository.Status.Queued.toString() -> {
                       actionButton.setIconResource(R.drawable.ic_baseline_delete_outline_24)
                       actionButton.contentDescription = activity.getString(R.string.Remove)
                   }
                   else -> {
                       actionButton.setIconResource(R.drawable.ic_baseline_file_open_24)
                       actionButton.contentDescription = activity.getString(R.string.logs)
                       if (item.logID == null){
                           actionButton.visibility = View.GONE
                       }
                   }
               }

               actionButton.gone()


               if ((checkedItems.contains(item.id) && !inverted) || (!checkedItems.contains(item.id) && inverted)) {
                   downloadCardView.isChecked = true
                   downloadCardView.strokeWidth = 5
               } else {
                   downloadCardView.isChecked = false
                   downloadCardView.strokeWidth = 0
               }
               downloadCardView.setOnClickListener {
                   if (checkedItems.isNotEmpty() || inverted) {
                       checkCard(downloadCardView, item.id, position)
                   } else {
                       onItemClickListener.onCardClick(item.id)
                   }
               }

               downloadCardView.setOnLongClickListener {
                   true
//                   checkCard(downloadCardView, item.id, position)
               }
           }
       }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DownloadCardBinding.inflate(LayoutInflater.from(parent.context), parent,false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)


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
        fun onActionButtonClick(itemID: Long)
        fun onCardClick(itemID: Long)
        fun onCardSelect(isChecked: Boolean, position: Int)
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