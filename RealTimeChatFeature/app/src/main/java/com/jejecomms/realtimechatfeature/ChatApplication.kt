package com.jejecomms.realtimechatfeature

import android.app.Application
import com.google.firebase.FirebaseApp
import com.jejecomms.realtimechatfeature.utils.SharedPreferencesUtil
import com.jejecomms.realtimechatfeature.utils.NetworkMonitor

/**
 * Chat application class for the chat feature.
 */
class ChatApplication : Application() {
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