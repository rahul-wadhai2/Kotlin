package com.jejecomms.realtimechatfeature.workers
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jejecomms.realtimechatfeature.ChatApplication

/**
 * TimestampInitializationWorker is a background worker responsible for initializing
 * timestamps in the local database.
 */
class TimestampInitializationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val chatRoomsRepository = (appContext as ChatApplication).chatRoomsRepository

    override suspend fun doWork(): Result {
        return try {
            chatRoomsRepository.ensureAllTimestampsInitialized()
            Result.success()
        } catch (_: Exception) {
            Result.failure()
        }
    }
}