package com.ezt.priv.shortvideodownloader.ui.whatsapp

import android.os.Bundle
import com.bumptech.glide.Glide
import com.ezt.priv.shortvideodownloader.databinding.ActivityPreviewBinding
import com.ezt.priv.shortvideodownloader.ui.BaseActivity2

class PreviewActivity : BaseActivity2<ActivityPreviewBinding>(ActivityPreviewBinding::inflate) {
    private val videoUrl by lazy {
        intent.getStringExtra("playerURL") ?: ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.apply {
            Glide.with(this@PreviewActivity).load(videoUrl).into(statusImage)

            iconHome.setOnClickListener {
                finish()
            }
        }
    }
}