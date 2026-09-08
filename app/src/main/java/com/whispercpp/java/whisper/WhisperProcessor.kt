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

            val segmentCount = whisperLib.getTextSegmentCount(context)
            val segments = ArrayList<WhisperSegment>(segmentCount)

            for (segmentIndex in 0 until segmentCount) {

                val segmentStart =
                    whisperLib.getTextSegmentT0(context, segmentIndex) * 10

                val segmentEnd =
                    whisperLib.getTextSegmentT1(context, segmentIndex) * 10

                val words = buildWords(
                    context,
                    segmentIndex
                )

                segments.add(
                    WhisperSegment(
                        text = whisperLib.getTextSegment(
                            context,
                            segmentIndex
                        ),
                        startTimeMs = segmentStart,
                        endTimeMs = segmentEnd,
                        words = words
                    )
                )
            }

            segments

        } finally {
            whisperLib.freeContext(context)
        }
    }

    private fun buildWords(
        context: Long,
        segmentIndex: Int
    ): List<WhisperWord> {

        val tokenCount =
            whisperLib.getTextTokenCount(
                context,
                segmentIndex
            )

        if (tokenCount <= 0) {
            return emptyList()
        }

        val words = ArrayList<WhisperWord>()

        var currentText = StringBuilder()
        var currentStart = 0L
        var currentEnd = 0L
        var currentProbability = 0f
        var probabilityCount = 0

        fun finishWord() {

            val text = currentText
                .toString()
                .trim()

            if (text.isNotEmpty() && currentEnd >= currentStart) {

                val probability =
                    if (probabilityCount > 0) {
                        currentProbability / probabilityCount
                    } else {
                        0f
                    }

                words.add(
                    WhisperWord(
                        text = text,
                        startTimeMs = currentStart,
                        endTimeMs = currentEnd,
                        probability = probability
                    )
                )
            }

            currentText = StringBuilder()
            currentStart = 0L
            currentEnd = 0L
            currentProbability = 0f
            probabilityCount = 0
        }

        for (tokenIndex in 0 until tokenCount) {

            val tokenText =
                whisperLib.getTextToken(
                    context,
                    segmentIndex,
                    tokenIndex
                )

            if (tokenText.isBlank()) {
                continue
            }

            val tokenStart =
                whisperLib.getTextTokenT0(
                    context,
                    segmentIndex,
                    tokenIndex
                ) * 10

            val tokenEnd =
                whisperLib.getTextTokenT1(
                    context,
                    segmentIndex,
                    tokenIndex
                ) * 10

            val probability =
                whisperLib.getTextTokenProbability(
                    context,
                    segmentIndex,
                    tokenIndex
                )

            val startsNewWord =
                currentText.isNotEmpty() &&
                    tokenText.any { it.isWhitespace() }

            if (startsNewWord) {
                finishWord()
            }

            if (currentText.isEmpty()) {
                currentStart = tokenStart
            }

            currentText.append(tokenText)
            currentEnd = tokenEnd
            currentProbability += probability
            probabilityCount++
        }

        finishWord()

        return words
    }
}
