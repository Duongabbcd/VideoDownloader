package com.ezt.video.downloader.ui.player

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.ezt.video.downloader.R
import com.ezt.video.downloader.databinding.ActivityPlayerBinding
import com.ezt.video.downloader.ui.BaseActivity2
import com.ezt.video.downloader.ui.home.MainActivity
import com.ezt.video.downloader.util.Common
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerActivity : BaseActivity2<ActivityPlayerBinding>(ActivityPlayerBinding::inflate) {
    private val videoUrl by lazy {
        intent.getStringExtra("playerURL") ?: ""
    }

    private var player: ExoPlayer? = null
    private var hasInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 🔐 Register here

        binding.apply {
            Log.d(TAG, "PlayerActivity: $videoUrl")
            playButton.setOnClickListener {
                if (!hasInitialized) {
                    initializePlayer(videoUrl)
                } else {
                    player?.playWhenReady = true
                }
            }

//            downloadButton.setOnClickListener {
//                downloadMediaFiles()
//            }

            iconBack.setOnClickListener {
                finish()
            }

            iconHome.setOnClickListener {
                startActivity(Intent(this@PlayerActivity, MainActivity::class.java))
            }
        }
    }


    private fun initializePlayer(url: String) {
        println("initializePlayer: $url")
        hasInitialized = true
        binding.apply {
            playerView.visibility = PlayerView.VISIBLE

            player = ExoPlayer.Builder(this@PlayerActivity).build().also { exoPlayer ->
                playerView.player = exoPlayer

                val mediaItem = MediaItem.fromUri(Uri.parse(url))
                exoPlayer.setMediaItem(mediaItem)
                // Loop one track indefinitely
                exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
                // Playback error listener
                exoPlayer.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Toast.makeText(
                            this@PlayerActivity,
                            "Playback error: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                })

                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }
        }

    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    private fun releasePlayer() {
        player?.release()
        player = null
        hasInitialized = false
    }

    override fun onResume() {
        super.onResume()

    }

    companion object {
        var returnedFromSettings = false
        private val TAG = PlayerActivity::class.java.simpleName
    }
}