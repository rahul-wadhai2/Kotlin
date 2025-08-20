package com.jejecomms.realtimechatfeature

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.jejecomms.realtimechatfeature.data.local.ChatDatabase
import com.jejecomms.realtimechatfeature.data.repository.ChatRoomRepository
import com.jejecomms.realtimechatfeature.data.repository.ChatRoomsRepository
import com.jejecomms.realtimechatfeature.data.repository.LoginRepository
import com.jejecomms.realtimechatfeature.utils.NetworkMonitor
import com.jejecomms.realtimechatfeature.utils.SharedPreferencesUtils
import com.jejecomms.realtimechatfeature.workers.MessageRetryWorker
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

    /**
     * Lazy initialization of the UserDao instance.
     */
    val userDao by lazy { chatDatabase.userDao() }

    /**
     * Lazy initialization of the ChatRoomDetailDao instance.
     */
    val chatRoomDetailDao by lazy { chatDatabase.chatRoomDetailDao() }

    /**
     * Lazily create the Firestore instance.
     */
    private val firebasFireStore by lazy { FirebaseFirestore.getInstance() }

    /**
     * Lazily create the Firesbase Storage instance.
     */
    private val firebaseStorage by lazy { FirebaseStorage.getInstance() }

    /**
     * Lazily create the Firesbase Auth instance.
     */
    private val firebaseAuth by lazy { FirebaseAuth.getInstance() }

    /**
     * Expose the LoginRepository instance.
     */
    val loginRepository by lazy {
        LoginRepository(firebaseAuth, firebasFireStore, userDao)
    }

    /**
     * Expose the ChatRoomsRepository instance.
     */
    val chatRoomsRepository by lazy {
        ChatRoomsRepository(this, firebasFireStore, messageDao, loginRepository, applicationScope)
    }

    /**
     * Expose the ChatRoomRepository instance.
     */
    val chatRoomRepository by lazy {
        ChatRoomRepository(firebasFireStore, messageDao, firebaseStorage)
    }

    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase.
        FirebaseApp.initializeApp(this)
        // Initialize SharedPreferencesUtil
        SharedPreferencesUtils.init(this)
        // Initialize NetworkMonitor
        NetworkMonitor.init(this)

        // Enqueue the worker to initialize timestamps on app launch
        val workRequest = OneTimeWorkRequest.Builder(TimestampInitializationWorker::class.java)
            .setInitialDelay(1, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            "TimestampInitializationWork",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
        // Enqueue the worker to retry failed messages
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Create a one-time work request
        val retryMessagesRequest = OneTimeWorkRequestBuilder<MessageRetryWorker>()
            .setConstraints(constraints)
            .build()
        // Enqueue the work request
        WorkManager.getInstance(this).enqueue(retryMessagesRequest)
    }
}