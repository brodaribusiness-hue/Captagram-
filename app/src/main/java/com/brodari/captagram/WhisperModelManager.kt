package com.brodari.captagram

import android.content.Context
import android.net.Uri
import java.io.File

class WhisperModelManager(
    private val context: Context
) {

    companion object {
        private const val MODEL_FILE_NAME = "ggml-base.bin"
    }

    fun saveModel(uri: Uri): File {
        val modelFile = File(context.filesDir, MODEL_FILE_NAME)

        context.contentResolver.openInputStream(uri)?.use { input ->
            modelFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Unable to open Whisper model")

        return modelFile
    }

    fun getModelFile(): File {
        return File(context.filesDir, MODEL_FILE_NAME)
    }

    fun isModelAvailable(): Boolean {
        return getModelFile().exists() && getModelFile().length() > 0
    }
}
