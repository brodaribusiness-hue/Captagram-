package com.whispercpp.java.whisper

data class WhisperSegment(
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long
)
