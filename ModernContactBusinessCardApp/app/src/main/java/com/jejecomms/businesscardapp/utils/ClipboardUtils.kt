package com.jejecomms.businesscardapp.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.google.android.material.textfield.TextInputEditText
import com.jejecomms.businesscardapp.R

/**
 * Clipboard Utils class.
 */
object ClipboardUtils {

    /**
     * Helper function to copy text from a TextInputEditText to the clipboard.
     * @param editText The TextInputEditText from which to copy text.
     * @param label A descriptive label for the copied content (e.g., "Phone Number", "Email").
     */
    fun copyTextToClipboard(context: Context, editText: TextInputEditText, label: String) {
        val textToCopy = editText.text?.toString()

        if (!textToCopy.isNullOrEmpty()) {
            val clipboardManager =
                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText(label, textToCopy)
            clipboardManager.setPrimaryClip(clipData)
            ToastUtils.showLongMessage(
                "$label " + context.getString(R.string.copied_to_clipboard),
                context)
        } else {
            ToastUtils.showLongMessage(
                context.getString(R.string.no) + " $label "
                        + context.getString(R.string.to_copy), context)
        }
    }
}