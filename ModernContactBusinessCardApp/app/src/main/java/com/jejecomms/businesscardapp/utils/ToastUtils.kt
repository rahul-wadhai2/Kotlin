package com.jejecomms.businesscardapp.utils

import android.content.Context
import android.widget.Toast

/**
 * Toast utils class.
 */
object ToastUtils {
    /**
     * Show long message.
     *
     * @param message message to show.
     * @param context context of activity.
     */
    fun showLongMessage(message: String?, context: Context?) {
        getToast(message, context).show()
    }

    /**
     * Get toast.
     *
     *
     * @param message message to show.
     * @param context context of activity.
     * @return toast instance.
     */
    private fun getToast(message: String?, context: Context?): Toast {
        return Toast.makeText(context, message, Toast.LENGTH_LONG)
    }
}