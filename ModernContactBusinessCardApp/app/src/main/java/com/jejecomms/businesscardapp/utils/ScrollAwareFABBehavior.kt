package com.jejecomms.businesscardapp.utils

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.core.view.isVisible

/**
 * ScrollAwareFABBehavior is a custom behavior for a FloatingActionButton that shows or hides.
 * Use in XML.
 */
class ScrollAwareFABBehavior(context: Context, attrs: AttributeSet) : FloatingActionButton.Behavior(context, attrs) {

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: FloatingActionButton,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ): Boolean {
        // Ensure we react to vertical scrolling
        return axes == ViewCompat.SCROLL_AXIS_VERTICAL ||
                super.onStartNestedScroll(coordinatorLayout, child, directTargetChild, target, axes, type)
    }

    override fun onNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: FloatingActionButton,
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int,
        consumed: IntArray
    ) {
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, consumed)

        if (dyConsumed > 0 && child.isVisible) {
            // User scrolled down and the FAB is currently visible -> hide it
            child.hide(object : FloatingActionButton.OnVisibilityChangedListener() {
                // Optional: Use this anonymous class if you need to perform an action when
                // the FAB is fully hidden, like setting its visibility to GONE to prevent
                // it from consuming touch events. However, hide() typically handles this.
                override fun onHidden(fab: FloatingActionButton?) {
                    super.onHidden(fab)
                    fab?.visibility = View.INVISIBLE // Or GONE, depending on desired behavior
                }
            })
        } else if (dyConsumed < 0 && child.visibility != View.VISIBLE) {
            // User scrolled up and the FAB is currently hidden -> show it
            child.show()
        }
    }
}