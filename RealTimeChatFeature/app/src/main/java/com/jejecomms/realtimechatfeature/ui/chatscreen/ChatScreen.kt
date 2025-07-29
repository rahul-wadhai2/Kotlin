package com.jejecomms.realtimechatfeature.ui.chatscreen

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jejecomms.realtimechatfeature.R
import com.jejecomms.realtimechatfeature.ui.chatscreen.components.DateSeparator
import com.jejecomms.realtimechatfeature.ui.chatscreen.components.MessageBubble
import com.jejecomms.realtimechatfeature.ui.chatscreen.components.MessageInputField
import com.jejecomms.realtimechatfeature.utils.DateUtils

/**
 *  Composable function for the chat screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatScreenViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val listState = rememberLazyListState()
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
                title = { Text(stringResource(R.string.real_time_chat)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                scrollBehavior = scrollBehavior
            )
        },
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
                        viewModel.sendMessage(message)
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
                    val messages = (uiState as ChatScreenState.Content).messages
                    LaunchedEffect(messages.size) {
                        if (messages.isNotEmpty()) {
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp),
                        reverseLayout = false
                    ) {
                        itemsIndexed(messages) { index, message ->
                            val previousMessageTimestamp = messages.getOrNull(index - 1)?.timestamp
                            if (previousMessageTimestamp != null && !DateUtils
                                .isSameDay(previousMessageTimestamp, message.timestamp)) {
                                DateSeparator(timestamp = message.timestamp)
                            } else if (index == 0) {
                                DateSeparator(timestamp = message.timestamp)
                            }
                            MessageBubble(
                                message = message,
                                isCurrentUser = message.senderId == viewModel.currentUserId
                            )
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