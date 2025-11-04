package com.ezt.priv.shortvideodownloader.ui.player

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
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
import com.googlecode.mp4parser.FileDataSourceImpl
import com.googlecode.mp4parser.authoring.builder.FragmentedMp4Builder
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class PlayerActivity : BaseActivity2<ActivityPlayerBinding>(ActivityPlayerBinding::inflate) {
    private val videoUrl by lazy {
        intent.getStringExtra("playerURL") ?: ""
    }

    private val videoNameStr by lazy {
        intent.getStringExtra("playerName") ?: ""
    }
    private var isFragmented: Boolean = false

    private val isWhatsApp by lazy {
        intent.getBooleanExtra("isWhatsApp", false)
    }
    private var decryptedFile: File? = null

    private lateinit var player: ExoPlayer
    private var hasInitialized = false
    private var isUserSeeking = false

    private val handler = Handler(Looper.getMainLooper())
    private val updateSeekBarRunnable = object : Runnable {
        override fun run() {
            val duration = player.duration
            if (duration != C.TIME_UNSET && !isUserSeeking) {
                val pos = player.currentPosition.coerceAtMost(duration)
                binding.playingSeekbar.progress = pos.toInt()
                binding.currentTime.text = formatTime(pos)
            }
            handler.postDelayed(this, 500)
        }
    }

    private var fadeRunnableTop: Runnable? = null
    private var fadeRunnableBottom: Runnable? = null


    private var startPositionValue: Long = 0L
    private var playWhenReadyState: Boolean = true

    @SuppressLint("ClickableViewAccessibility")
    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 🔐 Register here
        returnedFromSettings = true
        startPositionValue = savedInstanceState?.getLong("playback_position") ?: 0L
        playWhenReadyState = savedInstanceState?.getBoolean("play_when_ready") ?: true

        isFragmented = intent.getBooleanExtra("isFragmented", false)
        if (videoUrl.endsWith(".jpg", true) || videoUrl.endsWith(".png", true) || videoUrl.endsWith(
                ".jpeg",
                true
            )
        ) {
            binding.apply {
                playerView.gone()
                playerButtons.gone()
                playerControls.gone()
                iconRotation.gone()
                iconVolume.gone()

                iconHome.setOnClickListener {
                    finish()
                }
            }
        } else {
            binding.apply {
                Log.d(TAG, "PlayerActivity: $videoUrl and $videoNameStr")
                videoName.text = videoNameStr
                videoName.isSelected = true
                playPause.setOnClickListener {
                    player.let { exoPlayer ->
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
                            isUserSeeking = true
                            val seekTime = progress.toLong()
                            binding.currentTime.text = formatTime(seekTime)
                        }
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                        isUserSeeking = true
                    }
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                        val duration = player.duration
                        if (duration != C.TIME_UNSET) {
                            val seekTo = (seekBar?.progress ?: 0).coerceIn(0, duration.toInt()).toLong()
                            player.seekTo(seekTo)
                        }
                        isUserSeeking = false
                    }
                })

                forward10.setOnClickListener {
                    val duration = player.duration
                    if (duration != C.TIME_UNSET) {
                        val target = (player.currentPosition + 10_000).coerceAtMost(duration)
                        println("forward10: $target")
                        player.seekTo(target)
                    }
                }

                rewind10.setOnClickListener {
                    val target = (player.currentPosition - 10_000).coerceAtLeast(0)
                    println("rewind10: $target")
                    player.seekTo(target)
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

                binding.playerView.setOnTouchListener { _, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            // Cancel previous fades
                            fadeRunnableTop?.let { binding.topLayout.removeCallbacks(it) }
                            fadeRunnableBottom?.let { binding.bottomLayout.removeCallbacks(it) }

                            // Show layouts immediately
                            binding.topLayout.visibility = View.VISIBLE
                            binding.topLayout.alpha = 1f
                            binding.bottomLayout.visibility = View.VISIBLE
                            binding.bottomLayout.alpha = 1f
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            // Start fade after finger lifted
                            fadeRunnableTop = binding.topLayout.fadeInAndOut(3000L)
                            fadeRunnableBottom = binding.bottomLayout.fadeInAndOut(3000L)
                        }
                    }
                    true // consume the touch
                }

                binding.iconVolume.stopFadeOnTouch()
                binding.iconRotation.stopFadeOnTouch()
                binding.forward10.stopFadeOnTouch()
                binding.rewind10.stopFadeOnTouch()
                binding.playPause.stopFadeOnTouch()
                binding.iconHome.stopFadeOnTouch()
                binding.videoName.stopFadeOnTouch()
                binding.playingSeekbar.setOnTouchListener { _, event ->
                    when(event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            fadeRunnableTop?.let { binding.topLayout.removeCallbacks(it) }
                            fadeRunnableBottom?.let { binding.bottomLayout.removeCallbacks(it) }
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            fadeRunnableTop = binding.topLayout.fadeInAndOut(3000L)
                            fadeRunnableBottom = binding.bottomLayout.fadeInAndOut(3000L)
                        }
                    }
                    false
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
            player.playWhenReady = true
        }

        fadeRunnableTop = binding.topLayout.fadeInAndOut(3000L)
        fadeRunnableBottom = binding.bottomLayout.fadeInAndOut(3000L)
    }


    private fun initializePlayer(encryptedFilePath: String) {

        hasInitialized = true
        val isEncrypted = CryptoConstants.isFileEncryptedByUs(File(encryptedFilePath))
        println("initializePlayer: $encryptedFilePath and $isEncrypted")
        println("isFragmented: $isFragmented")

        try {
            // Step 0: Decrypt file if needed
            decryptedFile = if (!isWhatsApp)
                CryptoConstants.decryptMediaHeader(File(encryptedFilePath), this)
            else
                File(encryptedFilePath)

            lifecycleScope.launch {
                // Step 1: Decide file to play based on `isFragmented`
                val fileToPlay: File = if (isFragmented) {
                    // Show "please wait" if needed
                    binding.loadingText.visibility = View.VISIBLE

                    // Remux on IO thread and wait until done
                    val remuxedFile = File(filesDir, "remuxed_${decryptedFile!!.name}")
                    withContext(Dispatchers.IO) {
                        remuxWithMediaMuxer(decryptedFile!!, remuxedFile)
                    }

                    binding.loadingText.visibility = View.GONE
                    remuxedFile
                } else {
                    decryptedFile!!
                }

                // Step 2: Initialize ExoPlayer
                binding.apply {
                    playerView.visibility = PlayerView.VISIBLE

                    player = ExoPlayer.Builder(this@PlayerActivity).build().also { exoPlayer ->
                        playerView.player = exoPlayer
                        exoPlayer.volume = 1f
                        iconVolume.setImageResource(R.drawable.icon_volume_on)

                        val mediaItem = MediaItem.fromUri(Uri.fromFile(fileToPlay))
                        exoPlayer.setMediaItem(mediaItem)
                        exoPlayer.repeatMode = Player.REPEAT_MODE_OFF

                        exoPlayer.addListener(object : Player.Listener {
                            override fun onPlaybackStateChanged(playbackState: Int) {
                                if (playbackState == Player.STATE_READY && player.duration != C.TIME_UNSET) {
                                    playingSeekbar.max = player.duration.toInt()
                                    totalTime.text = formatTime(player.duration)
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
                                playPause.setImageResource(
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

        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "Failed to decrypt or play file: ${e.message}")
        }
    }



    /** Remux fragmented MP4 into a normal MP4 using MediaMuxer */
    private fun remuxWithMediaMuxer(inputFile: File, outputFile: File) {
        val extractor = MediaExtractor()
        extractor.setDataSource(inputFile.absolutePath)

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val trackMap = mutableMapOf<Int, Int>()

        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                extractor.selectTrack(i)
                val newTrackIndex = muxer.addTrack(format)
                trackMap[i] = newTrackIndex
            }
        }

        muxer.start()

        val buffer = ByteBuffer.allocate(1_024 * 1_024)
        val bufferInfo = MediaCodec.BufferInfo()

        while (true) {
            val sampleTrackIndex = extractor.sampleTrackIndex
            if (sampleTrackIndex < 0) break

            val trackIndex = trackMap[sampleTrackIndex] ?: break
            bufferInfo.offset = 0
            bufferInfo.size = extractor.readSampleData(buffer, 0)
            if (bufferInfo.size < 0) break

            bufferInfo.presentationTimeUs = extractor.sampleTime
            bufferInfo.flags = extractor.sampleFlags
            muxer.writeSampleData(trackIndex, buffer, bufferInfo)
            extractor.advance()
        }

        muxer.stop()
        muxer.release()
        extractor.release()
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

    fun View.stopFadeOnTouch() {
        setOnTouchListener { v, event ->
            when(event.action) {
                MotionEvent.ACTION_DOWN -> {
                    fadeRunnableTop?.let { binding.topLayout.removeCallbacks(it) }
                    fadeRunnableBottom?.let { binding.bottomLayout.removeCallbacks(it) }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    fadeRunnableTop = binding.topLayout.fadeInAndOut(3000L)
                    fadeRunnableBottom = binding.bottomLayout.fadeInAndOut(3000L)
                }
            }
            false // allow click or seek to continue
        }
    }

    companion object {
        var returnedFromSettings = false
        private val TAG = PlayerActivity::class.java.simpleName

        fun View.fadeInAndOut(duration: Long = 3000L): Runnable {
            // Cancel previous animations first
            animate().cancel()
            visibility = View.VISIBLE
            alpha = 1f

            val runnable = Runnable {
                animate()
                    .alpha(0f)
                    .setDuration(500L)
                    .withEndAction { visibility = View.GONE }
                    .start()
            }
            postDelayed(runnable, duration)
            return runnable
        }



    }
}