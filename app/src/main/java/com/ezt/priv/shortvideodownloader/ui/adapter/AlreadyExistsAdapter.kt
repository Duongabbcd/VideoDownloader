package com.ezt.priv.shortvideodownloader.ui.adapter

import android.app.Activity
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.database.models.expand.non_table.AlreadyExistsItem
import com.ezt.priv.shortvideodownloader.database.viewmodel.DownloadViewModel
import com.ezt.priv.shortvideodownloader.databinding.AlreadyExistsItemBinding
import com.ezt.priv.shortvideodownloader.ui.adapter.ActiveDownloadMinifiedAdapter.ViewHolder
import com.ezt.priv.shortvideodownloader.util.Extensions.loadThumbnail
import com.ezt.priv.shortvideodownloader.util.FileUtil
import com.ezt.priv.shortvideodownloader.util.UiUtil
import com.google.android.material.card.MaterialCardView
import kotlin.text.ifEmpty

class AlreadyExistsAdapter(onItemClickListener: OnItemClickListener, activity: Activity) : ListAdapter<AlreadyExistsItem, AlreadyExistsAdapter.ViewHolder>(AsyncDifferConfig.Builder(
    DIFF_CALLBACK
).build()) {
    private val onItemClickListener: OnItemClickListener
    private val activity: Activity
    private val sharedPreferences: SharedPreferences

    init {
        this.onItemClickListener = onItemClickListener
        this.activity = activity
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
    }

     inner class ViewHolder(private val binding: AlreadyExistsItemBinding, private val activity: Activity, private val sharedPreferences: SharedPreferences,
        private val onItemClickListener: OnItemClickListener) : RecyclerView.ViewHolder(binding.root) {
      fun bind(alreadyExistsItem: AlreadyExistsItem) {
          binding.apply {
              downloadCardView.tag = alreadyExistsItem.downloadItem.id.toString()
              val item = alreadyExistsItem.downloadItem

              val uiHandler = Handler(Looper.getMainLooper())

              // THUMBNAIL ----------------------------------
              val hideThumb = sharedPreferences.getStringSet("hide_thumbnails", emptySet())!!.contains("queue")
              uiHandler.post { downloadsImageView.loadThumbnail(hideThumb, item.thumb) }

              duration.text = item.duration

              // TITLE  ----------------------------------
              var titleStr = item.title
              if (titleStr.length > 100) {
                  titleStr = titleStr.substring(0, 40) + "..."
              }
              title.text = titleStr.ifEmpty { item.url }

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

              options.isVisible = true
              options.setOnClickListener {
                  val popup = PopupMenu(activity, it)
                  popup.menuInflater.inflate(R.menu.already_exists_menu, popup.menu)
                  if (Build.VERSION.SDK_INT > 27) popup.menu.setGroupDividerEnabled(true)

                  popup.setOnMenuItemClickListener { m ->
                      when(m.itemId){
                          R.id.edit -> {
                              onItemClickListener.onEditItem(alreadyExistsItem, position)
                              popup.dismiss()
                          }
                          R.id.delete -> {
                              onItemClickListener.onDeleteItem(alreadyExistsItem, position)
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

              downloadCardView.setOnLongClickListener {
                  onItemClickListener.onDeleteItem(alreadyExistsItem, position)
                  true
              }

              if (alreadyExistsItem.historyID != null){
                  downloadCardView.setOnClickListener {
                      onItemClickListener.onShowHistoryItem(alreadyExistsItem.historyID!!)
                  }
              }else{
                  downloadCardView.setOnClickListener(null)
              }
          }

      }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AlreadyExistsItemBinding.inflate(LayoutInflater.from(parent.context), parent,false)
        return ViewHolder(binding, activity, sharedPreferences,onItemClickListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val alreadyExistsItem = getItem(position)
        holder.bind(alreadyExistsItem)

    }

    interface OnItemClickListener {
        fun onEditItem(alreadyExistsItem: AlreadyExistsItem, position: Int)
        fun onDeleteItem(alreadyExistsItem: AlreadyExistsItem, position: Int)
        fun onShowHistoryItem(historyItemID: Long)
    }

    companion object {
        private val DIFF_CALLBACK: DiffUtil.ItemCallback<AlreadyExistsItem> = object : DiffUtil.ItemCallback<AlreadyExistsItem>() {
            override fun areItemsTheSame(oldItem: AlreadyExistsItem, newItem: AlreadyExistsItem): Boolean {
                return oldItem.downloadItem.id == newItem.downloadItem.id
            }

            override fun areContentsTheSame(oldItem: AlreadyExistsItem, newItem: AlreadyExistsItem): Boolean {
                return oldItem.downloadItem.id == newItem.downloadItem.id && oldItem.downloadItem.title == newItem.downloadItem.title && oldItem.downloadItem.author == newItem.downloadItem.author && oldItem.downloadItem.thumb == newItem.downloadItem.thumb
            }
        }
    }
}