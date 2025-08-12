package com.jejecomms.realtimechatfeature.ui.chatroomscreen

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jejecomms.realtimechatfeature.R
import com.jejecomms.realtimechatfeature.data.local.ChatRoomEntity
import com.jejecomms.realtimechatfeature.data.model.MessageStatus
import com.jejecomms.realtimechatfeature.data.model.MessageType
import com.jejecomms.realtimechatfeature.ui.chatroomscreen.components.AttachmentOptionsBottomSheet
import com.jejecomms.realtimechatfeature.ui.chatroomscreen.components.DateSeparator
import com.jejecomms.realtimechatfeature.ui.chatroomscreen.components.MessageBubble
import com.jejecomms.realtimechatfeature.ui.chatroomscreen.components.MessageInputField
import com.jejecomms.realtimechatfeature.ui.chatroomscreen.components.SystemMessage
import com.jejecomms.realtimechatfeature.ui.theme.DarkGreenTheme
import com.jejecomms.realtimechatfeature.ui.theme.LightYellow
import com.jejecomms.realtimechatfeature.ui.theme.White
import com.jejecomms.realtimechatfeature.utils.DateUtils
import com.jejecomms.realtimechatfeature.utils.PermissionUtils
import kotlinx.coroutines.launch

/**
 * Composable function for the chat screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    modifier: Modifier = Modifier,
    chatRoomViewModel: ChatRoomViewModel,
    currentSenderId: String,
    selectedChatRoom: ChatRoomEntity?,
    onBackClick: () -> Unit,
) {
    /**
     * Get the room ID from the selected chat room.
     */
    val roomId = selectedChatRoom?.roomId ?: ""

    /**
     * Handle the system back button press to navigate back to the chat room list.
     */
    BackHandler {
        onBackClick()
    }

    /**
     * Initialize the chat room when the screen is first composed.
     */
    LaunchedEffect(roomId) {
        chatRoomViewModel.startDataCollectionAndSync(roomId)
        chatRoomViewModel.updateLastReadTimestamp()
    }

    /**
     * Collects the UI state from the ViewModel.
     */
    val uiState by chatRoomViewModel.uiState.collectAsState()

    /**
     * Creates a TopAppBar scroll behavior.
     */
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    /**
     * Creates a LazyListState for managing the list of messages.
     */
    val listState = rememberLazyListState()

    /**
     * Controls the visibility of the message input field.
     */
    var showInputField by remember { mutableStateOf(false) }

    /**
     * State variable for the check roomId.
     */
    val isRoomIdExists by chatRoomViewModel.isRoomExists.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        showInputField = true
    }

    /**
     * Get the messages list from the UI state.
     */
    val messages = (uiState as? ChatScreenState.Content)?.messages ?: emptyList()

    /**
     * Get the context from the current composition.
     */
    val context = LocalContext.current

    /**
     * Initialize the bottom sheet state.
     */
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    /**
     * Initialize the coroutine scope.
     */
    val coroutineScope = rememberCoroutineScope()

    /**
     * Controls the visibility of the bottom sheet.
     */
    var showBottomSheet by remember { mutableStateOf(false) }

    /**
     * State for the dropdown menu.
     */
    var showMenu by remember { mutableStateOf(false) }

    /**
     * Initialize the image picker launcher.
     */
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            chatRoomViewModel.sendMultipleFileTypeMessage(
                currentSenderId, roomId, uri, context, MessageType.IMAGE
            )
        }
    }

    /**
     * Initialize the document picker launcher.
     */
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            chatRoomViewModel.sendMultipleFileTypeMessage(
                currentSenderId, roomId, uri, context, MessageType.DOCUMENT
            )
        }
    }

    /**
     * Initialize the audio picker launcher.
     */
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            chatRoomViewModel.sendMultipleFileTypeMessage(
                currentSenderId, roomId, uri, context, MessageType.AUDIO
            )
        }
    }

    /**
     * Permission launcher for pre-Android 13
     */
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            imagePickerLauncher.launch(
                PickVisualMediaRequest
                    (ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    /**
     * This LaunchedEffect will check the state of the list before trying to scroll.
     * It also uses a more stable key (the messages list itself), ensuring it reacts
     * to all changes, including messages being updated.
     */
    LaunchedEffect(messages) {
        if (!listState.isScrollInProgress && messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    /**
     * Find the ID of the last visible message
     */
    LaunchedEffect(listState, messages) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null) {

                    val message = messages.getOrNull(lastVisibleIndex)
                    // Check if the message exists, is not from the current user,
                    // and has not already been marked as read.
                    if (message != null && message.senderId != currentSenderId
                        && message.status != MessageStatus.READ
                    ) {
                        chatRoomViewModel.markMessageAsRead(message, currentSenderId)
                    }
                }
            }
    }

    // The bottom sheet is now a separate composable, shown conditionally
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
            },
            sheetState = bottomSheetState,
            containerColor = White
        ) {
            AttachmentOptionsBottomSheet(
                onImageClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest.Builder()
                                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                .build()
                        )
                    } else {
                        PermissionUtils.checkAndRequestLegacyPermission(
                            context = context,
                            permissionLauncher = permissionLauncher,
                            permission = Manifest.permission.READ_EXTERNAL_STORAGE,
                            onPermissionGranted = {
                                imagePickerLauncher
                                    .launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts
                                                .PickVisualMedia.ImageOnly
                                        )
                                    )
                            }
                        )
                    }
                    showBottomSheet = false
                },
                onDocumentClick = {
                    documentPickerLauncher.launch("application/pdf")
                    showBottomSheet = false
                },
                onAudioClick = {
                    audioPickerLauncher.launch("audio/*")
                    showBottomSheet = false
                })
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(selectedChatRoom?.groupName.toString()) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkGreenTheme,
                    titleContentColor = White
                ),
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = White
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            text = {
                                Text(
                                    text = stringResource(R.string.export_chat),
                                    fontSize = 14.sp,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            },
                            onClick = {
                                showMenu = false
                                // Trigger the export logic
                                roomId.let { roomId ->
                                    chatRoomViewModel.exportChat(roomId, context)
                                }
                            }
                        )
                    }
                }
            )
        },
        containerColor = LightYellow,
        bottomBar = {
            if (isRoomIdExists == false) {
                // Show the "room not available" message and exit button
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .background(Color.White)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.room_is_no_longer_available),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Red,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Button(
                        onClick = onBackClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.btn_exit_room),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Show the message input field
                AnimatedVisibility(
                    visible = showInputField,
                    enter = slideInVertically(
                        initialOffsetY = { fullHeight -> fullHeight / 2 }
                    ) + fadeIn(animationSpec = tween(durationMillis = 300)),
                    exit = ExitTransition.None
                ) {
                    MessageInputField(
                        onSendMessage = { message ->
                            chatRoomViewModel.sendMessage(message, currentSenderId, roomId)
                        },
                        modifier = Modifier
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(start = 6.dp, end = 6.dp, bottom = 6.dp),
                        onAttachmentClick = {
                            coroutineScope.launch {
                                showBottomSheet = true
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is ChatScreenState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is ChatScreenState.Content -> {
                    if (messages.isNotEmpty()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp),
                            reverseLayout = false
                        ) {
                            itemsIndexed(messages) { index, message ->
                                // Determine if a date separator should be shown
                                val previousMessageTimestamp =
                                    messages.getOrNull(index - 1)?.timestamp
                                val showDateSeparator = previousMessageTimestamp == null ||
                                        !DateUtils.isSameDay(
                                            previousMessageTimestamp,
                                            message.timestamp
                                        )

                                if (showDateSeparator) {
                                    DateSeparator(timestamp = message.timestamp)
                                }

                                if (message.isSystemMessage) {
                                    SystemMessage(message = message)
                                } else {
                                    MessageBubble(
                                        message = message,
                                        isCurrentUser = message.senderId == currentSenderId
                                    )
                                }
                            }
                        }
                    }
                }

                is ChatScreenState.Error -> {
                    Text(
                        text = (uiState as ChatScreenState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}