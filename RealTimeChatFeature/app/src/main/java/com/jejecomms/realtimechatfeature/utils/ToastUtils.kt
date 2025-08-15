package com.jejecomms.realtimechatfeature.utils

import android.content.Context
import android.view.Gravity
import android.widget.Toast

/**
 * A singleton object to centralize toast-related logic.
 */
object ToastUtils {

    // A simple function to show a short toast message.
    fun showShortToast(context: Context, message: String) {
        showToast(context, message, Toast.LENGTH_SHORT)
    }

    // A simple function to show a long toast message.
    fun showLongToast(context: Context, message: String) {
        showToast(context, message, Toast.LENGTH_LONG)
    }

    /**
     * A more flexible function to show a toast with custom duration.
     * @param context The application context to prevent memory leaks.
     * @param message The message to be displayed.
     * @param duration The duration of the toast (Toast.LENGTH_SHORT or Toast.LENGTH_LONG).
     */
    fun showToast(context: Context, message: String, duration: Int) {
        Toast.makeText(context.applicationContext, message, duration).show()
    }

    /**
     * A highly customizable function to show a toast with custom gravity,
     * duration, and optional icon.
     *
     * @param context The application context.
     * @param message The message to be displayed.
     * @param duration The duration.
     * @param gravity The gravity for the toast's position (e.g., Gravity.TOP, Gravity.CENTER).
     * @param xOffset The horizontal offset.
     * @param yOffset The vertical offset.
     */
    fun showCustomToast(
        context: Context,
        message: String,
        duration: Int = Toast.LENGTH_SHORT,
        gravity: Int = 0,
        xOffset: Int = 0,
        yOffset: Int = 0,
    ) {
        val toast = Toast.makeText(context.applicationContext, message, duration)
        toast.setGravity(gravity, xOffset, yOffset)
        toast.show()
    }
}