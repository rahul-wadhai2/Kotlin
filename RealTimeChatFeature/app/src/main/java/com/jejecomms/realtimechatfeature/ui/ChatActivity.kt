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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.google.firebase.firestore.FirebaseFirestore
import com.jejecomms.realtimechatfeature.ChatApplication
import com.jejecomms.realtimechatfeature.data.local.ChatRoomEntity
import com.jejecomms.realtimechatfeature.data.repository.ChatRepository
import com.jejecomms.realtimechatfeature.ui.chatroomlist.ChatRoomListScreen
import com.jejecomms.realtimechatfeature.ui.chatroomlist.ChatRoomListViewModel
import com.jejecomms.realtimechatfeature.ui.chatroomlist.ChatRoomListViewModelFactory
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

    private val chatRepository by lazy {
        val application = application as ChatApplication
        val applicationScope = application.applicationScope
        val messageDao = application.messageDao
        ChatRepository(firestoreDb, messageDao, applicationScope)
    }

    /**
     * Initialize the ChatScreenViewModel using viewModels delegate.
     * We pass a ChatViewModelFactory to provide the ChatRepository and Application dependencies.
     */
    private val chatScreenViewModel: ChatScreenViewModel by viewModels {
        ChatViewModelFactory(chatRepository, application as ChatApplication)
    }

    /**
     * Initialize the ChatRoomListViewModel using viewModels delegate.
     * We pass a ChatRoomListViewModelFactory to provide the ChatRepository.
     */
    private val chatRoomListViewModel: ChatRoomListViewModel by viewModels {
        ChatRoomListViewModelFactory(chatRepository, application as ChatApplication)
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
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var selectedChatRoom by remember { mutableStateOf<ChatRoomEntity?>(null) }
                    var toogleChatRoom by remember { mutableStateOf<ChatRoomEntity?>(null) }
                    val chatRooms by chatRoomListViewModel.chatRooms.collectAsState()

                    if (selectedChatRoom == null) {
                        ChatRoomListScreen(
                            chatRooms = chatRooms,
                            onChatRoomClick = { chatRoom ->
                                selectedChatRoom = chatRoom
                            },onToggleMute = { chatRoom ->
                                // Call the ViewModel method to toggle mute
                                // chatRoomListViewModel.toggleMute(chatRoom)
                            },
                            onArchive = { chatRoom ->
                                // Call the ViewModel method to archive the chat room
                                // chatRoomListViewModel.archiveChatRoom(chatRoom)
                            },
                            onDelete = { chatRoom ->
                                // Call the ViewModel method to delete the chat room
                                //chatRoomListViewModel.deleteChatRoom(chatRoom)
                            },
                            viewModel = chatRoomListViewModel,
                            currentUserId = currentSenderId
                        )
                    } else {
                        // Pass the selected chat room and a back navigation callback to the ChatScreen
                        ChatScreen(
                            chatViewModel = chatScreenViewModel,
                            currentSenderId = currentSenderId,
                            selectedChatRoom = selectedChatRoom,
                            onBackClick = { selectedChatRoom = null }
                        )
                    }
                }
            }
        }
    }
}