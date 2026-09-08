package com.brodari.captagram

import android.content.Context
import java.io.BufferedInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class WhisperModelManager(
    private val context: Context
) {

    companion object {
        private const val MODEL_FILE_NAME = "ggml-base.bin"

        private const val MODEL_URL =
            "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin"

        private const val BUFFER_SIZE = 64 * 1024
    }

    fun ensureModelAvailable(
        onProgress: ((downloadedBytes: Long, totalBytes: Long) -> Unit)? = null
    ): File {
        val modelFile = getModelFile()

        if (modelFile.exists() && modelFile.length() > 0L) {
            return modelFile
        }

        downloadModel(
            destination = modelFile,
            onProgress = onProgress
        )

        if (!modelFile.exists() || modelFile.length() <= 0L) {
            throw IllegalStateException("Whisper model download failed")
        }

        return modelFile
    }

    fun getModelFile(): File {
        return File(
            context.filesDir,
            MODEL_FILE_NAME
        )
    }

    fun isModelAvailable(): Boolean {
        val modelFile = getModelFile()

        return modelFile.exists() &&
            modelFile.length() > 0L
    }

    private fun downloadModel(
        destination: File,
        onProgress: ((downloadedBytes: Long, totalBytes: Long) -> Unit)?
    ) {
        val temporaryFile =
            File(
                context.filesDir,
                "$MODEL_FILE_NAME.download"
            )

        if (temporaryFile.exists()) {
            temporaryFile.delete()
        }

        val connection =
            (URL(MODEL_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 30_000
                readTimeout = 60_000
                useCaches = false
                doInput = true
            }

        try {
            val responseCode = connection.responseCode

            if (responseCode !in 200..299) {
                throw IllegalStateException(
                    "Whisper model download failed: HTTP $responseCode"
                )
            }

            val totalBytes =
                connection.contentLengthLong

            var downloadedBytes = 0L

            BufferedInputStream(
                connection.inputStream,
                BUFFER_SIZE
            ).use { input ->

                temporaryFile.outputStream(
                    BUFFER_SIZE
                ).use { output ->

                    val buffer =
                        ByteArray(BUFFER_SIZE)

                    while (true) {
                        val bytesRead =
                            input.read(buffer)

                        if (bytesRead == -1) {
                            break
                        }

                        output.write(
                            buffer,
                            0,
                            bytesRead
                        )

                        downloadedBytes += bytesRead

                        onProgress?.invoke(
                            downloadedBytes,
                            totalBytes
                        )
                    }

                    output.flush()
                }
            }

            if (!temporaryFile.exists() ||
                temporaryFile.length() <= 0L
            ) {
                throw IllegalStateException(
                    "Downloaded Whisper model is empty"
                )
            }

            if (destination.exists()) {
                destination.delete()
            }

            if (!temporaryFile.renameTo(destination)) {
                temporaryFile.copyTo(
                    destination,
                    overwrite = true
                )

                temporaryFile.delete()
            }
        } finally {
            connection.disconnect()

            if (temporaryFile.exists() &&
                destination.length() > 0L
            ) {
                temporaryFile.delete()
            }
        }
    }
}
