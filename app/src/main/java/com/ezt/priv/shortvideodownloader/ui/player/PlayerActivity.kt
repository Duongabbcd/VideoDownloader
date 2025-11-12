package com.ezt.priv.shortvideodownloader.ui.player

import android.annotation.SuppressLint
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
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.databinding.ActivityPlayerBinding
import com.ezt.priv.shortvideodownloader.ui.BaseActivity2
import com.ezt.priv.shortvideodownloader.util.Common.gone
import com.ezt.priv.shortvideodownloader.work.CryptoConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

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

    private var isLocked = false

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
                            val seekTo =
                                (seekBar?.progress ?: 0).coerceIn(0, duration.toInt()).toLong()
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
                    changeLockScreenOrientation()
                }

                binding.iconRotation.setOnClickListener {
                    if(isLocked) {
                        return@setOnClickListener
                    }
                    binding.root.post {
                        val currentOrientation = resources.configuration.orientation
                        when (currentOrientation) {
                            Configuration.ORIENTATION_PORTRAIT -> {
                                // Rotate to landscape
                                requestedOrientation =
                                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                // Fit with black bars (don’t stretch)
                                playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }

                            Configuration.ORIENTATION_LANDSCAPE -> {
                                // Rotate back to portrait
                                requestedOrientation =
                                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                                // Fit again (good default)
                                playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }

                            else -> {
                                requestedOrientation =
                                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
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
                    when (event.action) {
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

    private fun changeLockScreenOrientation() {
        if (!isLocked) {
            val currentOrientation = resources.configuration.orientation
            requestedOrientation = when (currentOrientation) {
                Configuration.ORIENTATION_PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                Configuration.ORIENTATION_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                else -> ActivityInfo.SCREEN_ORIENTATION_LOCKED
            }
            isLocked = true
            binding.iconVolume.setImageResource(R.drawable.icon_locked)
//            Toast.makeText(this, "Screen locked", Toast.LENGTH_SHORT).show()
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
            isLocked = false
            binding.iconVolume.setImageResource(R.drawable.icon_lock_screen)
//            Toast.makeText(this, "Screen unlocked", Toast.LENGTH_SHORT).show()
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

    @OptIn(UnstableApi::class)
    private fun initializePlayer(encryptedFilePath: String) {
        hasInitialized = true
        val isEncrypted = CryptoConstants.isFileEncryptedByUs(File(encryptedFilePath))
        println("initializePlayer: $encryptedFilePath and $isEncrypted")
        println("isFragmented: $isFragmented")

        try {
            // Step 0: Decrypt if needed
            decryptedFile = if (!isWhatsApp)
                CryptoConstants.decryptMediaHeader(File(encryptedFilePath), this)
            else
                File(encryptedFilePath)

            val fragmentFiles = listOf(decryptedFile!!)

            binding.loadingText.visibility = View.VISIBLE

            lifecycleScope.launch(Dispatchers.IO) {
                // Step 1: Prepare MediaItems for all fragments
                val mediaItems = fragmentFiles.map { fragmentFile ->
                    MediaItem.fromUri(Uri.fromFile(fragmentFile))
                }

                withContext(Dispatchers.Main) {
                    // Step 2: Initialize ExoPlayer
                    val renderersFactory = DefaultRenderersFactory(this@PlayerActivity)
                        .setEnableDecoderFallback(true)
                        .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)

                    binding.apply {
                        player = ExoPlayer.Builder(this@PlayerActivity, renderersFactory).build().also { exoPlayer ->
                            playerView.player = exoPlayer
                            exoPlayer.volume = 1f
//                            iconVolume.setImageResource(R.drawable.icon_volume_on)

                            // Step 3: Add all fragments as MediaItems
                            exoPlayer.setMediaItems(mediaItems)
                            exoPlayer.repeatMode = Player.REPEAT_MODE_OFF

                            // Step 4: Listeners for UI updates
                            exoPlayer.addListener(object : Player.Listener {
                                override fun onPlaybackStateChanged(playbackState: Int) {
                                    if (playbackState == Player.STATE_READY && exoPlayer.duration != C.TIME_UNSET) {
                                        playingSeekbar.max = exoPlayer.duration.toInt()
                                        totalTime.text = formatTime(exoPlayer.duration)
                                    }
                                }

                                override fun onPlayerError(error: PlaybackException) {
                                    Log.d(TAG, "Playback error: ${error.message}")
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
                            binding.loadingText.visibility = View.GONE
                        }
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "Failed to decrypt or play file: ${e.message}")
        }
    }



    /** Remux fragments sequentially with audio/video sync */
    private fun remuxFragmentsSequential(fragmentFiles: List<File>, outputFile: File) {
        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val trackMap = mutableMapOf<Int, Int>()

        fragmentFiles.forEach { file ->
            val extractor = MediaExtractor()
            extractor.setDataSource(file.absolutePath)

            // Add tracks if not already added
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (!trackMap.containsKey(i)) {
                    trackMap[i] = muxer.addTrack(format)
                }
                extractor.selectTrack(i)
            }

            muxer.start()

            val buffer = ByteBuffer.allocate(256 * 1024) // 256 KB buffer
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

            extractor.release()
        }

        muxer.stop()
        muxer.release()
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
            when (event.action) {
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

data class SampleData(
    val trackIndex: Int,
    val buffer: ByteBuffer,
    val info: MediaCodec.BufferInfo
)