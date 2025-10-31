package com.ezt.priv.shortvideodownloader.ui.home.adapter

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.databinding.BookmarkViewBinding
import com.ezt.priv.shortvideodownloader.ui.browse.BrowseActivity
import com.ezt.priv.shortvideodownloader.ui.home.Bookmark
import com.ezt.priv.shortvideodownloader.ui.social.FacebookInfoActivity
import com.ezt.priv.shortvideodownloader.ui.whatsapp.WhatsAppActivity
import com.google.android.material.snackbar.Snackbar

class BookmarkAdapter(private val isActivity: Boolean = false) :
    RecyclerView.Adapter<BookmarkAdapter.MyHolder>() {
    private lateinit var context: Context
    private val allBookmarks =  mutableListOf<Bookmark>()
    private val colors by lazy {
        context.resources.getIntArray(R.array.myColors)
    }

    fun submitList(list: List<Bookmark>) {
        allBookmarks.clear()
        allBookmarks.addAll(list)
        notifyDataSetChanged()
    }

    inner class MyHolder(
        private val binding: BookmarkViewBinding
    ) : RecyclerView.ViewHolder(binding.root ) {

        fun bind(position: Int) {
            binding.apply {
                val bookmark = allBookmarks[position]
                try {
                    val iconName = "icon_${bookmark.name.toLowerCase()}"
                    val iconRes = context.resources.getIdentifier(iconName, "drawable", context.packageName)

                    println("BookmarkAdapter: $iconName and $iconRes")

                    binding.bookmarkIcon.setImageResource(iconRes)
                } catch (e: Exception) {
                    e.printStackTrace()
                    binding.bookmarkIcon.setBackgroundColor(colors[(colors.indices).random()])
                }

                binding.bookmarkName.text = bookmark.name

                binding.root.setOnClickListener {
                    if(bookmark.name.contains("WhatsApp", true)) {
                        context.startActivity(Intent(context, WhatsAppActivity::class.java))
                    } else {
                        openBookmark(bookmark)

                    }

                }
            }
        }

        private fun openBookmark(bookmark: Bookmark) {
            openInBrowseActivity(bookmark.url)
        }


        private fun openInBrowseActivity(url: String) {
            when {
                url.contains("facebook", true) ->    context.startActivity(Intent(context,
                    FacebookInfoActivity::class.java).apply {
                    putExtra("facebookURL", url)
                })

                url.contains("instagram", true) ->    context.startActivity(Intent(context,
                    FacebookInfoActivity::class.java).apply {
                    putExtra("facebookURL", url)
                })

                url.contains("whatsapp", true) ->    context.startActivity(Intent(context,
                    WhatsAppActivity::class.java))

                else -> {
                    context.startActivity(Intent(context, BrowseActivity::class.java).apply {
                        putExtra("receivedURL", url)
                    })
                }
            }

        }

    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyHolder {
        context = parent.context
        return MyHolder(
            binding = BookmarkViewBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(
        holder: MyHolder,
        position: Int
    ) {
     holder.bind(position)
    }

    override fun getItemCount(): Int {
        return allBookmarks.size
    }
}
