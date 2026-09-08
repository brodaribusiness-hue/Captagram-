package com.brodari.captagram

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.brodari.captagram.databinding.ActivityMainBinding
import com.whispercpp.java.whisper.WhisperSegment
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var whisperManager: WhisperManager
    private lateinit var whisperModelManager: WhisperModelManager

    private var player: ExoPlayer? = null
    private var selectedVideoUri: Uri? = null
    private var transcriptionResult: List<WhisperSegment> = emptyList()
    private var currentCaptionText = ""

    private val transcriptionExecutor: ExecutorService =
        Executors.newSingleThreadExecutor()

    private val captionHandler =
        Handler(Looper.getMainLooper())

    private val captionSyncRunnable =
        object : Runnable {
            override fun run() {
                updateCaptionForCurrentPosition()
                captionHandler.postDelayed(this, 50L)
            }
        }

    private val videoPicker =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let { loadVideo(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        whisperManager = WhisperManager(this)
        whisperModelManager = WhisperModelManager(this)

        binding.selectVideoButton.setOnClickListener {
            videoPicker.launch("video/*")
        }

        binding.generateCaptionsButton.setOnClickListener {
            generateCaptions()
        }

        initializePlayer()
        updateGenerateCaptionsButton()
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
            binding.playerView.player = exoPlayer
        }

        captionHandler.post(captionSyncRunnable)
    }

    private fun loadVideo(uri: Uri) {
        selectedVideoUri = uri
        transcriptionResult = emptyList()
        currentCaptionText = ""

        binding.emptyStateText.visibility = View.GONE
        binding.selectVideoButton.visibility = View.GONE

        binding.statusText.text = "Video selected"
        binding.captionText.text = ""

        player?.apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = false
            seekTo(0L)
        }

        updateGenerateCaptionsButton()
    }

    private fun generateCaptions() {
        val videoUri = selectedVideoUri

        if (videoUri == null) {
            binding.statusText.text = "Select a video first"
            return
        }

        binding.generateCaptionsButton.isEnabled = false
        binding.statusText.text = "Preparing Whisper..."
        binding.captionText.text = ""

        transcriptionExecutor.execute {
            try {
                val modelFile =
                    whisperModelManager.ensureModelAvailable { downloadedBytes, totalBytes ->
                        runOnUiThread {
                            val statusText =
                                if (totalBytes > 0L) {
                                    val percent =
                                        (downloadedBytes * 100L / totalBytes)
                                            .coerceIn(0L, 100L)

                                    "Downloading Whisper model... $percent%"
                                } else {
                                    "Downloading Whisper model..."
                                }

                            binding.statusText.text = statusText
                        }
                    }

                runOnUiThread {
                    binding.statusText.text = "Generating captions..."
                }

                val segments =
                    whisperManager.transcribeVideo(
                        videoUri = videoUri
                    )

                transcriptionResult = segments

                runOnUiThread {
                    binding.statusText.text =
                        if (segments.isEmpty()) {
                            "No speech detected"
                        } else {
                            "Captions generated"
                        }

                    updateCaptionForCurrentPosition()
                    updateGenerateCaptionsButton()
                }
            } catch (error: Exception) {
                runOnUiThread {
                    binding.statusText.text =
                        error.message ?: "Caption generation failed"

                    binding.captionText.text = ""
                    updateGenerateCaptionsButton()
                }
            }
        }
    }

    private fun updateCaptionForCurrentPosition() {
        val positionMs =
            player?.currentPosition ?: return

        val segment =
            findCurrentSegment(positionMs)

        if (segment == null) {
            if (currentCaptionText.isNotEmpty()) {
                currentCaptionText = ""
                binding.captionText.text = ""
            }

            return
        }

        val captionText =
            segment.text.trim()

        if (captionText != currentCaptionText) {
            currentCaptionText = captionText
            binding.captionText.text = captionText
        }
    }

    private fun findCurrentSegment(
        positionMs: Long
    ): WhisperSegment? {
        return transcriptionResult.firstOrNull { segment ->
            positionMs >= segment.startTimeMs &&
                positionMs < segment.endTimeMs
        }
    }

    private fun updateGenerateCaptionsButton() {
        binding.generateCaptionsButton.isEnabled =
            selectedVideoUri != null
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        captionHandler.removeCallbacks(captionSyncRunnable)

        binding.playerView.player = null

        player?.release()
        player = null

        transcriptionExecutor.shutdownNow()

        super.onDestroy()
    }
}
