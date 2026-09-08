package com.brodari.captagram

class CaptionUndoRedoManager(
    initialState: CaptionEditorState = CaptionEditorState()
) {

    private val undoStack = ArrayDeque<CaptionEditorState>()
    private val redoStack = ArrayDeque<CaptionEditorState>()

    private var currentState: CaptionEditorState = initialState

    fun getState(): CaptionEditorState {
        return currentState
    }

    fun updateState(newState: CaptionEditorState) {
        if (newState == currentState) {
            return
        }

        undoStack.addLast(currentState)
        currentState = newState
        redoStack.clear()
    }

    fun undo(): CaptionEditorState {
        if (undoStack.isEmpty()) {
            return currentState
        }

        redoStack.addLast(currentState)
        currentState = undoStack.removeLast()

        return currentState
    }

    fun redo(): CaptionEditorState {
        if (redoStack.isEmpty()) {
            return currentState
        }

        undoStack.addLast(currentState)
        currentState = redoStack.removeLast()

        return currentState
    }

    fun canUndo(): Boolean {
        return undoStack.isNotEmpty()
    }

    fun canRedo(): Boolean {
        return redoStack.isNotEmpty()
    }

    fun clearHistory() {
        undoStack.clear()
        redoStack.clear()
    }
}
