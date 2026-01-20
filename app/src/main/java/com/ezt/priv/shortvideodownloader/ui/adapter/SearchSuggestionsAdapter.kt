package com.ezt.priv.shortvideodownloader.ui.adapter

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.database.models.expand.non_table.SearchSuggestionItem
import com.ezt.priv.shortvideodownloader.database.models.expand.non_table.SearchSuggestionType
import com.ezt.priv.shortvideodownloader.ui.home.HomeFragment
import com.ezt.priv.shortvideodownloader.util.Common.gone


class SearchSuggestionsAdapter(onItemClickListener: OnItemClickListener, activity: Activity) : ListAdapter<SearchSuggestionItem, SearchSuggestionsAdapter.ViewHolder>(AsyncDifferConfig.Builder(
    DIFF_CALLBACK
).build()) {
    private val onItemClickListener: OnItemClickListener
    private val activity: Activity
    private val sharedPreferences: SharedPreferences

    private lateinit var context: Context

    init {
        this.onItemClickListener = onItemClickListener
        this.activity = activity
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
    }

    class ViewHolder(itemView: View, onItemClickListener: OnItemClickListener?) : RecyclerView.ViewHolder(itemView) {
        val linear: LinearLayout

        init {
            linear = itemView.findViewById(R.id.linear)
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchSuggestionsAdapter.ViewHolder {
        context = parent.context
        val cardView = LayoutInflater.from(parent.context)
            .inflate(R.layout.search_suggestion_item, parent, false)
        return ViewHolder(cardView, onItemClickListener)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position) ?: return
        val linear = holder.linear
        when (item.type){
            SearchSuggestionType.SUGGESTION -> {
                holder.itemView.tag = SearchSuggestionType.SUGGESTION.toString()
                val textView = linear.findViewById<TextView>(R.id.suggestion_text)
                textView.text = item.text
                textView.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_search, 0, 0, 0)

                textView.setOnClickListener {
                    println("${item.type} as 1")
                    onItemClickListener.onSearchSuggestionClick(item.text, true)
                }
                textView.setOnLongClickListener { true }
                val mb = linear.findViewById<ImageButton>(R.id.set_search_query_button)
                mb.setImageResource(R.drawable.ic_arrow_outward)
                mb.setOnClickListener {
                    println("${item.type} as 2")

                    onItemClickListener.onSearchSuggestionAddToSearchBar(item.text)

                }
                if (item.text.contains("www", true)) {
                    holder.itemView.visibility = View.GONE
                }
            }
            SearchSuggestionType.HISTORY -> {
                holder.itemView.tag = SearchSuggestionType.HISTORY.toString()
                val textView = linear.findViewById<TextView>(R.id.suggestion_text)
                textView.text = item.text
                textView.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_restore, 0, 0, 0)
                textView.setOnClickListener {
                    println("${item.type} as 3")
                    onItemClickListener.onSearchSuggestionClick(item.text, true)
                }
                textView.setOnLongClickListener {
                    println("${item.type} as 4")
                    onItemClickListener.onSearchSuggestionLongClick(item.text, position)
                    true
                }

                val mb = linear.findViewById<ImageButton>(R.id.set_search_query_button)
                mb.setImageResource(R.drawable.ic_arrow_outward)
                mb.setOnClickListener {
                    println("${item.type} as 5")
                    onItemClickListener.onSearchSuggestionAddToSearchBar(item.text)
                }
                if (item.text.contains("www", true)) {
                    holder.itemView.visibility = View.GONE
                }
            }
            SearchSuggestionType.CLIPBOARD -> {
                holder.itemView.tag = SearchSuggestionType.CLIPBOARD.toString()
                val textView = linear.findViewById<TextView>(R.id.suggestion_text)
                textView.text = activity.getString(R.string.link_you_copied)
                textView.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_language, 0, 0, 0)
                val mb = linear.findViewById<ImageButton>(R.id.set_search_query_button)
                mb.setImageResource(R.drawable.ic_plus)
                mb.gone()
                mb.setOnClickListener {
                    println("${item.type} as 6")
                    onItemClickListener.onSearchSuggestionAdd(item.text)
                }

                textView.setOnClickListener {
                    println("${item.type} as 7")
                    onItemClickListener.onSearchSuggestionClick(item.text, false)
                }
                textView.setOnLongClickListener { true }
            }
        }
    }
    fun getList() : List<SearchSuggestionItem> {
        return this.currentList
    }

    interface OnItemClickListener {
        fun onSearchSuggestionClick(text: String, isAllowed: Boolean)
        fun onSearchSuggestionAdd(text: String)

        fun onSearchSuggestionLongClick(text: String, position: Int)

        fun onSearchSuggestionAddToSearchBar(text: String)

    }

    companion object {
        private val DIFF_CALLBACK: DiffUtil.ItemCallback<SearchSuggestionItem> = object : DiffUtil.ItemCallback<SearchSuggestionItem>() {
            override fun areItemsTheSame(oldItem: SearchSuggestionItem, newItem: SearchSuggestionItem): Boolean {
                return oldItem.text == newItem.text && oldItem.type == newItem.type
            }

            override fun areContentsTheSame(oldItem: SearchSuggestionItem, newItem: SearchSuggestionItem): Boolean {
                return oldItem.text == newItem.text && oldItem.type == newItem.type
            }
        }
    }
}