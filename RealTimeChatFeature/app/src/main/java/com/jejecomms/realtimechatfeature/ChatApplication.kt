package com.jejecomms.realtimechatfeature

import android.app.Application
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.jejecomms.realtimechatfeature.data.local.ChatDatabase
import com.jejecomms.realtimechatfeature.data.repository.ChatRoomRepository
import com.jejecomms.realtimechatfeature.data.repository.ChatRoomsRepository
import com.jejecomms.realtimechatfeature.utils.NetworkMonitor
import com.jejecomms.realtimechatfeature.utils.SharedPreferencesUtil
import com.jejecomms.realtimechatfeature.workers.TimestampInitializationWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.TimeUnit

/**
 * Chat application class for the chat feature.
 */
class ChatApplication : Application() {
    /**
     *  Use a CoroutineScope to run long-running tasks like the Firestore listener.
     */
    val applicationScope = CoroutineScope(SupervisorJob())

    /**
     * Lazy initialization of the ChatDatabase instance.
     */
    private val chatDatabase: ChatDatabase by lazy { ChatDatabase.getDatabase(this) }

    /**
     * Lazy initialization of the MessageDao instance.
     */
    val messageDao by lazy { chatDatabase.messageDao() }

    // Lazily create the Firestore instance
    private val firebasFireStore by lazy { FirebaseFirestore.getInstance() }

    /**
     * Expose the ChatRoomRepository instance.
     */
    val chatRoomsRepository by lazy {
        ChatRoomsRepository(firebasFireStore, messageDao, this)
    }

    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase.
        FirebaseApp.initializeApp(this)
        // Initialize SharedPreferencesUtil
        SharedPreferencesUtil.init(applicationContext)
        // Initialize NetworkMonitor
        NetworkMonitor.init(applicationContext)

        // Enqueue the worker to initialize timestamps on app launch
        val workRequest = OneTimeWorkRequest.Builder(TimestampInitializationWorker::class.java)
            .setInitialDelay(1, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            "TimestampInitializationWork",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }
}