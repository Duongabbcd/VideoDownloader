package com.ezt.video.downloader.ui.whatsapp.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ezt.video.downloader.database.models.expand.non_table.STATUS_TYPE
import com.ezt.video.downloader.database.models.expand.non_table.WhatsAppStatus
import com.ezt.video.downloader.databinding.ItemStatusBinding
import com.ezt.video.downloader.util.Common.gone
import com.ezt.video.downloader.util.Common.visible
import com.ezt.video.downloader.util.Utils
import com.ezt.video.downloader.util.Utils.formatDurationTime
import java.io.File

class StatusAdapter(private val onDownloadListener: (WhatsAppStatus) -> Unit) : RecyclerView.Adapter<StatusAdapter.StatusViewHolder>() {
    private lateinit var context: Context
    private val allWhatsAppStatuses = mutableListOf<WhatsAppStatus>()
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): StatusAdapter.StatusViewHolder {
       context = parent.context
        val binding = ItemStatusBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return StatusViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StatusViewHolder, position: Int) {
       holder.bind(position)
    }

    override fun getItemCount(): Int = allWhatsAppStatuses.size

    fun submitList(input: List<WhatsAppStatus>) {
        println("submitList: $input")
        allWhatsAppStatuses.clear()
        allWhatsAppStatuses.addAll(input)
        notifyDataSetChanged()
    }

    inner class StatusViewHolder(private val binding: ItemStatusBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            val statusData = allWhatsAppStatuses[position]
            println("StatusViewHolder-statusData: $statusData")
            val uri = Uri.parse(statusData.path)  // Convert the String to Uri
            val existence = doesContentUriExist(context, uri)
            println("StatusViewHolder: $existence")
            if(!existence) {
                return
            }

            Glide.with(context).load(uri).into(binding.imageView)


            if (statusData.type == STATUS_TYPE.VIDEO) {
                binding.duration.visible()
                binding.duration.text = formatDurationTime(statusData.duration)
            } else {
                binding.duration.gone()
            }

            binding.lastModified.text = Utils.getRelativeTime(statusData.lastModifiedTime)

            binding.fabDownload.setOnClickListener {
                onDownloadListener(statusData)
            }
        }

        fun doesContentUriExist(context: Context, uriString: Uri): Boolean {
            return try {
                context.contentResolver.openInputStream(uriString)?.close()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    }
}