package com.ezt.priv.shortvideodownloader.ui.adapter

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.database.models.main.CookieItem
import com.ezt.priv.shortvideodownloader.databinding.CookieItemBinding
import com.ezt.priv.shortvideodownloader.databinding.DownloadCardBinding
import com.ezt.priv.shortvideodownloader.util.Extensions.popup

class CookieAdapter(onItemClickListener: OnItemClickListener, activity: Activity) : ListAdapter<CookieItem?, CookieAdapter.ViewHolder>(AsyncDifferConfig.Builder(
    DIFF_CALLBACK
).build()) {
    private val onItemClickListener: OnItemClickListener
    private val activity: Activity

    init {
        this.onItemClickListener = onItemClickListener
        this.activity = activity
    }

    inner class ViewHolder(private val binding: CookieItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CookieItem?) {
            binding.apply {
                if (item == null) return
                cookieCard.popup()

                title.text = item.description.ifBlank { item.url }
                content.text = item.content

                cookieEnabled.isChecked = item.enabled

                cookieCard.setOnClickListener {
                    onItemClickListener.onItemClick(item, position)
                }

                cookieCard.setOnLongClickListener {
                    onItemClickListener.onDelete(item); true
                }

                cookieEnabled.setOnCheckedChangeListener { _, isEnabled ->
                    onItemClickListener.onItemEnabledChanged(item, isEnabled)
                    true
                }
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = CookieItemBinding.inflate(LayoutInflater.from(parent.context), parent,false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    interface OnItemClickListener {
        fun onItemClick(cookieItem: CookieItem, position: Int)
        fun onSelected(cookieItem: CookieItem)
        fun onDelete(cookieItem: CookieItem)
        fun onItemEnabledChanged(cookieItem: CookieItem, isEnabled: Boolean)
    }

    companion object {
        private val DIFF_CALLBACK: DiffUtil.ItemCallback<CookieItem> = object : DiffUtil.ItemCallback<CookieItem>() {
            override fun areItemsTheSame(oldItem: CookieItem, newItem: CookieItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: CookieItem, newItem: CookieItem): Boolean {
                return oldItem.id == newItem.id && oldItem.description == newItem.description
            }
        }
    }
}