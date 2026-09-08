package com.whispercpp.java.whisper

class WhisperLib {

    companion object {
        init {
            System.loadLibrary("whisper")
        }
    }

    external fun initContext(
        modelPath: String
    ): Long

    external fun freeContext(
        contextPtr: Long
    )

    external fun fullTranscribe(
        contextPtr: Long,
        numThreads: Int,
        audioData: FloatArray
    )

    external fun getTextSegmentCount(
        contextPtr: Long
    ): Int

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

    external fun getTextTokenCount(
        contextPtr: Long,
        segmentIndex: Int
    ): Int

    external fun getTextToken(
        contextPtr: Long,
        segmentIndex: Int,
        tokenIndex: Int
    ): String

    external fun getTextTokenT0(
        contextPtr: Long,
        segmentIndex: Int,
        tokenIndex: Int
    ): Long

    external fun getTextTokenT1(
        contextPtr: Long,
        segmentIndex: Int,
        tokenIndex: Int
    ): Long

    external fun getTextTokenProbability(
        contextPtr: Long,
        segmentIndex: Int,
        tokenIndex: Int
    ): Float

    external fun getSystemInfo(): String
}
