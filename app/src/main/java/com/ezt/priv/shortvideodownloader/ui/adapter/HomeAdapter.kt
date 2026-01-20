package com.ezt.priv.shortvideodownloader.ui.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.database.models.main.ResultItem
import com.ezt.priv.shortvideodownloader.database.viewmodel.DownloadViewModel
import com.ezt.priv.shortvideodownloader.databinding.ResultCardBinding
import com.ezt.priv.shortvideodownloader.util.Extensions.loadThumbnail
import com.ezt.priv.shortvideodownloader.util.Extensions.popup
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator


class HomeAdapter(onItemClickListener: OnItemClickListener, activity: Activity) : ListAdapter<ResultItem?, HomeAdapter.ViewHolder>(AsyncDifferConfig.Builder(
    DIFF_CALLBACK
).build()) {
    private val checkedItems: ArrayList<String>
    private val onItemClickListener: OnItemClickListener
    private val activity: Activity
    private val sharedPreferences: SharedPreferences

    init {
        checkedItems = ArrayList()
        this.onItemClickListener = onItemClickListener
        this.activity = activity
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
    }

    inner class ViewHolder(private val binding: ResultCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(video: ResultItem?) {
            binding.apply {
                resultCardView.popup()

                val uiHandler = Handler(Looper.getMainLooper())

                // THUMBNAIL ----------------------------------
                val hideThumb = sharedPreferences.getStringSet("hide_thumbnails", emptySet())!!.contains("home")
                uiHandler.post { resultImageView.loadThumbnail(hideThumb, video!!.thumb) }

                // TITLE  ----------------------------------
                var title = video!!.title.ifBlank { video.url }
                if (title.length > 100) {
                    title = title.substring(0, 40) + "..."
                }
                resultTitle.text = title

                // Bottom Info ----------------------------------
                author.text = video.author
                if (video.duration.isNotEmpty() && video.duration != "-1") {
                    duration.text = video.duration
                }

                // BUTTONS ----------------------------------
                val videoURL = video.url
                downloadMusic.tag = "$videoURL##audio"
                downloadMusic.setTag(R.id.cancelDownload, "false")
                downloadMusic.setOnClickListener { onItemClickListener.onButtonClick(videoURL, DownloadViewModel.Type.audio) }
                downloadMusic.setOnLongClickListener{ onItemClickListener.onLongButtonClick(videoURL, DownloadViewModel.Type.audio); true}
                downloadVideo.tag = "$videoURL##video"
                downloadVideo.setTag(R.id.cancelDownload, "false")
                downloadVideo.setOnClickListener { onItemClickListener.onButtonClick(videoURL, DownloadViewModel.Type.video) }
                downloadVideo.setOnLongClickListener{ onItemClickListener.onLongButtonClick(videoURL, DownloadViewModel.Type.video); true}


                // PROGRESS BAR ----------------------------------------------------
                downloadProgress.tag = "$videoURL##progress"
                downloadProgress.progress = 0
                downloadProgress.isIndeterminate = true
                downloadProgress.visibility = View.GONE

//        if (video.isDownloading()){
//            progressBar.setVisibility(View.VISIBLE);
//        }else {
//            progressBar.setProgress(0);
//            progressBar.setIndeterminate(true);
//            progressBar.setVisibility(View.GONE);
//        }
//
//        if (video.isDownloadingAudio()) {
//            musicBtn.setIcon(ContextCompat.getDrawable(activity, R.drawable.ic_cancel));
//            musicBtn.setTag(R.id.cancelDownload, "true");
//        }else{
//            if(video.isAudioDownloaded() == 1){
//                musicBtn.setIcon(ContextCompat.getDrawable(activity, R.drawable.ic_music_downloaded));
//            }else{
//                musicBtn.setIcon(ContextCompat.getDrawable(activity, R.drawable.ic_music));
//            }
//        }
//
//        if (video.isDownloadingVideo()){
//            videoBtn.setIcon(ContextCompat.getDrawable(activity, R.drawable.ic_cancel));
//            videoBtn.setTag(R.id.cancelDownload, "true");
//        }else{
//            if(video.isVideoDownloaded() == 1){
//                videoBtn.setIcon(ContextCompat.getDrawable(activity, R.drawable.ic_video_downloaded));
//            }else{
//                videoBtn.setIcon(ContextCompat.getDrawable(activity, R.drawable.ic_video));
//            }
//        }
                if (checkedItems.contains(videoURL)) {
                    resultCardView.isChecked = true
                    resultCardView.strokeWidth = 5
                } else {
                    resultCardView.isChecked = false
                    resultCardView.strokeWidth = 0
                }
                resultCardView.tag = "$videoURL##card"
                resultCardView.setOnLongClickListener {
                    checkCard(resultCardView, videoURL)
                    true
                }
                resultCardView.setOnClickListener {
                    if (checkedItems.size > 0) {
                        checkCard(resultCardView, videoURL)
                    }else{
                        onItemClickListener.onCardDetailsClick(videoURL)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ResultCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val video = getItem(position)
        holder.bind(video)
    }

    private fun checkCard(card: MaterialCardView, videoURL: String) {
        if (card.isChecked) {
            card.strokeWidth = 0
            checkedItems.remove(videoURL)
        } else {
            card.strokeWidth = 5
            checkedItems.add(videoURL)
        }
        card.isChecked = !card.isChecked
        onItemClickListener.onCardClick(videoURL, card.isChecked)
    }

    interface OnItemClickListener {
        fun onButtonClick(videoURL: String, type: DownloadViewModel.Type?)
        fun onLongButtonClick(videoURL: String, type: DownloadViewModel.Type?)
        fun onCardClick(videoURL: String, add: Boolean)
        fun onCardDetailsClick(videoURL: String)
    }

    fun checkAll(items: List<ResultItem?>?){
        checkedItems.clear()
        checkedItems.addAll(items!!.map { it!!.url })
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun checkMultipleItems(list: List<String>){
        checkedItems.clear()
        checkedItems.addAll(list)
        notifyDataSetChanged()
    }

    fun invertSelected(items: List<ResultItem?>?){
        val invertedList = mutableListOf<String>()
        items?.forEach {
            if (!checkedItems.contains(it!!.url)) invertedList.add(it.url)
        }
        checkedItems.clear()
        checkedItems.addAll(invertedList)
        notifyDataSetChanged()
    }

    fun clearCheckedItems(){
        checkedItems.clear()
    }

    companion object {
        private val DIFF_CALLBACK: DiffUtil.ItemCallback<ResultItem> = object : DiffUtil.ItemCallback<ResultItem>() {
            override fun areItemsTheSame(oldItem: ResultItem, newItem: ResultItem): Boolean {
                val ranged = arrayListOf(oldItem.id, newItem.id)
                return ranged[0] == ranged[1]
            }

            override fun areContentsTheSame(oldItem: ResultItem, newItem: ResultItem): Boolean {
                return oldItem.url == newItem.url && oldItem.title == newItem.title && oldItem.author == newItem.author
            }
        }
    }
}