package com.jejecomms.realtimechatfeature.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jejecomms.realtimechatfeature.ChatApplication
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/**
 * MessageRetryWorker is a background worker responsible for retrying failed
 * or pending, sent messages.
 */
class MessageRetryWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {


    override suspend fun doWork(): Result {
        val chatRoomRepository = (applicationContext as ChatApplication).chatRoomRepository

        // Collect only failed and pending messages.
        // SENT messages are waiting for a delivery receipt and should not be retried.
        val failedAndPendingMessages = combine(
            chatRoomRepository.getFailedMessages(),
            chatRoomRepository.getPendingMessages()
        ) { failed, pending ->
            failed + pending
        }.first()

        var allRetriesSuccessful = true

        failedAndPendingMessages.forEach { message ->
            try {
                chatRoomRepository.retrySingleMessage(message.roomId, message)
            } catch (e: Exception) {
                allRetriesSuccessful = false
                e.printStackTrace()
            }
        }

        chatRoomRepository.retryFailedReadReceipts()

        return if (allRetriesSuccessful) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}