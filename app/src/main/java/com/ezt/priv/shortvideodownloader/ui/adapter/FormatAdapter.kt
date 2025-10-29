package com.ezt.priv.shortvideodownloader.ui.adapter

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ezt.priv.shortvideodownloader.database.models.expand.non_table.FormatRecyclerView
import com.ezt.priv.shortvideodownloader.database.models.expand.table.Format
import com.ezt.priv.shortvideodownloader.databinding.FormatItemBinding
import com.ezt.priv.shortvideodownloader.databinding.FormatTypeLabelBinding
import com.ezt.priv.shortvideodownloader.util.UiUtil

class FormatAdapter(onItemClickListener: OnItemClickListener, activity: Activity) : ListAdapter<FormatRecyclerView?, FormatAdapter.ViewHolder>(AsyncDifferConfig.Builder(
    DIFF_CALLBACK
).build()) {
    private val onItemClickListener: OnItemClickListener
    private val activity: Activity
    var selectedVideoFormat: Format? = null
    val selectedAudioFormats: MutableList<Format> = mutableListOf()
    private var canMultiSelectAudio: Boolean = false
    private var formats: MutableList<FormatRecyclerView?> = mutableListOf()


    init {
        this.onItemClickListener = onItemClickListener
        this.activity = activity
    }

    sealed class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        class LabelViewHolder(val binding: FormatTypeLabelBinding) : ViewHolder(binding.root)

        class FormatViewHolder(
            val binding: FormatItemBinding,
            val activity: Activity,
            val onItemClickListener: OnItemClickListener,
            val adapter: FormatAdapter
        ) : ViewHolder(binding.root) {

            fun bind(item: FormatRecyclerView) {
                val format = item.format ?: return
                val card = binding.formatCardConstraintLayout

                UiUtil.populateFormatCard(activity, card, format)

                // Handle selection
                val isSelected = adapter.selectedVideoFormat == format ||
                        adapter.selectedAudioFormats.any { it == format }
                card.isChecked = isSelected

                card.setOnClickListener {
                    if (!adapter.canMultiSelectAudio) {
                        onItemClickListener.onItemSelect(format, null)
                    } else {
                        if (format.isVideo()) {
                            if (card.isChecked) {
                                onItemClickListener.onItemSelect(format, adapter.selectedAudioFormats)
                            } else {
                                adapter.selectedVideoFormat = format
                                adapter.notifyDataSetChanged()
                            }
                        } else {
                            if (card.isChecked) {
                                adapter.selectedAudioFormats.remove(format)
                            } else {
                                adapter.selectedAudioFormats.add(format)
                            }
                            adapter.notifyDataSetChanged()
                        }
                    }
                }

                card.setOnLongClickListener {
                    UiUtil.showFormatDetails(format, activity)
                    true
                }
            }
        }
    }


    override fun submitList(list: MutableList<FormatRecyclerView?>?) {
        formats = list ?: mutableListOf()
        super.submitList(list)
    }

    fun setCanMultiSelectAudio(it: Boolean) {
        canMultiSelectAudio = it
    }

    override fun getItemViewType(position: Int): Int {
        try {
            val isLabel = formats[position]!!.label != null
            return if (isLabel) 0 else 1
        }catch (err: Exception) {
            return 1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return if (viewType == 0) {
            val binding = FormatTypeLabelBinding.inflate(inflater, parent, false)
            ViewHolder.LabelViewHolder(binding)
        } else {
            val binding = FormatItemBinding.inflate(inflater, parent, false)
            ViewHolder.FormatViewHolder(binding, activity, onItemClickListener, this)
        }
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position) ?: return

        when (holder) {
            is ViewHolder.LabelViewHolder -> {
                holder.binding.title.text = item.label
            }
            is ViewHolder.FormatViewHolder -> {
                holder.bind(item)
            }
        }
    }



    interface OnItemClickListener {
        fun onItemSelect(item: Format, audioFormats: List<Format>?)
    }

    companion object {
        fun Format.isVideo(): Boolean {
            return this.vcodec.isNotBlank() && this.vcodec != "none"
        }

        private val DIFF_CALLBACK: DiffUtil.ItemCallback<FormatRecyclerView> = object : DiffUtil.ItemCallback<FormatRecyclerView>() {
            override fun areItemsTheSame(oldItem: FormatRecyclerView, newItem: FormatRecyclerView): Boolean {
                return oldItem.label == newItem.label && oldItem.format?.format_id == newItem.format?.format_id && oldItem.format?.format_note == newItem.format?.format_note
            }

            override fun areContentsTheSame(oldItem: FormatRecyclerView, newItem: FormatRecyclerView): Boolean {
                return oldItem.label == newItem.label && oldItem.format?.format_id == newItem.format?.format_id && oldItem.format?.format_note == newItem.format?.format_note
            }
        }
    }
}