package com.jejecomms.realtimechatfeature.utils

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/***
 * Date Utils class.
 */
object DateUtils {

    /**
     * Formats a timestamp into a readable time string.
     */
    fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * Formats a timestamp into a readable date string.
     */
    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * Checks if two timestamps represent the same day.
     */
    fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Formats a duration in milliseconds into a readable time string for audio files.
     */
    @SuppressLint("DefaultLocale")
    fun formatTime(milliseconds: Int): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds.toLong())
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds.toLong()) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    /**
     * Returns the current timestamp in milliseconds.
     */
    fun getTimestamp(): Long {
        return System.currentTimeMillis()
    }
}