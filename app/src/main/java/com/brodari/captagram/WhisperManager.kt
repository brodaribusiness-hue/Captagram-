package com.brodari.captagram

import android.content.Context
import android.net.Uri
import com.whispercpp.java.whisper.WhisperProcessor
import com.whispercpp.java.whisper.WhisperSegment
import java.io.File

class WhisperManager(
    private val context: Context
) {

    private val processor = WhisperProcessor()
    private val audioExtractor = VideoAudioExtractor(context)

    fun transcribe(
        audioData: FloatArray,
        numThreads: Int = 4
    ): List<WhisperSegment> {

        val modelFile = File(context.filesDir, "ggml-base.bin")

        if (!modelFile.exists()) {
            throw IllegalStateException("Whisper model not found")
        }

        return processor.transcribe(
            modelPath = modelFile.absolutePath,
            audioData = audioData,
            numThreads = numThreads
        )
    }

    fun transcribeVideo(
        videoUri: Uri,
        numThreads: Int = 4
    ): List<WhisperSegment> {

        val audioData = audioExtractor.extract(videoUri)

        if (audioData.isEmpty()) {
            throw IllegalStateException("Unable to extract audio from video")
        }

        return transcribe(
            audioData = audioData,
            numThreads = numThreads
        )
    }
}
