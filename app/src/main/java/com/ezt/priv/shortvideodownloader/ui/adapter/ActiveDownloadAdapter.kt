package com.ezt.priv.shortvideodownloader.ui.adapter

import android.animation.ValueAnimator
import android.app.Activity
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.database.models.main.DownloadItem
import com.ezt.priv.shortvideodownloader.database.repository.DownloadRepository
import com.ezt.priv.shortvideodownloader.database.viewmodel.DownloadViewModel
import com.ezt.priv.shortvideodownloader.databinding.ActiveDownloadCardBinding
import com.ezt.priv.shortvideodownloader.util.Extensions.loadBlurryThumbnail
import com.ezt.priv.shortvideodownloader.util.FileUtil
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.progressindicator.LinearProgressIndicator


class ActiveDownloadAdapter(onItemClickListener: OnItemClickListener, activity: Activity) : ListAdapter<DownloadItem?, ActiveDownloadAdapter.ViewHolder>(AsyncDifferConfig.Builder(
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

    inner class ViewHolder(private val binding: ActiveDownloadCardBinding,
                     private val activity: Activity,
                     private val sharedPreferences: SharedPreferences,
                     private val onItemClickListener: OnItemClickListener) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DownloadItem?) {
            binding.apply {
                activeDownloadCardView.tag = "${item!!.id}##card"
                val uiHandler = Handler(Looper.getMainLooper())

                // THUMBNAIL ----------------------------------
                val hideThumb = sharedPreferences.getStringSet("hide_thumbnails", emptySet())!!.contains("queue")
                uiHandler.post { imageView.loadBlurryThumbnail(activity, hideThumb, item.thumb) }

                // PROGRESS BAR ----------------------------------------------------
                progress.tag = "${item.id}##progress"

                // TITLE  ----------------------------------
                var titleStr = item.title.ifEmpty { item.playlistTitle.ifEmpty { item.url } }
                if (titleStr.length > 100) {
                    titleStr = titleStr.substring(0, 40) + "..."
                }
                title.text = titleStr

                // Author ----------------------------------
                var info = item.author
                if (item.duration.isNotEmpty() && item.duration != "-1") {
                    if (item.author.isNotEmpty()) info += " • "
                    info += item.duration
                }
                author.text = info

                when(item.type){
                    DownloadViewModel.Type.audio -> downloadType.setIconResource(R.drawable.ic_music)
                    DownloadViewModel.Type.video -> downloadType.setIconResource(R.drawable.ic_video)
                    DownloadViewModel.Type.command -> downloadType.setIconResource(R.drawable.ic_terminal)
                    else -> {}
                }

                val sideDetails = mutableListOf<String>()
                sideDetails.add(item.format.format_note.uppercase().replace("\n", " "))
                sideDetails.add(item.container.uppercase().ifEmpty { item.format.container.uppercase() })

                val fileSize = FileUtil.convertFileSize(item.format.filesize)
                if (fileSize != "?" && item.downloadSections.isBlank()) sideDetails.add(fileSize)
                formatNote.text = sideDetails.filter { it.isNotBlank() }.joinToString("  ·  ")

                //OUTPUT

                output.tag = "${item.id}##output"
                output.text = ""

                output.setOnClickListener {
                    onItemClickListener.onOutputClick(item)
                }

                // CANCEL BUTTON ----------------------------------
                if (activeDownloadDelete.hasOnClickListeners()) activeDownloadDelete.setOnClickListener(null)
                activeDownloadDelete.setOnClickListener {onItemClickListener.onCancelClick(item.id)}

                activeDownloadResume.isEnabled = true
                if (activeDownloadResume.hasOnClickListeners()) activeDownloadResume.setOnClickListener(null)
                val isPaused = item.status == DownloadRepository.Status.Paused.toString()
                if (isPaused) {
                    activeDownloadResume.setIconResource(R.drawable.exomedia_ic_play_arrow_white)
                    activeDownloadResume.setOnClickListener {
                        activeDownloadResume.isEnabled = false
                        onItemClickListener.onResumeClick(item.id)
                    }
                }else {
                    activeDownloadResume.setIconResource(R.drawable.exomedia_ic_pause_white)
                    activeDownloadResume.setOnClickListener {
                        activeDownloadResume.isEnabled = false
                        onItemClickListener.onPauseClick(item.id)
                    }
                }

                if (isPaused) {
                    progress.isIndeterminate = false
                    progress.progress = 0
                    activeDownloadDelete.isEnabled = true
                    output.text = activity.getString(R.string.exo_download_paused)
                }else{
                    progress.isIndeterminate = progress.progress <= 0
                    activeDownloadDelete.isEnabled = true
                }
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ActiveDownloadCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, activity, sharedPreferences, onItemClickListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    interface OnItemClickListener {
        fun onCancelClick(itemID: Long)
        fun onOutputClick(item: DownloadItem)
        fun onPauseClick(itemID: Long)
        fun onResumeClick(itemID: Long)
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