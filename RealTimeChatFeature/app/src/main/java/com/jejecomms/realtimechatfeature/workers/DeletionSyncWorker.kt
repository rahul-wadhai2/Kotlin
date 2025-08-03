package com.jejecomms.realtimechatfeature.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jejecomms.realtimechatfeature.ChatApplication
import com.jejecomms.realtimechatfeature.utils.NetworkMonitor

/**
 * Delet
 */
class DeletionSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val chatRepository = (applicationContext as ChatApplication).chatRepository
        val locallyDeletedRooms = chatRepository.getLocallyDeletedRoomsForSync()

        if (locallyDeletedRooms.isEmpty()) {
            return Result.success()
        }

        if (!NetworkMonitor.isOnline()) {
            return Result.retry()
        }

        locallyDeletedRooms.forEach { room ->
            try {
                chatRepository.deleteRoomFromFirestore(room.roomId.toString())
                chatRepository.deleteRoomFromLocalDb(room.roomId.toString())
            } catch (_: Exception) {
                // If the Firestore deletion fails for a network reason, WorkManager will handle the retry
                return Result.retry()
            }
        }
        return Result.success()
    }
}