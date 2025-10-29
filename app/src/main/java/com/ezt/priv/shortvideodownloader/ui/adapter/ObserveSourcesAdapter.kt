package com.ezt.priv.shortvideodownloader.ui.adapter

import android.app.Activity
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.database.models.observeSources.ObserveSourcesItem
import com.ezt.priv.shortvideodownloader.database.repository.ObserveSourcesRepository
import com.ezt.priv.shortvideodownloader.databinding.ObserveSourcesItemBinding
import com.ezt.priv.shortvideodownloader.util.Extensions.calculateNextTimeForObserving
import com.ezt.priv.shortvideodownloader.util.Extensions.popup
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class ObserveSourcesAdapter(onItemClickListener: OnItemClickListener, activity: Activity) : ListAdapter<ObserveSourcesItem?, ObserveSourcesAdapter.ViewHolder>(AsyncDifferConfig.Builder(
    DIFF_CALLBACK
).build()) {
    private val onItemClickListener: OnItemClickListener
    private val activity: Activity

    init {
        this.onItemClickListener = onItemClickListener
        this.activity = activity
    }

    inner class ViewHolder(private val binding: ObserveSourcesItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ObserveSourcesItem?) {
            binding.apply {
                observeSourcesCard.popup()

                if (item == null) return
                observeSourcesCard.tag = item.url
                itemView.tag = item.url


                // TITLE  ----------------------------------
                var titleStr = item.name
                if (titleStr.length > 100) {
                    titleStr = titleStr.substring(0, 40) + "..."
                }
                title.text = titleStr

                //URL
                url.text = item.url

                //INFO
                val nextTime = item.calculateNextTimeForObserving()
                val c = Calendar.getInstance()
                c.timeInMillis = nextTime

                val weekdays = DateFormatSymbols(Locale.getDefault()).shortWeekdays
                val text = "${weekdays[c.get(Calendar.DAY_OF_WEEK)]}, ${SimpleDateFormat(DateFormat.getBestDateTimePattern(Locale.getDefault(), "ddMMMyyyy - HHmm"), Locale.getDefault()).format(c.timeInMillis)}"
                info.text = text

                //CHECK MISSING
                checkMissing.isVisible = item.retryMissingDownloads

                downloadProgress.isIndeterminate = true
                downloadProgress.isVisible = false

                // BUTTON ----------------------------------
                search.isEnabled = true
                pauseResume.isEnabled = true
                if (item.status == ObserveSourcesRepository.SourceStatus.STOPPED){
                    info.isVisible = false
                    search.isVisible = false

                    pauseResume.setIconResource(R.drawable.exomedia_ic_play_arrow_white)
                    pauseResume.contentDescription = activity.getString(R.string.resume)
                    pauseResume.setOnClickListener {
                        pauseResume.isEnabled = false
                        onItemClickListener.onItemStart(item, position)
                    }
                }else{
                    info.isVisible = true
                    search.isVisible = true

                    search.setOnClickListener {
                        search.isEnabled = false
                        downloadProgress.isVisible = true
                        downloadProgress.animate()
                        onItemClickListener.onItemSearch(item)
                    }

                    pauseResume.setIconResource(R.drawable.exomedia_ic_pause_white)
                    pauseResume.contentDescription = activity.getString(R.string.pause)
                    pauseResume.setOnClickListener {
                        pauseResume.isEnabled = false
                        onItemClickListener.onItemPaused(item, position)
                    }
                }


                observeSourcesCard.setOnClickListener {
                    onItemClickListener.onItemClick(item)
                }

                observeSourcesCard.setOnLongClickListener {
                    onItemClickListener.onDelete(item); true
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ObserveSourcesItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    interface OnItemClickListener {

        fun onItemSearch(item: ObserveSourcesItem)
        fun onItemStart(item: ObserveSourcesItem, position: Int)
        fun onItemPaused(item: ObserveSourcesItem, position: Int)
        fun onItemClick(item: ObserveSourcesItem)
        fun onDelete(item: ObserveSourcesItem)
    }

    companion object {
        private val DIFF_CALLBACK: DiffUtil.ItemCallback<ObserveSourcesItem> = object : DiffUtil.ItemCallback<ObserveSourcesItem>() {
            override fun areItemsTheSame(oldItem: ObserveSourcesItem, newItem: ObserveSourcesItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ObserveSourcesItem, newItem: ObserveSourcesItem): Boolean {
                return oldItem.id == newItem.id
                        && oldItem.name == newItem.name
                        && oldItem.downloadItemTemplate == newItem.downloadItemTemplate
                        && oldItem.status == newItem.status
                        && oldItem.retryMissingDownloads == newItem.retryMissingDownloads
                        && oldItem.runCount == newItem.runCount
                        && oldItem.calculateNextTimeForObserving() == newItem.calculateNextTimeForObserving()
            }
        }
    }
}