package com.jejecomms.realtimechatfeature.ui.chatscreen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.jejecomms.realtimechatfeature.data.local.ChatRoomEntity
import com.jejecomms.realtimechatfeature.ui.chatscreen.components.DateSeparator
import com.jejecomms.realtimechatfeature.ui.chatscreen.components.MessageBubble
import com.jejecomms.realtimechatfeature.ui.chatscreen.components.MessageInputField
import com.jejecomms.realtimechatfeature.ui.chatscreen.components.SystemMessage
import com.jejecomms.realtimechatfeature.ui.theme.DarkGreenTheme
import com.jejecomms.realtimechatfeature.ui.theme.LightYellow
import com.jejecomms.realtimechatfeature.ui.theme.White
import com.jejecomms.realtimechatfeature.utils.DateUtils

/**
 * Composable function for the chat screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatViewModel: ChatScreenViewModel,
    currentSenderId: String,
    selectedChatRoom: ChatRoomEntity?,
    onBackClick: () -> Unit
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
        chatViewModel.initializeChatRoom(roomId)
    }

    /**
     * Collects the UI state from the ViewModel.
     */
    val uiState by chatViewModel.uiState.collectAsState()

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

    LaunchedEffect(Unit) {
        showInputField = true
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
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = LightYellow,
        bottomBar = {
            AnimatedVisibility(
                visible = showInputField,
                enter = slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight / 2 }
                ) + fadeIn(animationSpec = tween(durationMillis = 300)),
                exit = ExitTransition.None
            ) {
                MessageInputField(
                    onSendMessage = { message ->
                        chatViewModel.sendMessage(message, currentSenderId, roomId)
                    },
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(start = 6.dp, end = 6.dp, bottom = 6.dp)
                )
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
                    // This list now contains both chat messages and system messages
                    val messages = (uiState as ChatScreenState.Content).messages
                    LaunchedEffect(messages.size) {
                        if (messages.isNotEmpty()) {
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    }
                    if (messages.isNotEmpty()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
                        ) {
                            itemsIndexed(messages) { index, message ->
                                // Determine if a date separator should be shown
                                val previousMessageTimestamp = messages.getOrNull(index - 1)?.timestamp
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
                                        isCurrentUser = message.senderId == currentSenderId,
                                        onRetryClick = { msgToRetry ->
                                            chatViewModel.retrySendMessage(msgToRetry, roomId)
                                        }
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