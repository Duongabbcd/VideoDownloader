package com.ezt.priv.shortvideodownloader.ui.adapter

import android.app.Activity
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.view.get
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.database.models.main.DownloadItem
import com.ezt.priv.shortvideodownloader.database.repository.DownloadRepository
import com.ezt.priv.shortvideodownloader.database.viewmodel.DownloadViewModel
import com.ezt.priv.shortvideodownloader.databinding.ActiveDownloadCardMinifiedBinding
import com.ezt.priv.shortvideodownloader.util.Extensions.loadThumbnail
import com.ezt.priv.shortvideodownloader.util.Extensions.popup
import com.ezt.priv.shortvideodownloader.util.FileUtil
import com.google.android.material.progressindicator.LinearProgressIndicator

class ActiveDownloadMinifiedAdapter(onItemClickListener: OnItemClickListener, activity: Activity) : ListAdapter<DownloadItem?, ActiveDownloadMinifiedAdapter.ViewHolder>(AsyncDifferConfig.Builder(
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

    inner class ViewHolder(private val binding: ActiveDownloadCardMinifiedBinding, private val activity: Activity,
                     private val sharedPreferences: SharedPreferences, private val onItemClickListener: OnItemClickListener) : RecyclerView.ViewHolder(binding.root) {
      fun bind(item: DownloadItem?) {
          binding.apply {
              downloadCardView.popup()

              val uiHandler = Handler(Looper.getMainLooper())

              // THUMBNAIL ----------------------------------
              val hideThumb = sharedPreferences.getStringSet("hide_thumbnails", emptySet())!!.contains("queue")
              uiHandler.post { downloadsImageView.loadThumbnail(hideThumb, item!!.thumb) }

              // PROGRESS BAR ----------------------------------------------------
              progress.tag = "${item!!.id}##progress"
              progress.progress = 0
              progress.isIndeterminate = true

              //OUTPUT ------------------------------------------------------------
              output.tag = "${item.id}##output"

              // TITLE  ----------------------------------
              var titleStr = item.title.ifEmpty { item.playlistTitle.ifEmpty { item.url } }
              if (titleStr.length > 100) {
                  titleStr = titleStr.substring(0, 40) + "..."
              }
              title.text = titleStr

              //DOWNLOAD TYPE -----------------------------
              when(item.type){
                  DownloadViewModel.Type.audio -> downloadType.setCompoundDrawablesRelativeWithIntrinsicBounds(
                      R.drawable.ic_music_formatcard, 0, 0, 0
                  )
                  DownloadViewModel.Type.video -> downloadType.setCompoundDrawablesRelativeWithIntrinsicBounds(
                      R.drawable.ic_video_formatcard, 0, 0, 0
                  )
                  else -> downloadType.setCompoundDrawablesRelativeWithIntrinsicBounds(
                      R.drawable.ic_terminal_formatcard, 0, 0, 0
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
                      item.format.acodec?.uppercase()
                  }
              if (codecText == "" || codecText == "none"){
                  codec.visibility = View.GONE
              }else{
                  codec.visibility = View.VISIBLE
                  codec.text = codecText
              }
              val fileSizeReadable = FileUtil.convertFileSize(item.format.filesize)
              if (fileSizeReadable == "?" && item.downloadSections.isNotBlank()) fileSize.visibility = View.GONE
              else fileSize.text = fileSizeReadable

              val paused = item.status == DownloadRepository.Status.Paused.toString()
              options.setOnClickListener {
                  val popup = PopupMenu(activity, it)
                  popup.menuInflater.inflate(R.menu.active_downloads_minified, popup.menu)
                  if (Build.VERSION.SDK_INT > 27) popup.menu.setGroupDividerEnabled(true)

                  val pause = popup.menu[0]
                  val resume = popup.menu[1]

                  if (paused){
                      pause.isVisible = false
                      resume.isVisible = true
                  }else{
                      pause.isVisible = true
                      resume.isVisible = false
                  }

                  popup.setOnMenuItemClickListener { m ->
                      when(m.itemId){
                          R.id.pause -> {
                              onItemClickListener.onPauseClick(item.id, position)
                              if (binding.progress.progress == 0) binding.progress.isIndeterminate = false
                              popup.dismiss()
                          }
                          R.id.resume -> {
                              onItemClickListener.onResumeClick(item.id, position)
                              binding.progress.isIndeterminate = true
                              popup.dismiss()
                          }
                          R.id.cancel -> {
                              onItemClickListener.onCancelClick(item.id)
                              popup.dismiss()
                          }
                      }
                      true
                  }

                  popup.show()

              }

              binding.progress.isIndeterminate = !paused

              downloadCardView.setOnClickListener {
                  onItemClickListener.onCardClick()
              }

          }

      }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ActiveDownloadCardMinifiedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, activity, sharedPreferences,onItemClickListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }
    interface OnItemClickListener {
        fun onCancelClick(itemID: Long)
        fun onPauseClick(itemID: Long, position: Int)
        fun onResumeClick(itemID: Long, position: Int)
        fun onCardClick()
    }


    companion object {
        private val DIFF_CALLBACK: DiffUtil.ItemCallback<DownloadItem> = object : DiffUtil.ItemCallback<DownloadItem>() {
            override fun areItemsTheSame(oldItem: DownloadItem, newItem: DownloadItem): Boolean {
                val ranged = arrayListOf(oldItem.id, newItem.id)
                return ranged[0] == ranged[1]
            }

            override fun areContentsTheSame(oldItem: DownloadItem, newItem: DownloadItem): Boolean {
                return oldItem.id == newItem.id && oldItem.title == newItem.title && oldItem.author == newItem.author && oldItem.thumb == newItem.thumb && oldItem.status == newItem.status
            }
        }
    }
}