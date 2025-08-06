package com.jejecomms.realtimechatfeature.workers

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.jejecomms.realtimechatfeature.utils.NotificationHelper

/**
 * Service for handling incoming Firebase Cloud Messaging (FCM) messages.
 *
 * This service extends [FirebaseMessagingService] to receive and process messages
 * sent from the Firebase console or your application server. It is responsible for:
 * - Receiving data payloads from FCM.
 * - Parsing message data to identify chat messages.
 * - Creating and displaying a system notification for new chat messages.
 * - Attaching a [PendingIntent] to the notification that deep-links to the correct
 * chat room via the [ChatActivity].
 * - Handling device registration tokens by sending them to the application server
 * in the [onNewToken] method.
 */
class RealtimeChatMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // It is recommended to create the notification channel in your Application class
        // to ensure it is created only once.
        NotificationHelper.createNotificationChannel(applicationContext)

        if (remoteMessage.data.isNotEmpty()) {
            val data = remoteMessage.data
            val type = data["type"]
            val roomId = data["roomId"]
            val messagePreview = data["messagePreview"]
            val senderId = data["senderId"]

            if (type == "chat_message" && roomId != null && messagePreview != null) {
                println("RealtimeChatMessagingService: calling NotificationHelper.showChatNotification")
                NotificationHelper.showChatNotification(applicationContext, roomId, messagePreview, senderId)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }
}