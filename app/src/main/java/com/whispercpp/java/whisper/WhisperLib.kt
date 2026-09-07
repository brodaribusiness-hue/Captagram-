package com.whispercpp.java.whisper

class WhisperLib {

    companion object {
        init {
            System.loadLibrary("whisper")
        }
    }

    external fun initContextFromInputStream(inputStream: java.io.InputStream): Long

    external fun initContextFromAsset(
        assetManager: android.content.res.AssetManager,
        assetPath: String
    ): Long

    external fun initContext(modelPath: String): Long

    external fun freeContext(contextPtr: Long)

    external fun fullTranscribe(
        contextPtr: Long,
        numThreads: Int,
        audioData: FloatArray
    )

    external fun getTextSegmentCount(contextPtr: Long): Int

    external fun getTextSegment(
        contextPtr: Long,
        index: Int
    ): String

    external fun getTextSegmentT0(
        contextPtr: Long,
        index: Int
    ): Long

    external fun getTextSegmentT1(
        contextPtr: Long,
        index: Int
    ): Long

    external fun getSystemInfo(): String

    external fun benchMemcpy(numThreads: Int): String

    external fun benchGgmlMulMat(numThreads: Int): String
}
