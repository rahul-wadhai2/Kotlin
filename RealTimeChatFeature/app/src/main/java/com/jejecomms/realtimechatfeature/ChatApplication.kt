package com.jejecomms.realtimechatfeature

import android.app.Application
import com.google.firebase.FirebaseApp
import com.jejecomms.realtimechatfeature.data.local.ChatDatabase
import com.jejecomms.realtimechatfeature.utils.SharedPreferencesUtil
import com.jejecomms.realtimechatfeature.utils.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

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

    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase.
        FirebaseApp.initializeApp(this)
        // Initialize SharedPreferencesUtil
        SharedPreferencesUtil.init(applicationContext)
        // Initialize NetworkMonitor
        NetworkMonitor.init(applicationContext)
    }
}