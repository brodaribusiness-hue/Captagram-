package com.brodari.captagram

import android.content.Context
import com.whispercpp.java.whisper.WhisperProcessor
import com.whispercpp.java.whisper.WhisperSegment
import java.io.File

class WhisperManager(
    private val context: Context
) {

    private val processor = WhisperProcessor()

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
}
