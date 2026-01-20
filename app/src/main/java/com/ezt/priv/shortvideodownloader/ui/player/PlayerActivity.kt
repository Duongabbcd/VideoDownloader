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
import android.provider.DocumentsContract
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.database.viewmodel.HistoryViewModel
import com.ezt.priv.shortvideodownloader.databinding.ActivityPlayerBinding
import com.ezt.priv.shortvideodownloader.ui.BaseActivity2
import com.ezt.priv.shortvideodownloader.util.Common.visible
import com.ezt.priv.shortvideodownloader.work.CryptoConstants
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
    private var now1 = 0L

    private lateinit var historyViewModel: HistoryViewModel

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
        historyViewModel = ViewModelProvider(this)[HistoryViewModel::class.java]
        returnedFromSettings = true
        startPositionValue = savedInstanceState?.getLong("playback_position") ?: 0L
        playWhenReadyState = savedInstanceState?.getBoolean("play_when_ready") ?: true

        binding.loadingText.visible()

        println("hasInitialized: $hasInitialized")
        isFragmented = intent.getBooleanExtra("isFragmented", false)
        if (!hasInitialized) {
            lifecycleScope.launch {
                initializePlayer(videoUrl)
            }
        } else {
            player.playWhenReady = true
        }

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
                changeLockScreenOrientation()
            }

            binding.iconRotation.setOnClickListener {
                if (isLocked) {
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

        fadeRunnableTop = binding.topLayout.fadeInAndOut(3000L)
        fadeRunnableBottom = binding.bottomLayout.fadeInAndOut(3000L)
    }


    @OptIn(UnstableApi::class)
    private fun initializePlayer(encryptedFilePath: String) {
        hasInitialized = true
        val isEncrypted = CryptoConstants.isFileEncryptedByUs(File(encryptedFilePath))
        println("initializePlayer: $encryptedFilePath and $isEncrypted")
        println("isFragmented: $isFragmented")
        now1 = System.currentTimeMillis()
        try {
            if (isWhatsApp) {
                playWhatsAppVideo()
            } else {
                lifecycleScope.launch(Dispatchers.IO) {
                    // Step 0: Decrypt if needed
                    decryptedFile = CryptoConstants.decryptMediaHeader(
                        File(encryptedFilePath),
                        this@PlayerActivity
                    )

                    val fragmentFiles = listOf(decryptedFile!!)


                    val result = historyViewModel.getSegmentedVideo(videoNameStr)
                    Log.d(TAG, "initializePlayer 123: $videoNameStr $result")
                    if (result.isNotEmpty() && result.first().totalSegments < 200) {
                        initializePlayer2()
                    } else {

                        val mediaItems = withContext(Dispatchers.IO) {
                            fragmentFiles.map { fragmentFile ->
                                MediaItem.fromUri(Uri.fromFile(fragmentFile))
                            }
                        }

                        val renderersFactory = withContext(Dispatchers.IO) {
                            DefaultRenderersFactory(this@PlayerActivity)
                                .setEnableDecoderFallback(true)
                                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                        }

                        withContext(Dispatchers.Main) {

                            binding.apply {
                                val totalTime = System.currentTimeMillis() - now1
                                Log.d(TAG, "Remux completed in 2 ${totalTime / 1000.0}s")

                                player = ExoPlayer.Builder(this@PlayerActivity, renderersFactory)
                                    .build()
                                    .also { exoPlayer ->
                                        playerView.player = exoPlayer
                                        exoPlayer.volume = 1f

                                        exoPlayer.setMediaItems(mediaItems)
                                        exoPlayer.repeatMode = Player.REPEAT_MODE_OFF

                                        exoPlayer.addListener(object : Player.Listener {
                                            override fun onPlaybackStateChanged(playbackState: Int) {
                                                if (playbackState == Player.STATE_READY) {
                                                    playingSeekbar.max = exoPlayer.duration.toInt()
                                                    binding.totalTime.text =
                                                        formatTime(exoPlayer.duration)
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
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "Failed to decrypt or play file: ${e.message}")
        }
    }

    private fun playWhatsAppVideo() {
        binding.apply {
            // Initialize ExoPlayer
            binding.loadingText.visibility = View.GONE
            player = ExoPlayer.Builder(this@PlayerActivity).build().also { exoPlayer ->
                playerView.player = exoPlayer
                exoPlayer.volume = 1f
//          iconVolume.setImageResource(R.drawable.icon_volume_on)

                exoPlayer.repeatMode = Player.REPEAT_MODE_OFF

                exoPlayer.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY && exoPlayer.duration != C.TIME_UNSET) {
                            playingSeekbar.max = exoPlayer.duration.toInt()
                            totalTime.text = formatTime(exoPlayer.duration)
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
            }

            playerView.player = player

            // ✅ Get proper URI for ExoPlayer
            val uri: Uri? = try {
                if (videoUrl.startsWith("content://")) {
                    val treeUri = videoUrl.toUri()
                    val docId = DocumentsContract.getDocumentId(treeUri)
                    DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                } else {
                    Uri.fromFile(File(videoUrl))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

            if (uri == null) {
                Toast.makeText(this@PlayerActivity, "Invalid video URI", Toast.LENGTH_SHORT).show()
                return
            }

            // Set media item and start playback
            val mediaItem = MediaItem.fromUri(uri)
            player.apply {
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = playWhenReadyState
                seekTo(startPositionValue)
                repeatMode = Player.REPEAT_MODE_ONE
            }

            handler.post(updateSeekBarRunnable)
        }
    }


    private fun initializePlayer2() {
        try {
            lifecycleScope.launch {
                // Step 1: Decide file to play based on `isFragmented`
                val fileToPlay: File = if (isFragmented) {

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
//                        iconVolume.setImageResource(R.drawable.icon_volume_on)

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
    /** Remux fragmented MP4 into a normal MP4 using MediaMuxer */
    suspend fun remuxWithMediaMuxer(inputFile: File, outputFile: File) =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            val TAG = "Remuxer"

            val videoExtractor = MediaExtractor()
            val audioExtractor = MediaExtractor()
            videoExtractor.setDataSource(inputFile.absolutePath)
            audioExtractor.setDataSource(inputFile.absolutePath)

            val muxer =
                MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var videoTrackIndex = -1
            var audioTrackIndex = -1

            //1️⃣ Add tracks
            for (i in 0 until videoExtractor.trackCount) {
                val format = videoExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                when {
                    mime.startsWith("video/") -> {
                        videoExtractor.selectTrack(i)
                        videoTrackIndex = muxer.addTrack(format)
                    }

                    mime.startsWith("audio/") -> {
                        audioExtractor.selectTrack(i)
                        audioTrackIndex = muxer.addTrack(format)
                    }
                }
            }

            muxer.start()

            // 2️⃣ Local suspend function for copying track
            suspend fun copyTrack(extractor: MediaExtractor, muxTrack: Int, label: String) {
                val buffer = ByteBuffer.allocateDirect(2 * 1024 * 1024) // 2MB buffer
                val info = MediaCodec.BufferInfo()
                var totalBytes = 0L

                while (true) {
                    buffer.clear()
                    val size = extractor.readSampleData(buffer, 0)
                    if (size < 0) break
                    info.set(0, size, extractor.sampleTime, extractor.sampleFlags)
                    muxer.writeSampleData(muxTrack, buffer, info)
                    extractor.advance()
                    totalBytes += size
                }

                Log.d(TAG, "✅ $label track done: ${totalBytes / 1024 / 1024} MB")
            }

            // 3️⃣ Parallel processing in proper coroutine scope
            coroutineScope {
                val jobs = mutableListOf<Deferred<Unit>>()
                if (videoTrackIndex >= 0) jobs.add(async {
                    copyTrack(
                        videoExtractor,
                        videoTrackIndex,
                        "Video"
                    )
                })
                if (audioTrackIndex >= 0) jobs.add(async {
                    copyTrack(
                        audioExtractor,
                        audioTrackIndex,
                        "Audio"
                    )
                })
                jobs.awaitAll() // wait for all tracks to finish
            }

            // 4️⃣ Cleanup
            try {
                muxer.stop()
                muxer.release()
            } catch (e: Exception) {
                Log.e(TAG, "muxer stop/release failed", e)
            }
            videoExtractor.release()
            audioExtractor.release()

            val totalTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "Remux completed in ${totalTime / 1000.0}s")
        }


    override fun onStop() {
        super.onStop()
        player.pause()
        handler.removeCallbacks(updateSeekBarRunnable)
    }

    override fun onPause() {
        super.onPause()
        player.pause()
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
        val result = String.format("%02d:%02d", minutes, seconds)
        return result
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