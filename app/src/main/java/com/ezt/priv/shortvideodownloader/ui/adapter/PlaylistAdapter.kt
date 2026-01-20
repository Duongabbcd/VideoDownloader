package com.ezt.priv.shortvideodownloader.ui.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.database.models.main.ResultItem
import com.ezt.priv.shortvideodownloader.databinding.PlaylistItemBinding
import com.ezt.priv.shortvideodownloader.util.Extensions.loadThumbnail
import com.ezt.priv.shortvideodownloader.util.Extensions.popup
import com.google.android.material.card.MaterialCardView
import java.util.*


class PlaylistAdapter(onItemClickListener: OnItemClickListener, activity: Activity) : ListAdapter<ResultItem?, PlaylistAdapter.ViewHolder>(AsyncDifferConfig.Builder(
    DIFF_CALLBACK
).build()) {
    private val checkedItems: ArrayList<Long>
    private val onItemClickListener: OnItemClickListener
    private val activity: Activity
    private val sharedPreferences: SharedPreferences

    init {
        checkedItems = ArrayList()
        this.onItemClickListener = onItemClickListener
        this.activity = activity
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
    }

    inner class ViewHolder(private val binding: PlaylistItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ResultItem?) {
            binding.apply {
                playlistCard.popup()
                val uiHandler = Handler(Looper.getMainLooper())

                // THUMBNAIL ----------------------------------
                val hideThumb = sharedPreferences.getStringSet("hide_thumbnails", emptySet())!!.contains("home")
                uiHandler.post { downloadsImageView.loadThumbnail(hideThumb, item!!.thumb) }

                title.text = item!!.title
                author.text = item.author
                duration.text = item.duration
                index.text = ((item.playlistIndex ?: (position + 1))).toString()

                // CHECKBOX ----------------------------------

                checkBox.isChecked = checkedItems.contains(item.id)
                checkBox.setOnClickListener {
                    checkCard(checkBox.isChecked, item.id)
                }

                playlistCard.setOnClickListener {
                    checkBox.performClick()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = PlaylistItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    private fun checkCard(isChecked: Boolean, id: Long) {
        if (isChecked) {
            checkedItems.add(id)
        } else {
            checkedItems.remove(id)
        }
        onItemClickListener.onCardSelect(id, isChecked, checkedItems)
    }

    interface OnItemClickListener {
        fun onCardSelect(itemID: Long, isChecked: Boolean, checkedItems: List<Long>)
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

    fun checkAll(){
        checkedItems.clear()
        for (i in 0 until itemCount){
            val item = getItem(i)
            checkedItems.add(item!!.id)
            notifyItemChanged(i)
        }
    }

    fun invertSelected(items: List<ResultItem?>?){
        val invertedList = mutableListOf<Long>()
        items?.forEach {
            if (!checkedItems.contains(it!!.id)) invertedList.add(it.id)
        }
        checkedItems.clear()
        checkedItems.addAll(invertedList)
        notifyDataSetChanged()
    }

    fun checkRange(start: Int, end: Int){
        checkedItems.clear()
        if (start == end ){
            val item = getItem(start)
            checkedItems.add(item!!.id)
            notifyItemChanged(start)
        }else{
            for (i in start..end){
                val item = getItem(i)
                checkedItems.add(item!!.id)
                notifyItemChanged(i)
            }
        }

    }

    fun getCheckedItems() : List<Long>{
        return checkedItems
    }

    companion object {
        private val DIFF_CALLBACK: DiffUtil.ItemCallback<ResultItem> = object : DiffUtil.ItemCallback<ResultItem>() {
            override fun areItemsTheSame(oldItem: ResultItem, newItem: ResultItem): Boolean {
                val ranged = arrayListOf(oldItem.id, newItem.id)
                return ranged[0] == ranged[1]
            }

            override fun areContentsTheSame(oldItem: ResultItem, newItem: ResultItem): Boolean {
                return oldItem.id == newItem.id
            }
        }
    }
}