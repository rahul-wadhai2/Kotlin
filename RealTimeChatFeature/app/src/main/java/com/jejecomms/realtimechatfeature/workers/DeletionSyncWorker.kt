package com.jejecomms.realtimechatfeature.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jejecomms.realtimechatfeature.ChatApplication
import com.jejecomms.realtimechatfeature.utils.NetworkMonitor.isOnline
import kotlinx.coroutines.flow.first

/**
 * DeletionSyncWorker is a background worker responsible for syncing locally deleted chat rooms.
 */
class DeletionSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val chatRoomsRepository = (applicationContext as ChatApplication).chatRoomsRepository
        val locallyDeletedRooms = chatRoomsRepository.getLocallyDeletedRoomsForSync()

        if (locallyDeletedRooms.isEmpty()) {
            return Result.success()
        }

        // Wait for the device to be online before proceeding.
        // This suspends the coroutine until `isOnline()` emits 'true'.
        try {
            isOnline().first { it }
        } catch (_: Exception) {
            // If the Flow is cancelled or fails for some reason, we retry.
            return Result.retry()
        }

        locallyDeletedRooms.forEach { room ->
            try {
                chatRoomsRepository.deleteRoomFromFirestore(room.roomId.toString())
                chatRoomsRepository.deleteRoomFromLocalDb(room.roomId.toString())
            } catch (_: Exception) {
                // If the Firestore deletion fails for a network reason, WorkManager will handle the retry
                return Result.retry()
            }
        }
        return Result.success()
    }
}