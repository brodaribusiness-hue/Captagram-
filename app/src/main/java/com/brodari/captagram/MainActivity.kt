package com.brodari.captagram

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.brodari.captagram.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var whisperManager: WhisperManager
    private lateinit var whisperModelManager: WhisperModelManager

    private var player: ExoPlayer? = null
    private var selectedVideoUri: Uri? = null

    private val transcriptionExecutor: ExecutorService =
        Executors.newSingleThreadExecutor()

    private val videoPicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { loadVideo(it) }
        }

    private val modelPicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { saveWhisperModel(it) }
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

        binding.selectModelButton.setOnClickListener {
            modelPicker.launch("*/*")
        }

        binding.transcribeButton.setOnClickListener {
            transcribeSelectedVideo()
        }

        initializePlayer()
        updateTranscribeButton()
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
            binding.playerView.player = exoPlayer
        }
    }

    private fun loadVideo(uri: Uri) {
        selectedVideoUri = uri

        binding.emptyStateText.visibility = View.GONE
        binding.selectVideoButton.visibility = View.GONE

        binding.statusText.text = "Video selected"
        binding.captionText.text = ""

        player?.apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = false
        }

        updateTranscribeButton()
    }

    private fun saveWhisperModel(uri: Uri) {
        binding.statusText.text = "Saving Whisper model..."
        binding.transcribeButton.isEnabled = false

        transcriptionExecutor.execute {
            try {
                whisperModelManager.saveModel(uri)

                runOnUiThread {
                    binding.statusText.text = "Whisper model ready"
                    updateTranscribeButton()
                }

            } catch (error: Exception) {
                runOnUiThread {
                    binding.statusText.text =
                        error.message ?: "Unable to save Whisper model"

                    updateTranscribeButton()
                }
            }
        }
    }

    private fun transcribeSelectedVideo() {
        val videoUri = selectedVideoUri

        if (videoUri == null) {
            binding.statusText.text = "Select a video first"
            return
        }

        if (!whisperModelManager.isModelAvailable()) {
            binding.statusText.text = "Select a Whisper model first"
            return
        }

        binding.transcribeButton.isEnabled = false
        binding.statusText.text = "Transcribing..."
        binding.captionText.text = ""

        transcriptionExecutor.execute {
            try {
                val segments =
                    whisperManager.transcribeVideo(
                        videoUri = videoUri
                    )

                val captionText =
                    segments
                        .flatMap { it.words }
                        .joinToString(" ") { it.text }
                        .trim()

                runOnUiThread {
                    binding.captionText.text = captionText

                    binding.statusText.text =
                        if (segments.isEmpty()) {
                            "No speech detected"
                        } else {
                            "Transcription complete"
                        }

                    updateTranscribeButton()
                }

            } catch (error: Exception) {
                runOnUiThread {
                    binding.statusText.text =
                        error.message ?: "Transcription failed"

                    updateTranscribeButton()
                }
            }
        }
    }

    private fun updateTranscribeButton() {
        binding.transcribeButton.isEnabled =
            selectedVideoUri != null &&
                whisperModelManager.isModelAvailable()
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        binding.playerView.player = null

        player?.release()
        player = null

        transcriptionExecutor.shutdownNow()

        super.onDestroy()
    }
}
