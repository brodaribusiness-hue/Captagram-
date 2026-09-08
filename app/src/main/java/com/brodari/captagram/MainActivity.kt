private fun updateCaptionForCurrentPosition() {
    val positionMs =
        player?.currentPosition ?: return

    val segment =
        findCurrentSegment(positionMs)

    val newCaptionText =
        segment?.text?.trim() ?: ""

    if (newCaptionText != currentCaptionText) {
        currentCaptionText = newCaptionText
        binding.captionText.text = newCaptionText
    }
}

private fun findCurrentSegment(
    positionMs: Long
): WhisperSegment? {
    return transcriptionResult.firstOrNull { segment ->
        positionMs >= segment.startTimeMs &&
            positionMs < segment.endTimeMs
    }
}
