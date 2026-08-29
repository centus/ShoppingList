package com.org.shoppinglist.ui

import androidx.recyclerview.widget.DefaultItemAnimator

/**
 * Animates item/section removals and moves (check-off) without the flicker from change animations.
 */
class ShoppingListItemAnimator : DefaultItemAnimator() {
    init {
        supportsChangeAnimations = false
        removeDuration = REMOVE_DURATION_MS
        moveDuration = MOVE_DURATION_MS
        addDuration = ADD_DURATION_MS
        changeDuration = 0
    }

    companion object {
        const val REMOVE_DURATION_MS = 220L
        const val MOVE_DURATION_MS = 220L
        const val ADD_DURATION_MS = 150L
    }
}
