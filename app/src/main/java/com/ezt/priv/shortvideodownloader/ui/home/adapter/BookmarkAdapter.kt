package com.ezt.priv.shortvideodownloader.ui.home.adapter

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.databinding.BookmarkListViewBinding
import com.ezt.priv.shortvideodownloader.databinding.BookmarkViewBinding
import com.ezt.priv.shortvideodownloader.ui.browse.BrowseActivity
import com.ezt.priv.shortvideodownloader.ui.home.Bookmark
import com.ezt.priv.shortvideodownloader.ui.social.FacebookInfoActivity
import com.ezt.priv.shortvideodownloader.ui.whatsapp.WhatsAppActivity
import com.ezt.priv.shortvideodownloader.util.Common.gone
import com.ezt.priv.shortvideodownloader.util.Common.visible
import com.google.android.material.snackbar.Snackbar

class BookmarkAdapter() :
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
        val packageManager = context.packageManager

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

        private fun openBookmark1(bookmark: Bookmark) {
            val packageName = bookmark.packageName

            if (!packageName.isNullOrEmpty() && isAppInstalled(packageName)) {
                // Open the installed app
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                } else {
                    // fallback in rare cases
                    openInBrowseActivity(bookmark.url)
                }
            } else {
                openInBrowseActivity(bookmark.url)
            }

        }

        private fun openBookmark(bookmark: Bookmark) {
            openInBrowseActivity(bookmark.url)
        }


        private fun isAppInstalled(packageName: String): Boolean {
            return try {
                packageManager.getPackageInfo(packageName, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
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

                url.contains("tiktok", true) ->    context.startActivity(Intent(context,
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

class BookmarkListAdapter(): RecyclerView.Adapter<BookmarkListAdapter.MyHolder>() {
    private lateinit var context1: Context
    private var isFewerThanEight: Boolean = false
    private val allListBookmarks =  mutableListOf<List<Bookmark>>()
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyHolder {
        context1 = parent.context
        return MyHolder(
            binding = BookmarkListViewBinding.inflate(
                LayoutInflater.from(context1),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
       holder.bind(position)
    }

    override fun getItemCount(): Int =allListBookmarks.size

    fun submitList(input: List<List<Bookmark>>, isMoreThanEight: Boolean) {
        allListBookmarks.clear()
        allListBookmarks.addAll(input)

        isFewerThanEight = !isMoreThanEight
        notifyDataSetChanged()
    }

    inner class MyHolder(private val binding: BookmarkListViewBinding) :  RecyclerView.ViewHolder(binding.root ) {
        private lateinit var bookmarkAdapter: BookmarkAdapter

        fun bind(position: Int) {
            val bookmarks = allListBookmarks[position]
            binding.apply {
                bookmarkAdapter = BookmarkAdapter()
                bookmarkAdapter.submitList(bookmarks)

                allBookmarks.adapter = bookmarkAdapter
                allBookmarks.layoutManager = GridLayoutManager(context1, 2)

                if (isFewerThanEight) image.gone() else image.visible()

                val imageRes = if(position == 0) R.drawable.icon_home1 else R.drawable.icon_home2
                image.setImageResource(imageRes)
            }
        }
    }

}