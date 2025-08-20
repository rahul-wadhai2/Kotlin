package com.jejecomms.realtimechatfeature.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.jejecomms.realtimechatfeature.ChatApplication
import com.jejecomms.realtimechatfeature.R
import com.jejecomms.realtimechatfeature.data.local.entity.ChatRoomEntity
import com.jejecomms.realtimechatfeature.data.repository.ChatRoomDetailRepository
import com.jejecomms.realtimechatfeature.data.repository.ChatRoomRepository
import com.jejecomms.realtimechatfeature.data.repository.ChatRoomsRepository
import com.jejecomms.realtimechatfeature.data.repository.LoginRepository
import com.jejecomms.realtimechatfeature.ui.chatroomAddmembers.AddMembersScreen
import com.jejecomms.realtimechatfeature.ui.chatroomdetailscreen.ChatRoomDetailScreen
import com.jejecomms.realtimechatfeature.ui.chatroomdetailscreen.ChatRoomDetailViewModel
import com.jejecomms.realtimechatfeature.ui.chatroomdetailscreen.ChatRoomDetailViewModelFactory
import com.jejecomms.realtimechatfeature.ui.chatroomscreen.ChatRoomScreen
import com.jejecomms.realtimechatfeature.ui.chatroomscreen.ChatRoomViewModel
import com.jejecomms.realtimechatfeature.ui.chatroomscreen.ChatRoomViewModelFactory
import com.jejecomms.realtimechatfeature.ui.chatroomsscreen.ChatRoomsScreen
import com.jejecomms.realtimechatfeature.ui.chatroomsscreen.ChatRoomsViewModel
import com.jejecomms.realtimechatfeature.ui.chatroomsscreen.ChatRoomsViewModelFactory
import com.jejecomms.realtimechatfeature.ui.loginscreen.LoginScreen
import com.jejecomms.realtimechatfeature.ui.loginscreen.LoginViewModel
import com.jejecomms.realtimechatfeature.ui.loginscreen.LoginViewModelFactory
import com.jejecomms.realtimechatfeature.ui.theme.LightGreen
import com.jejecomms.realtimechatfeature.ui.theme.RealTimeChatFeatureTheme
import com.jejecomms.realtimechatfeature.ui.theme.White
import com.jejecomms.realtimechatfeature.utils.Constants.EXTRA_ROOM_ID
import com.jejecomms.realtimechatfeature.utils.Constants.KEY_SENDER_ID
import com.jejecomms.realtimechatfeature.utils.NetworkMonitor
import com.jejecomms.realtimechatfeature.utils.PermissionUtils
import com.jejecomms.realtimechatfeature.utils.SharedPreferencesUtils
import com.jejecomms.realtimechatfeature.utils.ToastUtils.showCustomToast

/**
 * Chat activity for the chat feature.
 */
class ChatActivity : ComponentActivity() {
    /**
     *Initialize FireStore instance.
     */
    private val firestoreDb by lazy { FirebaseFirestore.getInstance() }

    /**
     * Initialize Firebase Storage instance.
     */
    private val firebaseStorage by lazy { FirebaseStorage.getInstance() }

    /**
     * Firebase Authentication instance.
     */
    private val firebaseAuth by lazy { FirebaseAuth.getInstance() }

    /**
     * Initialize the ChatRoomRepository using lazy initialization.
     */
    private val chatRoomRepository by lazy {
        val application = application as ChatApplication
        val messageDao = application.messageDao
        ChatRoomRepository(firestoreDb, messageDao, firebaseStorage)
    }

    /**
     * Initialize the ChatRoomDetailRepository using lazy initialization.
     */
    private val chatRoomDetailRepository by lazy {
        val application = application as ChatApplication
        val chatRoomDetailDao = application.chatRoomDetailDao
        ChatRoomDetailRepository(firestoreDb, chatRoomDetailDao, application.applicationScope)
    }

    /**
     * Initialize the ChatRoomsRepository using lazy initialization.
     */
    private val chatRoomsRepository by lazy {
        val application = application as ChatApplication
        val messageDao = application.messageDao
        ChatRoomsRepository(this, firestoreDb, messageDao, loginRepository,
            application.applicationScope)
    }

    /**
     * Initialize the LoginRepository using lazy initialization.
     */
    private val loginRepository by lazy {
        val application = application as ChatApplication
        val userDao = application.userDao
        LoginRepository(firebaseAuth, firestoreDb, userDao)
    }

    /**
     * Initialize the ChatRoomsViewModel using viewModels delegate.
     * We pass a ChatRoomsViewModelFactory to provide the ChatRoomsRepository.
     */
    private val chatRoomsViewModel: ChatRoomsViewModel by viewModels {
        ChatRoomsViewModelFactory(
            chatRoomsRepository, loginRepository, application as ChatApplication
        )
    }

    /**
     * Initialize the ChatRoomDetailViewModel using viewModels delegate.
     */
    private val loginViewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(loginRepository, NetworkMonitor)
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
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        // Pass the initial room ID from the intent to the ViewModel for handling
        val initialRoomId = intent.getStringExtra(EXTRA_ROOM_ID)
        chatRoomsViewModel.handleInitialDeepLink(initialRoomId)

        setContent {
            val coroutineScope = rememberCoroutineScope()
            val snackbarHostState = remember { SnackbarHostState() }
            val context = LocalContext.current
            var selectedChatRoom by remember { mutableStateOf<ChatRoomEntity?>(null) }
            val chatRooms by chatRoomsViewModel.chatRooms.collectAsState()

            val currentUser by loginViewModel.currentUser.collectAsState()

            // Collect the deep link chat room from the ViewModel
            val deepLinkRoom by chatRoomsViewModel.deepLinkChatRoom.collectAsState()

            // State variable to manage the delete confirmation dialog
            var showDeleteConfirmationDialog by remember { mutableStateOf<ChatRoomEntity?>(null) }

            // State to control ChatRoomDetailScreen visibility
            var showRoomDetailsForSelectedChatRoom by remember { mutableStateOf(false) }

            // State to control AddMembersScreen visibility
            var showAddMembersScreen by remember { mutableStateOf(false) }

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
                if (currentUser == null) {
                    LoginScreen(
                        loginViewModel = loginViewModel,
                        onLoginSuccess = {
                            showCustomToast(
                                context = context,
                                message = context.getString(R.string.successfully_logged_in),
                                gravity = Gravity.CENTER,
                                duration = Toast.LENGTH_LONG
                            )
                        }
                    )
                } else {
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
                            currentSenderId =
                                SharedPreferencesUtils.getString(KEY_SENDER_ID) ?: run {
                                    val newId = currentUser?.uid
                                    SharedPreferencesUtils.putString(
                                        KEY_SENDER_ID,
                                        newId.toString()
                                    )
                                    newId.toString()
                                }

                            val chatRoomDetailViewModel: ChatRoomDetailViewModel by viewModels {
                                ChatRoomDetailViewModelFactory(
                                    loginRepository,
                                    chatRoomsRepository,
                                    chatRoomDetailRepository,
                                    application as ChatApplication
                                )
                            }

                            // This LaunchedEffect will be triggered whenever selectedChatRoom changes.
                            LaunchedEffect(selectedChatRoom) {
                                selectedChatRoom?.roomId?.let { roomId ->
                                    chatRoomDetailViewModel.loadChatRoom(roomId)
                                }
                            }

                            when {
                                selectedChatRoom == null -> {
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
                                            showDeleteConfirmationDialog = chatRoom
                                            //chatRoomsViewModel.onDeleteChatRoom(chatRoom.roomId.toString())
                                        },
                                        chatRoomsViewModel = chatRoomsViewModel,
                                        currentUserId = currentSenderId
                                    )

                                    // Show the delete confirmation dialog
                                    // if a chat room is selected for deletion
                                    if (showDeleteConfirmationDialog != null) {
                                        Dialog(onDismissRequest = {
                                            showDeleteConfirmationDialog = null
                                        }) {
                                            Card(
                                                modifier = Modifier.padding(16.dp),
                                                shape = RoundedCornerShape(16.dp),
                                                colors = CardDefaults
                                                    .cardColors(containerColor = White)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(16.dp),
                                                    horizontalAlignment = Alignment
                                                        .CenterHorizontally
                                                ) {
                                                    Text(
                                                        color = Black,
                                                        fontSize = 14.sp,
                                                        text = stringResource(R.string
                                                            .delete_confirmation),
                                                        style = MaterialTheme.typography
                                                            .titleMedium
                                                    )
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.End
                                                    ) {
                                                        Button(
                                                            onClick = {
                                                                showDeleteConfirmationDialog = null
                                                            },
                                                            colors = ButtonDefaults
                                                                .buttonColors
                                                                    (containerColor = White)
                                                        ) {
                                                            Text(
                                                                stringResource(R.string.cancel),
                                                                color = LightGreen,
                                                                fontSize = 14.sp
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Button(
                                                            onClick = {
                                                                showDeleteConfirmationDialog
                                                                    ?.let { chatRoom ->
                                                                        chatRoomsViewModel
                                                                            .onDeleteChatRoom(
                                                                                chatRoom
                                                                            .roomId.toString()
                                                                            )
                                                                    }
                                                                showDeleteConfirmationDialog = null
                                                            },
                                                            colors = ButtonDefaults
                                                                .buttonColors
                                                                    (containerColor = White)
                                                        ) {
                                                            Text(stringResource(R.string.ok),
                                                                color = LightGreen,
                                                                fontSize = 14.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                showAddMembersScreen -> {
                                    val chatRoomDetailViewModel: ChatRoomDetailViewModel
                                    by viewModels {
                                        selectedChatRoom?.roomId.let { roomId ->
                                            ChatRoomDetailViewModelFactory(
                                                loginRepository, chatRoomsRepository,
                                                chatRoomDetailRepository,
                                                application as ChatApplication
                                            )
                                        }
                                    }
                                    AddMembersScreen(
                                        senderId = currentSenderId,
                                        onBack = { showAddMembersScreen = false },
                                        chatRoomDetailViewModel = chatRoomDetailViewModel)
                                }

                                showRoomDetailsForSelectedChatRoom -> {
                                    val chatRoomDetailViewModel: ChatRoomDetailViewModel
                                            by viewModels {
                                                selectedChatRoom?.roomId.let { roomId ->
                                                    ChatRoomDetailViewModelFactory(
                                                        loginRepository, chatRoomsRepository,
                                                        chatRoomDetailRepository,
                                                        application as ChatApplication
                                                    )
                                                }
                                            }
                                    // Retrieve the members from the ViewModel.
                                    val members by chatRoomDetailViewModel.members.collectAsState()

                                    if (members.isEmpty()) {
                                        // Show a loading indicator while members are being fetched
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator()
                                        }
                                    } else {
                                        ChatRoomDetailScreen(
                                            roomName = selectedChatRoom!!.title.toString(),
                                            members = members,
                                            onLeaveRoom = {
                                                chatRoomDetailViewModel
                                                    .onConfirmLeave(currentSenderId)
                                            },
                                            onBack = {
                                                showRoomDetailsForSelectedChatRoom = false
                                            },
                                            currentSenderId = currentSenderId,
                                            onRemoveMember = { member ->
                                                chatRoomDetailViewModel.removeMember(member)
                                            },
                                            onAddMembersClick = {
                                                showAddMembersScreen = true
                                            }
                                        )
                                    }
                                }

                                else -> {
                                    val chatRoomViewModel: ChatRoomViewModel = viewModel(
                                        factory = ChatRoomViewModelFactory(
                                            chatRoomRepository,
                                            application as ChatApplication,
                                            selectedChatRoom!!.roomId.toString()
                                        )
                                    )

                                    ChatRoomScreen(
                                        modifier = Modifier.padding(innerPadding),
                                        onNavigateToDetail = {
                                            showRoomDetailsForSelectedChatRoom = true
                                        },
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
    }
}