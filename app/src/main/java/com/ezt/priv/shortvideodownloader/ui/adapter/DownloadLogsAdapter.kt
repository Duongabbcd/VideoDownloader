package com.ezt.priv.shortvideodownloader.ui.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.database.models.main.LogItem
import com.ezt.priv.shortvideodownloader.database.viewmodel.DownloadViewModel
import com.ezt.priv.shortvideodownloader.databinding.LogCardBinding
import com.ezt.priv.shortvideodownloader.util.Extensions.popup
import com.ezt.priv.shortvideodownloader.util.FileUtil
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Locale

class DownloadLogsAdapter(onItemClickListener: OnItemClickListener, activity: Activity) : ListAdapter<LogItem?, DownloadLogsAdapter.ViewHolder>(AsyncDifferConfig.Builder(
    DIFF_CALLBACK
).build()) {
    private val onItemClickListener: OnItemClickListener
    private val activity: Activity
    private val checkedItems: ArrayList<Long>

    init {
        checkedItems = ArrayList()
        this.onItemClickListener = onItemClickListener
        this.activity = activity
    }

    inner class ViewHolder(private val binding:LogCardBinding ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: LogItem?) {
            binding.apply {
                logCardView.popup()

                title.text = item?.title

                downloadedTime.text = SimpleDateFormat(
                    DateFormat.getBestDateTimePattern(
                        Locale.getDefault(), "ddMMMyyyy - HH:mm"), Locale.getDefault()).format(item!!.downloadTime)

                when(item.downloadType){
                    DownloadViewModel.Type.audio -> {
                        downloadType.setIconResource(R.drawable.ic_music)
                        downloadType.contentDescription = activity.getString(R.string.audio)
                    }
                    DownloadViewModel.Type.video -> {
                        downloadType.setIconResource(R.drawable.ic_video)
                        downloadType.contentDescription = activity.getString(R.string.video)
                    }
                    DownloadViewModel.Type.command -> {
                        downloadType.setIconResource(R.drawable.ic_terminal)
                        downloadType.contentDescription = activity.getString(R.string.command)
                    }
                    else -> {}
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

                if (checkedItems.contains(item.id)) {
                    logCardView.isChecked = true
                    logCardView.strokeWidth = 5
                } else {
                    logCardView.isChecked = false
                    logCardView.strokeWidth = 0
                }
                logCardView.setOnClickListener {
                    if (checkedItems.size > 0) {
                        checkCard(logCardView, item.id)
                    } else {
                        onItemClickListener.onItemClick(item.id)
                    }
                }

                logCardView.setOnLongClickListener {
                    checkCard(logCardView, item.id)
                    true
                }
            }
        }


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = LogCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearCheckeditems() {
        for (i in 0 until itemCount){
            val item = getItem(i)
            if (checkedItems.find { it == item?.id } != null){
                checkedItems.remove(item?.id)
                notifyItemChanged(i)
            }
        }

        checkedItems.clear()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun checkAll(items: List<LogItem?>?){
        checkedItems.clear()
        checkedItems.addAll(items!!.map { it!!.id })
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun invertSelected(items: List<LogItem?>?){
        val invertedList = mutableListOf<Long>()
        items?.forEach {
            if (!checkedItems.contains(it!!.id)) invertedList.add(it.id)
        }
        checkedItems.clear()
        checkedItems.addAll(invertedList)
        notifyDataSetChanged()
    }

    private fun checkCard(card: MaterialCardView, itemID: Long) {
        if (card.isChecked) {
            card.strokeWidth = 0
            checkedItems.remove(itemID)
        } else {
            card.strokeWidth = 5
            checkedItems.add(itemID)
        }
        card.isChecked = !card.isChecked
        onItemClickListener.onCardSelect(itemID, card.isChecked)
    }

    interface OnItemClickListener {
        fun onItemClick(itemID: Long)
        fun onDeleteClick(item: LogItem)
        fun onCardSelect(itemID: Long, isChecked: Boolean)
    }

    companion object {
        private val DIFF_CALLBACK: DiffUtil.ItemCallback<LogItem> = object : DiffUtil.ItemCallback<LogItem>() {
            override fun areItemsTheSame(oldItem: LogItem, newItem: LogItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: LogItem, newItem: LogItem): Boolean {
                return oldItem.content == newItem.content
            }
        }
    }
}