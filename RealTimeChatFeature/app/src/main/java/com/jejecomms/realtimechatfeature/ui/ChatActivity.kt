package com.jejecomms.realtimechatfeature.ui

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.google.firebase.firestore.FirebaseFirestore
import com.jejecomms.realtimechatfeature.ChatApplication
import com.jejecomms.realtimechatfeature.data.repository.ChatRepository
import com.jejecomms.realtimechatfeature.ui.chatscreen.ChatScreen
import com.jejecomms.realtimechatfeature.ui.chatscreen.ChatScreenViewModel
import com.jejecomms.realtimechatfeature.ui.chatscreen.ChatViewModelFactory
import com.jejecomms.realtimechatfeature.ui.theme.RealTimeChatFeatureTheme
import com.jejecomms.realtimechatfeature.utils.Constants.KEY_SENDER_ID
import com.jejecomms.realtimechatfeature.utils.SharedPreferencesUtil
import com.jejecomms.realtimechatfeature.utils.UuidGenerator

/**
 * Chat activity for the chat feature.
 */
class ChatActivity : ComponentActivity() {
    /**
     *Initialize FireStore instance
     */
    private val firestoreDb by lazy { FirebaseFirestore.getInstance() }

    /**
     * Initialize the ChatScreenViewModel using viewModels delegate.
     * We pass a ChatViewModelFactory to provide the ChatRepository and Application dependencies.
     */
    private val chatViewModel: ChatScreenViewModel by viewModels {
        val application = application as ChatApplication
        val applicationScope = application.applicationScope
        val messageDao = application.messageDao
        ChatViewModelFactory(ChatRepository(firestoreDb, messageDao, applicationScope), application)
    }

    /**
     * Initialize the current sender ID.
     */
    private lateinit var currentSenderId: String

    @SuppressLint("CoroutineCreationDuringComposition")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        currentSenderId = SharedPreferencesUtil.getString(KEY_SENDER_ID) ?: run {
            val newId = UuidGenerator.generateUniqueId()
            SharedPreferencesUtil.putString(KEY_SENDER_ID, newId)
            newId
        }
        setContent {
            RealTimeChatFeatureTheme(
                dynamicColor = false
            ) {

                Surface(
                    modifier = Modifier.Companion.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatScreen(chatViewModel, currentSenderId)
                }
            }
        }
    }
}