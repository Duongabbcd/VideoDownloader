package com.ezt.video.downloader.ui.tab.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ezt.video.downloader.R
import com.ezt.video.downloader.databinding.ItemTabBinding
import com.ezt.video.downloader.ui.browse.BrowseActivity
import com.ezt.video.downloader.ui.home.MainActivity

class TabAdapter(private val onEditTabListener: OnEditTabListener) : RecyclerView.Adapter<TabAdapter.TabViewHolder>() {
    private val allTabs = mutableListOf<String>()
    private lateinit var context: Context

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TabViewHolder {
        context = parent.context
        val binding =
            ItemTabBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TabViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: TabViewHolder,
        position: Int
    ) {
        holder.bind(position)
    }

    fun submitList(input: List<String>) {
        allTabs.clear()
        allTabs.addAll(input)
        notifyDataSetChanged()
    }


    override fun getItemCount(): Int = allTabs.size

    inner class TabViewHolder(private val binding: ItemTabBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            val tab = allTabs[position]

            binding.apply {
                tabURL.text = tab

                closeBtn.setOnClickListener {
                    onEditTabListener.onDeleteTabListener(position)
                }

                if(tab.contains("Home", true)) {
                    root.setOnClickListener {
                        onEditTabListener.onEditTabListener(position)
                        onEditTabListener.onClickListener(tab)
                    }
                    tabWeb.text = "(${context.resources.getString(R.string.blank_page)})"
                } else {
                    root.setOnClickListener {
                        onEditTabListener.onEditTabListener(position)
                        onEditTabListener.onClickListener(tab)

                    }
                    if(tab.endsWith(".com", true)) {
                        tabWeb.text = context.resources.getString(R.string.blank_page)
                    } else {
                        tabWeb.text = tab
                    }

                }
            }
        }
    }
}

interface OnEditTabListener {
    fun onClickListener(tab: String)
    fun onDeleteTabListener(position: Int)
    fun onEditTabListener(position: Int)
}