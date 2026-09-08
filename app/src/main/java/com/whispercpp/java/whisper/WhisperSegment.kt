package com.whispercpp.java.whisper

data class WhisperWord(
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val probability: Float
)

data class WhisperSegment(
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val words: List<WhisperWord> = emptyList()
)
