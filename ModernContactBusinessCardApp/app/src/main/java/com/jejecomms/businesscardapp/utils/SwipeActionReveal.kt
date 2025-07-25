package com.jejecomms.businesscardapp.utils

import android.graphics.Canvas
import android.util.Log
import android.view.View
import androidx.core.view.isGone
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.jejecomms.businesscardapp.ui.contacts.ContactAdapter
import kotlin.math.abs

/**
 * A custom ItemTouchHelper.SimpleCallback for implementing swipe-to-reveal actions
 * with a front-view/back-view pattern in RecyclerView items.
 * This version is configured for left-side swipes only, with a snap-to-half-open behavior.
 */
class SwipeActionReveal(
    private var swipeCallbackLeft: SwipeCallbackLeft? = null
) : ItemTouchHelper.SimpleCallback(
    0, // No drag directions allowed
    ItemTouchHelper.LEFT // Only left swipe allowed
) {
    // To keep track of the currently "fully open" view holder for click handling
    private var currentSwipedViewHolder: ContactAdapter.ContactViewHolder? = null

    companion object {
        private const val ANIMATION_DURATION = 200 // Milliseconds
        private const val SNAP_OPEN_THRESHOLD_PERCENT = 0.20f // 20% of back view width
        private const val HALF_REVEAL_WIDTH_PERCENT = 0.3f // 30% of back view width
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        // Drag and drop is not supported
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // This method is typically called when an item is fully dismissed.
        // For a "reveal" action, we handle the snap-back/snap-open in clearView,
        // and actions are triggered by clicks on the revealed buttons, not by the swipe itself.
        // Therefore, no action is taken here.
    }

    /**
     * Animates the given ViewHolder's frontView to the closed position and hides the backView.
     */
    private fun closeItem(viewHolder: ContactAdapter.ContactViewHolder) {
        val frontView = viewHolder.frontView
        val backView = viewHolder.backView

        frontView.animate()
            .translationX(0f) // Target position: fully closed
            .setDuration(ANIMATION_DURATION.toLong())
            .withEndAction {
                backView.visibility = View.GONE
                backView.alpha = 0f
                if (currentSwipedViewHolder == viewHolder) {
                    currentSwipedViewHolder = null
                }
            }
            .start()
    }

    /**
     * This is crucial for controlling the visual swipe and revealing the back view.
     */
    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            val contactViewHolder = viewHolder as? ContactAdapter.ContactViewHolder ?: return

            val frontView = contactViewHolder.frontView
            val backView = contactViewHolder.backView

            // Ensure backView is measured and visible for proper revelation
            // If backView.width is 0, nothing will be revealed.
            // This can happen if it's GONE and not measured yet.
            if (backView.width == 0 && backView.isGone) {
                backView.visibility = View.INVISIBLE // Make it invisible to force measurement
                backView.requestLayout() // Request a layout pass
                // We might need to wait for a layout pass here if it's the very first swipe
                // but for subsequent swipes, it should have a width.
            }

            val backViewWidth = backView.width.toFloat()

            // Only make back view visible if swiping left and it's not already visible
            if (backView.visibility != View.VISIBLE && dX < 0) {
                backView.visibility = View.VISIBLE
                backView.alpha = 0f // Start transparent
            }

            // Clamp dX so the front view doesn't go further than the back view's width
            val clampedDx = dX.coerceIn(-backViewWidth, 0f)
            frontView.translationX = clampedDx

            // Fade in the back view as it's revealed
            if (backViewWidth > 0) {
                backView.alpha = abs(clampedDx) / backViewWidth
            }

            Log.e("TAG: ","isCurrentlyActive: "+isCurrentlyActive+" currentSwipedViewHolder:" +
                    currentSwipedViewHolder+" contactViewHolder: "+contactViewHolder)
            // If a new item is being actively swiped, close any previously open item
            if (isCurrentlyActive && currentSwipedViewHolder != null
                && currentSwipedViewHolder != contactViewHolder) {
                closeItem(currentSwipedViewHolder!!)
                currentSwipedViewHolder = null
            }

        } else {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }

    /**
     * Called when the user interaction with an element is Fling or Swipe, and it is completed.
     * This is where you handle the "snap" back to closed or "snap" open to half-revealed behavior.
     */
    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        val contactViewHolder = viewHolder as? ContactAdapter.ContactViewHolder
        contactViewHolder?.let {
            val frontView = it.frontView
            val backView = it.backView
            val backViewWidth = backView.width.toFloat()

            val snapOpenThreshold = backViewWidth * SNAP_OPEN_THRESHOLD_PERCENT
            val halfRevealWidth = backViewWidth * HALF_REVEAL_WIDTH_PERCENT

            val currentTranslationX = frontView.translationX
            val targetTranslationX: Float

            // Determine the target translation based on the current swipe distance
            targetTranslationX = if (abs(currentTranslationX) >= snapOpenThreshold) {
                -halfRevealWidth
            } else {
                0f
            }

            // Animate the front view to the target position
            frontView.animate()
                .translationX(targetTranslationX)
                .setDuration(ANIMATION_DURATION.toLong())
                .withEndAction {
                    if (targetTranslationX == 0f) {
                        // Item snapped closed
                        backView.visibility = View.GONE
                        backView.alpha = 0f
                        if (currentSwipedViewHolder == contactViewHolder) {
                            currentSwipedViewHolder = null
                        }
                    } else {
                        // Item snapped open
                        backView.visibility = View.VISIBLE
                        backView.alpha = 1f
                        // Close any other item that might have been left open
                        if (currentSwipedViewHolder != null && currentSwipedViewHolder != contactViewHolder) {
                            closeItem(currentSwipedViewHolder!!)
                        }
                        currentSwipedViewHolder = contactViewHolder
                    }
                }
                .start()
        }
        // Do NOT call super.clearView(recyclerView, viewHolder) here as we are manually animating.
    }

    /**
     * This controls how much the item needs to be swiped to "snap" into its final position.
     * For reveal, this affects the internal state of ItemTouchHelper, but the actual snapping
     * logic is handled in `clearView`.
     */
    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        return 0.5f // Default threshold, often doesn't need fine-tuning for reveal
    }

    /**
     * Determines how far the swipe needs to go (speed-wise) to be considered a full "swipe" for action.
     * For reveal, you want it to be easy to open.
     */
    override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
        return defaultValue * 1.5f // Make it easier to trigger a snap
    }

    /**
     * Determines the allowed swipe directions for the item.
     * Configured for left-swipe only.
     */
    override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        return ItemTouchHelper.LEFT
    }

    /**
     * Swipe callback for left-side swipes.
     * This interface is typically used if a *full dismissal* swipe triggers an action.
     * For a "reveal" pattern, actions are usually triggered by clicking buttons on the revealed view.
     * If this is only for full dismissal, and you're not using full dismissal, this can be removed.
     */
    interface SwipeCallbackLeft {
        fun onLeftSwipe(position: Int)
    }
}