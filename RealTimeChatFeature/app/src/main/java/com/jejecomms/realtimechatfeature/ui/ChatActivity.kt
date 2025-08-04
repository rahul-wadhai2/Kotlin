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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.jejecomms.realtimechatfeature.ChatApplication
import com.jejecomms.realtimechatfeature.data.local.ChatRoomEntity
import com.jejecomms.realtimechatfeature.data.repository.ChatRoomRepository
import com.jejecomms.realtimechatfeature.data.repository.ChatRoomsRepository
import com.jejecomms.realtimechatfeature.ui.chatroomsscreen.ChatRoomListScreen
import com.jejecomms.realtimechatfeature.ui.chatroomsscreen.ChatRoomsViewModel
import com.jejecomms.realtimechatfeature.ui.chatroomsscreen.ChatRoomsViewModelFactory
import com.jejecomms.realtimechatfeature.ui.chatroomscreen.ChatRoomViewModel
import com.jejecomms.realtimechatfeature.ui.chatroomscreen.ChatRoomViewModelFactory
import com.jejecomms.realtimechatfeature.ui.chatroomscreen.ChatScreen
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

    private val chatRoomRepository by lazy {
        val application = application as ChatApplication
        val applicationScope = application.applicationScope
        val messageDao = application.messageDao
        ChatRoomRepository(firestoreDb, messageDao, applicationScope)
    }

    private val chatRoomsRepository by lazy {
        val application = application as ChatApplication
        val messageDao = application.messageDao
        ChatRoomsRepository(firestoreDb, messageDao, this)
    }

    /**
     * Initialize the ChatRoomsViewModel using viewModels delegate.
     * We pass a ChatRoomsViewModelFactory to provide the ChatRoomsRepository.
     */
    private val chatRoomsViewModel: ChatRoomsViewModel by viewModels {
        ChatRoomsViewModelFactory(chatRoomsRepository, application as ChatApplication)
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
                    val chatRooms by chatRoomsViewModel.chatRooms.collectAsState()

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
                                chatRoomsViewModel.onDeleteChatRoom(chatRoom.roomId.toString())
                            },
                            chatRoomsViewModel = chatRoomsViewModel,
                            currentUserId = currentSenderId
                        )
                    } else {

                        val chatRoomViewModel: ChatRoomViewModel = viewModel(
                            factory = ChatRoomViewModelFactory(
                                chatRoomRepository,
                                application as ChatApplication,
                                selectedChatRoom!!.roomId.toString()
                            )
                        )

                        ChatScreen(
                            chatRoomViewModel = chatRoomViewModel,
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