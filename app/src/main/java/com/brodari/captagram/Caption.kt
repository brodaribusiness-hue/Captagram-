package com.brodari.captagram

data class Caption(
    val id: Long,
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val style: CaptionStyle,
    val words: List<CaptionWord> = emptyList()
)

data class CaptionWord(
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long
)
