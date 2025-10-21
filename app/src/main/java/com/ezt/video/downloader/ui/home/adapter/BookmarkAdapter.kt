package com.ezt.video.downloader.ui.home.adapter

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ezt.video.downloader.R
import com.ezt.video.downloader.databinding.BookmarkViewBinding
import com.ezt.video.downloader.ui.browse.BrowseActivity
import com.ezt.video.downloader.ui.home.Bookmark
import com.ezt.video.downloader.ui.whatsapp.WhatsAppActivity
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
        val packageManager = context.packageManager

        fun bind(position: Int) {
            binding.apply {
                val bookmark = allBookmarks[position]
                try {
                    Glide.with(context).load(bookmark.imagePath ).placeholder(R.drawable.icon_default_app).error(R.drawable.icon_default_app)
                        .into(binding.bookmarkIcon)
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
                // fallback to BrowseActivity
                openInBrowseActivity(bookmark.url)
            }
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
            context.startActivity(Intent(context, BrowseActivity::class.java).apply {
                putExtra("receivedURL", url)
            })
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
