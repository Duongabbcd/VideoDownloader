package com.ezt.video.downloader.ui.player

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.ezt.video.downloader.R
import com.ezt.video.downloader.databinding.ActivityPlayerBinding
import com.ezt.video.downloader.ui.BaseActivity2
import com.ezt.video.downloader.util.Common.gone
import com.ezt.video.downloader.work.CryptoConstants
import java.io.File

class PlayerActivity : BaseActivity2<ActivityPlayerBinding>(ActivityPlayerBinding::inflate) {
    private val videoUrl by lazy {
        intent.getStringExtra("playerURL") ?: ""
    }

    private val videoNameStr by lazy {
        intent.getStringExtra("playerName") ?: ""
    }
    private var decryptedFile: File? = null

    private var player: ExoPlayer? = null
    private var hasInitialized = false
    private var currentRotation = 0

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 🔐 Register here

        startPositionValue = savedInstanceState?.getLong("playback_position") ?: 0L
        playWhenReadyState = savedInstanceState?.getBoolean("play_when_ready") ?: true


        binding.apply {
            playerButtons.gone()
            Log.d(TAG, "PlayerActivity: $videoUrl and $videoNameStr")
            videoName.text = videoNameStr
            videoName.isSelected = true
            root.setOnClickListener {
                playerButtons.fadeInAndOut()
            }

            playPause.setOnClickListener {
                player?.let {exoPlayer ->
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
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
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

            iconRotation.setOnClickListener {
                currentRotation = (currentRotation + 90) % 360

                playerView.animate()
                    .rotation(currentRotation.toFloat())
                    .setDuration(300)
                    .start()
            }
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
        println("initializePlayer: $encryptedFilePath")
        hasInitialized = true
        val isEncrypted = CryptoConstants.isFileEncryptedByUs(File(encryptedFilePath))
        decryptedFile = if(!isEncrypted) {
            File(encryptedFilePath)
        } else {
            Toast.makeText(this@PlayerActivity, resources.getString(R.string.preparing), Toast.LENGTH_SHORT).show()
            try {
                CryptoConstants.decryptMediaHeader(File(encryptedFilePath), this)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to decrypt file: ${e.message}", Toast.LENGTH_LONG).show()
                return
            }
        }

        binding.apply {
            playerView.visibility = PlayerView.VISIBLE

            player = ExoPlayer.Builder(this@PlayerActivity).build().also { exoPlayer ->
                playerView.player = exoPlayer

                val mediaItem = MediaItem.fromUri(Uri.fromFile(decryptedFile))
                exoPlayer.setMediaItem(mediaItem)
                // Loop one track indefinitely
                exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                // Playback error listener
                exoPlayer.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if(playbackState == Player.STATE_ENDED) {

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
        player?.release()
        player = null
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