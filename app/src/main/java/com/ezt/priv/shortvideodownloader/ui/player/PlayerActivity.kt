package com.ezt.priv.shortvideodownloader.ui.player

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.bumptech.glide.Glide
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.databinding.ActivityPlayerBinding
import com.ezt.priv.shortvideodownloader.ui.BaseActivity2
import com.ezt.priv.shortvideodownloader.util.Common.gone
import com.ezt.priv.shortvideodownloader.util.Common.visible
import com.ezt.priv.shortvideodownloader.work.CryptoConstants
import java.io.File

class PlayerActivity : BaseActivity2<ActivityPlayerBinding>(ActivityPlayerBinding::inflate) {
    private val videoUrl by lazy {
        intent.getStringExtra("playerURL") ?: ""
    }

    private val videoNameStr by lazy {
        intent.getStringExtra("playerName") ?: ""
    }

    private val isWhatsApp by lazy {
        intent.getBooleanExtra("isWhatsApp", false)
    }
    private var decryptedFile: File? = null

    private lateinit var player: ExoPlayer
    private var hasInitialized = false

    private val handler = Handler(Looper.getMainLooper())
    private val updateSeekBarRunnable = object : Runnable {
        override fun run() {
            player?.let {
                val duration = player?.duration ?: 0
                if (binding.playingSeekbar.max != duration.toInt()) {
                    binding.totalTime.text = formatTime(duration)
                    binding.playingSeekbar.max = duration.toInt()
                }

                val position = it.currentPosition
                binding.playingSeekbar.progress = position.toInt()
                binding.currentTime.text = formatTime(position)
            }
            handler.postDelayed(this, 500)
        }
    }

    private var startPositionValue: Long = 0L
    private var playWhenReadyState: Boolean = true

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 🔐 Register here
        returnedFromSettings = true
        startPositionValue = savedInstanceState?.getLong("playback_position") ?: 0L
        playWhenReadyState = savedInstanceState?.getBoolean("play_when_ready") ?: true


        if (videoUrl.endsWith(".jpg", true) || videoUrl.endsWith(".png", true) || videoUrl.endsWith(
                ".jpeg",
                true
            )
        ) {
            binding.apply {
                statusImage.visible()
                playerView.gone()
                playerButtons.gone()
                playerControls.gone()
                iconRotation.gone()
                iconVolume.gone()
                Glide.with(this@PlayerActivity)
                    .load(Uri.fromFile(File(videoUrl)))
                    .into(binding.statusImage)


                iconHome.setOnClickListener {
                    finish()
                }
            }
        } else {
            binding.apply {
                statusImage.gone()
                Log.d(TAG, "PlayerActivity: $videoUrl and $videoNameStr")
                videoName.text = videoNameStr
                videoName.isSelected = true
                playPause.setOnClickListener {
                    player?.let { exoPlayer ->
                        when {
                            exoPlayer.playbackState == Player.STATE_ENDED -> {
                                // Restart from beginning
                                exoPlayer.seekTo(0)
                                exoPlayer.playWhenReady = true
                            }

                            exoPlayer.isPlaying -> {
                                exoPlayer.pause()
                            }

                            else -> {
                                exoPlayer.play()
                            }
                        }
                    }
                }

                playingSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        if (fromUser) {
                            player?.seekTo(progress.toLong())
                            binding.currentTime.text = formatTime(progress.toLong())
                        }
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                        player?.seekTo(seekBar?.progress?.toLong() ?: 0L)
                    }
                })

                forward10.setOnClickListener {
                    player?.seekTo((player?.currentPosition ?: 0) + 10_000)
                }

                rewind10.setOnClickListener {
                    player?.seekTo((player?.currentPosition ?: 0) - 10_000)
                }

                iconHome.setOnClickListener {
                    releasePlayer()
                    finish()
                }

                iconVolume.setOnClickListener {
                    toggleMute()
                }

                binding.iconRotation.setOnClickListener {
                    binding.root.post {
                        val currentOrientation = resources.configuration.orientation
                        when (currentOrientation) {
                            Configuration.ORIENTATION_PORTRAIT -> {
                                // Rotate to landscape
                                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                // Fit with black bars (don’t stretch)
                                playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }

                            Configuration.ORIENTATION_LANDSCAPE -> {
                                // Rotate back to portrait
                                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                                // Fit again (good default)
                                playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }

                            else -> {
                                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        }
                    }
                }


            }
        }
    }

    private fun toggleMute() {
        player?.let {
            val isMuted = it.volume == 0f
            it.volume = if (isMuted) 1f else 0f

            // Optionally change the icon depending on the state
            binding.iconVolume.setImageResource(
                if (isMuted) R.drawable.icon_volume_on else R.drawable.icon_volume_off
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (!hasInitialized) {
            initializePlayer(videoUrl)
        } else {
            player?.playWhenReady = true
        }
    }


    private fun initializePlayer(encryptedFilePath: String) {

        hasInitialized = true
        val isEncrypted = CryptoConstants.isFileEncryptedByUs(File(encryptedFilePath))
        println("initializePlayer: $encryptedFilePath and $isEncrypted")
        try {
            decryptedFile = if(!isWhatsApp) CryptoConstants.decryptMediaHeader(File(encryptedFilePath), this) else File(encryptedFilePath)
            binding.apply {
                playerView.visibility = PlayerView.VISIBLE

                player = ExoPlayer.Builder(this@PlayerActivity).build().also { exoPlayer ->
                    playerView.player = exoPlayer
                    exoPlayer.volume = 1f
                    iconVolume.setImageResource(R.drawable.icon_volume_on)
                    val mediaItem = MediaItem.fromUri(Uri.fromFile(decryptedFile))
                    exoPlayer.setMediaItem(mediaItem)
                    // Loop one track indefinitely
                    exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                    // Playback error listener
                    exoPlayer.addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_ENDED) {

                            }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            Toast.makeText(
                                this@PlayerActivity,
                                "Playback error: ${error.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            super.onIsPlayingChanged(isPlaying)
                            binding.playPause.setImageResource(
                                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                            )
                        }
                    })

                    exoPlayer.prepare()
                    exoPlayer.seekTo(startPositionValue)
                    exoPlayer.playWhenReady = playWhenReadyState
                    handler.post(updateSeekBarRunnable)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "Failed to decrypt file: ${e.message}")
            return
        }
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
        handler.removeCallbacks(updateSeekBarRunnable)
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
        handler.removeCallbacks(updateSeekBarRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    private fun releasePlayer() {
        returnedFromSettings = false
        player.release()
        hasInitialized = false
        handler.removeCallbacks(updateSeekBarRunnable)
        decryptedFile?.delete()
        decryptedFile = null
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        player?.let {
            outState.putLong("playback_position", it.currentPosition)
            outState.putBoolean("play_when_ready", it.playWhenReady)
        }
    }

    companion object {
        var returnedFromSettings = false
        private val TAG = PlayerActivity::class.java.simpleName

        fun View.fadeInAndOut(duration: Long = 1000L) {
            // First, fade in
            this.apply {
                alpha = 0f
                visibility = View.VISIBLE
                animate()
                    .alpha(1f)
                    .setDuration(1000L)
                    .withEndAction {
                        // After fade-in, delay, then fade out
                        postDelayed({
                            animate()
                                .alpha(0f)
                                .setDuration(duration)
                                .withEndAction {
                                    visibility = View.GONE
                                }
                                .start()
                        }, duration)
                    }
                    .start()
            }
        }
    }
}