package com.jejecomms.realtimechatfeature.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.jejecomms.realtimechatfeature.ChatApplication
import com.jejecomms.realtimechatfeature.data.local.ChatRoomEntity
import com.jejecomms.realtimechatfeature.data.repository.ChatRoomRepository
import com.jejecomms.realtimechatfeature.data.repository.ChatRoomsRepository
import com.jejecomms.realtimechatfeature.ui.chatroomscreen.ChatRoomScreen
import com.jejecomms.realtimechatfeature.ui.chatroomscreen.ChatRoomViewModel
import com.jejecomms.realtimechatfeature.ui.chatroomscreen.ChatRoomViewModelFactory
import com.jejecomms.realtimechatfeature.ui.chatroomsscreen.ChatRoomsScreen
import com.jejecomms.realtimechatfeature.ui.chatroomsscreen.ChatRoomsViewModel
import com.jejecomms.realtimechatfeature.ui.chatroomsscreen.ChatRoomsViewModelFactory
import com.jejecomms.realtimechatfeature.ui.theme.RealTimeChatFeatureTheme
import com.jejecomms.realtimechatfeature.utils.Constants.EXTRA_ROOM_ID
import com.jejecomms.realtimechatfeature.utils.Constants.KEY_SENDER_ID
import com.jejecomms.realtimechatfeature.utils.PermissionUtils
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
     * Initialize Firebase Storage instance
     */
    private val firebaseStorage by lazy { FirebaseStorage.getInstance() }

    /**
     * Initialize the ChatRoomRepository using lazy initialization.
     */
    private val chatRoomRepository by lazy {
        val application = application as ChatApplication
        val applicationScope = application.applicationScope
        val messageDao = application.messageDao
        ChatRoomRepository(firestoreDb, messageDao, applicationScope, firebaseStorage)
    }

    /**
     * Initialize the ChatRoomsRepository using lazy initialization.
     */
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

    // This state variable tracks the permission status and triggers recomposition
    private var isNotificationPermissionGranted by mutableStateOf(false)

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // The result of the permission request is stored here, triggering a recomposition
        isNotificationPermissionGranted = isGranted
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val newRoomId = intent.getStringExtra(EXTRA_ROOM_ID)
        chatRoomsViewModel.handleInitialDeepLink(newRoomId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        currentSenderId = SharedPreferencesUtil.getString(KEY_SENDER_ID) ?: run {
            val newId = UuidGenerator.generateUniqueId()
            SharedPreferencesUtil.putString(KEY_SENDER_ID, newId)
            newId
        }

        // Pass the initial room ID from the intent to the ViewModel for handling
        val initialRoomId = intent.getStringExtra(EXTRA_ROOM_ID)
        chatRoomsViewModel.handleInitialDeepLink(initialRoomId)

        setContent {
            val coroutineScope = rememberCoroutineScope()
            val snackbarHostState = remember { SnackbarHostState() }
            val context = LocalContext.current
            var selectedChatRoom by remember { mutableStateOf<ChatRoomEntity?>(null) }
            val chatRooms by chatRoomsViewModel.chatRooms.collectAsState()

            // Collect the deep link chat room from the ViewModel
            val deepLinkRoom by chatRoomsViewModel.deepLinkChatRoom.collectAsState()

            // LaunchedEffect to navigate when the ViewModel provides a deep link room
            LaunchedEffect(deepLinkRoom) {
                deepLinkRoom?.let { room ->
                    selectedChatRoom = room
                    // Clear the deep link state in the ViewModel to prevent re-navigation
                    chatRoomsViewModel.clearDeepLinkChatRoom()
                }
            }

            // State variable to track the permission status
            isNotificationPermissionGranted = remember {
                mutableStateOf(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    } else {
                        true
                    }
                )
            }.value

            // State variable to ensure the request is only attempted once
            var permissionCheckStarted by remember { mutableStateOf(false) }

            // This LaunchedEffect is now triggered by a state change, ensuring it's not a race condition.
            LaunchedEffect(permissionCheckStarted) {
                if (!permissionCheckStarted) {
                    permissionCheckStarted = true

                    // Call the new utility function to handle the permission logic
                    PermissionUtils.checkAndRequestNotificationPermission(
                        context = context,
                        activity = this@ChatActivity,
                        permissionLauncher = requestNotificationPermissionLauncher,
                        snackbarHostState = snackbarHostState,
                        coroutineScope = coroutineScope
                    )
                }
            }

            RealTimeChatFeatureTheme(
                dynamicColor = false
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        snackbarHost = {
                            SnackbarHost(hostState = snackbarHostState) { data ->
                                Snackbar(
                                    modifier = Modifier,
                                    content = {
                                        Text(data.visuals.message)
                                    },
                                    action = {
                                        data.visuals.actionLabel?.let { actionLabel ->
                                            TextButton(onClick = {
                                                data.performAction()
                                            }) {
                                                Text(
                                                    actionLabel,
                                                    color = MaterialTheme.colorScheme.inversePrimary
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    ) { innerPadding ->
                        if (selectedChatRoom == null) {
                            ChatRoomsScreen(
                                modifier = Modifier.padding(innerPadding),
                                chatRooms = chatRooms,
                                onChatRoomClick = { chatRoom ->
                                    selectedChatRoom = chatRoom
                                }, onToggleMute = {
                                    // Call the ViewModel method to toggle mute
                                },
                                onArchive = {
                                    // Call the ViewModel method to archive the chat room
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
                            ChatRoomScreen(
                                modifier = Modifier.padding(innerPadding),
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
}