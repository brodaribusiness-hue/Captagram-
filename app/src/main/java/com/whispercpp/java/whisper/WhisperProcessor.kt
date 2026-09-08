package com.whispercpp.java.whisper

class WhisperProcessor(
    private val whisperLib: WhisperLib = WhisperLib()
) {

    fun transcribe(
        modelPath: String,
        audioData: FloatArray,
        numThreads: Int = 4
    ): List<WhisperSegment> {

        val context = whisperLib.initContext(modelPath)

        if (context == 0L) {
            throw IllegalStateException("Failed to initialize Whisper model")
        }

        return try {
            whisperLib.fullTranscribe(
                context,
                numThreads,
                audioData
            )

            val count = whisperLib.getTextSegmentCount(context)
            val segments = ArrayList<WhisperSegment>(count)

            for (index in 0 until count) {
                segments.add(
                    WhisperSegment(
                        text = whisperLib.getTextSegment(context, index),
                        startTimeMs = whisperLib.getTextSegmentT0(context, index) * 10,
                        endTimeMs = whisperLib.getTextSegmentT1(context, index) * 10
                    )
                )
            }

            segments
        } finally {
            whisperLib.freeContext(context)
        }
    }
}
