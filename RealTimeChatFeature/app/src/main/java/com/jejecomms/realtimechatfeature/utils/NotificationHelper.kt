package com.jejecomms.realtimechatfeature.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jejecomms.realtimechatfeature.R
import com.jejecomms.realtimechatfeature.ui.ChatActivity

object NotificationHelper {
    private const val CHANNEL_ID = "com.jejecomms.realtimechatfeature.chat_channel"
    private const val CHANNEL_NAME = "Chat Messages"
    private const val NOTIFICATION_ID = 100

    /**
     * Creates a notification channel for Android O and above.
     * This should be called in your Application class or the onCreate method of your service.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Notifications for new chat messages"
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Builds and shows a chat notification with a deep link to the chat room.
     *
     * @param context The application context.
     * @param roomId The ID of the chat room to link to.
     * @param message The message to display in the notification.
     */
    fun showChatNotification(context: Context, roomId: String, message: String,senderId: String?) {
        // First, check if the app has the POST_NOTIFICATIONS permission.
        // This is a defensive check to prevent a crash if permission isn't granted.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // If permission is not granted, we can't show the notification.
                // We'll log an error and return without showing it.
                // The permission request should be handled by the Activity.
                return
            }
        }

        // Create an Intent to launch the ChatActivity
        val intent = Intent(context, ChatActivity::class.java).apply {
            // Add the room ID as an extra, which is crucial for deep linking
            putExtra(Constants.EXTRA_ROOM_ID, roomId)

            // These flags ensure the activity is launched correctly from a closed state
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        // Create a PendingIntent to wrap the Intent
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            roomId.hashCode(), //a unique request code based on
            intent,
            // FLAG_IMMUTABLE is recommended for security
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build the notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("New Message from $senderId")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Show the notification
        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }
}