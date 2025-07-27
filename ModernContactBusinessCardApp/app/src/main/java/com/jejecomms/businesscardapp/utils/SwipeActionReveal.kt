package com.jejecomms.businesscardapp.utils

import android.graphics.Canvas
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.jejecomms.businesscardapp.ui.contacts.ContactAdapter

/**
 * A custom ItemTouchHelper.SimpleCallback for implementing swipe-to-reveal actions
 * with a front-view/back-view pattern in RecyclerView items.
 * This version is configured for left-side swipes only.
 */
class SwipeActionReveal(
    private var swipeCallbackLeft: SwipeCallbackLeft? = null
) : ItemTouchHelper.SimpleCallback(
    0, // No drag directions allowed
    ItemTouchHelper.LEFT // Only left swipe allowed
) {
    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        // Drag and drop is not supported
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, position: Int) {
        // This method is typically called when an item is fully dismissed.
        // For a "reveal" action, we handle the snap-back/snap-open in clearView,
        // and actions are triggered by clicks on the revealed buttons, not by the swipe itself.
        // Therefore, no action is taken here.
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val contactViewHolder: ContactAdapter.ContactViewHolder =
            viewHolder as ContactAdapter.ContactViewHolder
        val backView = contactViewHolder.backView
        val frontView = contactViewHolder.frontView

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            if (frontView.isVisible) {
                frontView.translationX = dX
                frontView.visibility = View.INVISIBLE
                backView.visibility = View.VISIBLE
                backView.isFocusable = true
                backView.isClickable = true
            }

            if (dX < 0) {
                backView.visibility = View.VISIBLE
                backView.isFocusable = true
                backView.isClickable = true

                getDefaultUIUtil().onDraw(
                    c,
                    recyclerView,
                    frontView,
                    dX/4,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        (viewHolder as ContactAdapter.ContactViewHolder).frontView.let {
            getDefaultUIUtil().clearView(it)
        }
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        return 1f
    }

    override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
        return defaultValue * 1.5f
    }

    override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        return ItemTouchHelper.LEFT
    }

    /**
     * Swipe callback for left-side swipes.
     */
    interface SwipeCallbackLeft {
        fun onLeftSwipe(position: Int)
    }
}