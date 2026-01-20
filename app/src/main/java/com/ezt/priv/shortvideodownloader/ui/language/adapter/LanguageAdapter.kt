package com.ezt.priv.shortvideodownloader.ui.language.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.databinding.ItemLanguageBinding

/**
 * Adapter for displaying language selection items
 */
class LanguageAdapter(private val onClickListener: OnClickListener) :
    RecyclerView.Adapter<LanguageAdapter.ViewHolder>() {

    private lateinit var context: Context
    private var selectedLanguageKey = ""
    private var languageList: ArrayList<Language> = ArrayList()

    class ViewHolder(val binding: ItemLanguageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        return ViewHolder(
            ItemLanguageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val language = languageList[position]
        holder.bind(language)
    }

    override fun getItemCount(): Int = languageList.size

    /**
     * Interface for language selection callbacks
     */
    interface OnClickListener {
        fun onClickListener(position: Int, language: Language)
    }

    /**
     * Update the adapter data and selected language
     */
    fun updateData(data: ArrayList<Language>, selectedLang: String) {
        languageList.clear()
        languageList.addAll(data)
        selectedLanguageKey = selectedLang
        notifyDataSetChanged()
    }

    /**
     * Update the selected language position
     */
    fun updatePosition(languageKey: String) {
        selectedLanguageKey = languageKey
        notifyDataSetChanged()
    }

    /**
     * Bind language data to view holder
     */
    private fun ViewHolder.bind(language: Language) {
        binding.apply {
            // Set language icon
            languageIcon.setImageResource(language.img)

            // Set language name with marquee effect
            languageName.text = language.name
            languageName.isSelected = true

            // Update UI based on selection state
            updateSelectionState(language.key == selectedLanguageKey)

            // Set click listener
            root.setOnClickListener {
                onClickListener.onClickListener(adapterPosition, language)
            }
        }
    }

    /**
     * Update the visual state based on selection
     */
    @SuppressLint("ResourceAsColor")
    private fun ItemLanguageBinding.updateSelectionState(isSelected: Boolean) {
        if (isSelected) {
            language.setBackgroundResource(R.drawable.checkbox_select)
        } else {
            language.setBackgroundResource(R.drawable.checkbox_un_select)
        }
    }
}

/**
 * Data class representing a language option
 */
data class Language(
    val img: Int,
    val name: String,
    val key: String
)
