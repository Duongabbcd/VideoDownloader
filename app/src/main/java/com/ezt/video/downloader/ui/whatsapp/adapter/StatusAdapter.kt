package com.ezt.video.downloader.ui.whatsapp.adapter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ezt.video.downloader.R
import com.ezt.video.downloader.database.models.expand.non_table.STATUS_TYPE
import com.ezt.video.downloader.database.models.expand.non_table.WhatsAppStatus
import com.ezt.video.downloader.databinding.ItemStatusBinding
import com.ezt.video.downloader.ui.player.PlayerActivity
import com.ezt.video.downloader.util.Common.gone
import com.ezt.video.downloader.util.Common.visible
import com.ezt.video.downloader.util.Utils
import com.ezt.video.downloader.util.Utils.formatDurationTime
import java.io.File

class StatusAdapter(private val onEditWhatsAppListener: OnEditWhatsAppListener ) : RecyclerView.Adapter<StatusAdapter.StatusViewHolder>() {
    private lateinit var context: Context
    private var isLocalSaved = false
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

    fun submitList(input: List<WhatsAppStatus>, isSaved: Boolean = false) {
        println("submitList: $input")
        isLocalSaved = isSaved
        allWhatsAppStatuses.clear()
        allWhatsAppStatuses.addAll(input)
        notifyDataSetChanged()
    }

    inner class StatusViewHolder(private val binding: ItemStatusBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            val statusData = allWhatsAppStatuses[position]
            println("StatusViewHolder-statusData: $statusData")
            val uri = Uri.parse(statusData.path)  // Convert the String to Uri
            if(!isLocalSaved) {
                val existence = doesContentUriExist(context, uri)
                println("StatusViewHolder: $existence")
                if(!existence ) {
                    return
                }
            }

            if(!isLocalSaved) {
                binding.lastModified.visible()
                Glide.with(context).load(uri).into(binding.imageView)
            } else {
                binding.lastModified.gone()
                Glide.with(context)
                    .load(Uri.fromFile(File(statusData.path)))
                    .into(binding.imageView)
            }


            if (statusData.type == STATUS_TYPE.VIDEO) {
                binding.duration.visible()
                binding.imageTypeVideo.setImageResource(R.drawable.ic_video)
                binding.duration.text = formatDurationTime(statusData.duration)
            } else {
                binding.imageTypeVideo.setImageResource(R.drawable.ic_image)
                binding.duration.gone()
            }

            binding.lastModified.text = Utils.getRelativeTime(statusData.lastModifiedTime)

            if(!isLocalSaved) {
                binding.fabDownload.visible()
                binding.fabDelete.gone()
            } else {
                binding.fabDownload.gone()
                binding.fabDelete.visible()
            }

            binding.fabDownload.setOnClickListener {
                onEditWhatsAppListener.onDownloadListener(statusData)
            }

            binding.fabDelete.setOnClickListener {
                onEditWhatsAppListener.onDeleteListener(statusData)
            }

            binding.root.setOnClickListener {
                context.startActivity(Intent(context, PlayerActivity::class.java).apply {
                    putExtra("playerURL", statusData.path)
                    putExtra("isWhatsApp", true)
                })
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

interface OnEditWhatsAppListener {
    fun onDownloadListener(status: WhatsAppStatus)
    fun onDeleteListener(status: WhatsAppStatus)
}