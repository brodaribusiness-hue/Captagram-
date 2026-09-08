package com.brodari.captagram

data class CaptionEditorState(
    val captions: List<Caption> = emptyList(),
    val selectedCaptionId: Long? = null,
    val selectedStyleId: String? = null,
    val isEditingText: Boolean = false
) {

    val selectedCaption: Caption?
        get() = captions.firstOrNull {
            it.id == selectedCaptionId
        }

    fun updateCaption(updatedCaption: Caption): CaptionEditorState {
        return copy(
            captions = captions.map { caption ->
                if (caption.id == updatedCaption.id) {
                    updatedCaption
                } else {
                    caption
                }
            }
        )
    }

    fun addCaption(caption: Caption): CaptionEditorState {
        return copy(
            captions = captions + caption,
            selectedCaptionId = caption.id,
            selectedStyleId = caption.style.id
        )
    }

    fun removeCaption(captionId: Long): CaptionEditorState {
        val updatedCaptions = captions.filterNot {
            it.id == captionId
        }

        val newSelectedId =
            if (selectedCaptionId == captionId) {
                updatedCaptions.firstOrNull()?.id
            } else {
                selectedCaptionId
            }

        return copy(
            captions = updatedCaptions,
            selectedCaptionId = newSelectedId
        )
    }

    fun selectCaption(captionId: Long): CaptionEditorState {
        val caption = captions.firstOrNull {
            it.id == captionId
        }

        return copy(
            selectedCaptionId = caption?.id,
            selectedStyleId = caption?.style?.id
        )
    }

    fun updateCaptionText(
        captionId: Long,
        text: String
    ): CaptionEditorState {
        return updateCaptionById(captionId) { caption ->
            caption.copy(text = text)
        }
    }

    fun updateCaptionTiming(
        captionId: Long,
        startTimeMs: Long,
        endTimeMs: Long
    ): CaptionEditorState {
        require(startTimeMs >= 0L)
        require(endTimeMs >= startTimeMs)

        return updateCaptionById(captionId) { caption ->
            caption.copy(
                startTimeMs = startTimeMs,
                endTimeMs = endTimeMs
            )
        }
    }

    fun updateCaptionStyle(
        captionId: Long,
        style: CaptionStyle
    ): CaptionEditorState {
        return updateCaptionById(captionId) { caption ->
            caption.copy(style = style)
        }.copy(
            selectedStyleId = style.id
        )
    }

    fun updateCaptionPosition(
        captionId: Long,
        positionX: Float,
        positionY: Float
    ): CaptionEditorState {
        val x = positionX.coerceIn(0f, 1f)
        val y = positionY.coerceIn(0f, 1f)

        return updateCaptionById(captionId) { caption ->
            caption.copy(
                style = caption.style.copy(
                    positionX = x,
                    positionY = y
                )
            )
        }
    }

    fun updateCaptionFontSize(
        captionId: Long,
        fontSizeSp: Float
    ): CaptionEditorState {
        require(fontSizeSp > 0f)

        return updateCaptionById(captionId) { caption ->
            caption.copy(
                style = caption.style.copy(
                    fontSizeSp = fontSizeSp
                )
            )
        }
    }

    private fun updateCaptionById(
        captionId: Long,
        update: (Caption) -> Caption
    ): CaptionEditorState {
        return copy(
            captions = captions.map { caption ->
                if (caption.id == captionId) {
                    update(caption)
                } else {
                    caption
                }
            }
        )
    }
}
