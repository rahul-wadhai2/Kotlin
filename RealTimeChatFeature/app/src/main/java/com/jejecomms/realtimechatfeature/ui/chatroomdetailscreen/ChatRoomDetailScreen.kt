package com.jejecomms.realtimechatfeature.ui.chatroomdetailscreen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jejecomms.realtimechatfeature.R
import com.jejecomms.realtimechatfeature.data.local.entity.ChatRoomMemberEntity
import com.jejecomms.realtimechatfeature.ui.chatroomdetailscreen.components.MemberListItem
import com.jejecomms.realtimechatfeature.ui.theme.DarkGreenTheme
import com.jejecomms.realtimechatfeature.ui.theme.LightGreen
import com.jejecomms.realtimechatfeature.ui.theme.LightGrey240
import com.jejecomms.realtimechatfeature.ui.theme.White
import com.jejecomms.realtimechatfeature.utils.Constants.CHAT_ROOM_ROLE_ADMIN
import com.jejecomms.realtimechatfeature.utils.ToastUtils
import me.onebone.toolbar.CollapsingToolbarScaffold
import me.onebone.toolbar.CollapsingToolbarState
import me.onebone.toolbar.ScrollStrategy
import me.onebone.toolbar.rememberCollapsingToolbarScaffoldState

/**
 * Composable function to display the chat room detail screen.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatRoomDetailScreen(
    roomName: String,
    members: List<ChatRoomMemberEntity>,
    onLeaveRoom: () -> Unit,
    onBack: (message: String?) -> Unit,
    currentSenderId: String,
    onRemoveMember: (ChatRoomMemberEntity) -> Unit,
    chatRoomDetailViewModel: ChatRoomDetailViewModel = viewModel()
) {
    /**
     * Handle back button press.
     */
    BackHandler { onBack(null) }

    /**
     * Initialize the CollapsingToolbarScaffold state.
     */
    val state = rememberCollapsingToolbarScaffoldState()

    /**
     * Access the toolbar state from the CollapsingToolbarScaffold.
     */
    val toolbarState: CollapsingToolbarState = state.toolbarState

    /**
     * Check if the current user is an admin.
     */
    val isAdmin = members.any { it.role == CHAT_ROOM_ROLE_ADMIN && it.userId == currentSenderId }

    /**
     * Sort members based on admin status and name.
     */
    val sortedMembers = members.sortedWith(
        compareByDescending<ChatRoomMemberEntity> { it.role == CHAT_ROOM_ROLE_ADMIN }
            .thenBy { it.userName }
    )

    /**
     * Initialize remove dialog states.
     */
    val showRemoveDialog = remember { mutableStateOf(false) }

    /**
     * Initialize menu states.
     */
    val showMenu = remember { mutableStateOf(false) }

    /**
     * Initialize member selection states.
     */
    val memberToRemove = remember { mutableStateOf<ChatRoomMemberEntity?>(null) }

    /**
     * Initialize ownership transfer states.
     */
    val memberToManage = remember { mutableStateOf<ChatRoomMemberEntity?>(null) }

    /**
     * Transfer ownership message state.
     */
    val transferMessage = chatRoomDetailViewModel.transferOwnershipMessage.collectAsState()

    /**
     * Show leave dialog state.
     */
    val showLeaveDialog = chatRoomDetailViewModel.showLeaveDialog.collectAsState()

    /**
     * Get the context.
     */
    val context = LocalContext.current

    // LaunchedEffect to show Toast message for ownership transfer
    LaunchedEffect(transferMessage) {
        if (transferMessage.value != null) {
            transferMessage.let { message ->
                ToastUtils.showLongToast(context, message.toString())
                chatRoomDetailViewModel.clearTransferOwnershipMessage()
            }
        }
    }

    CollapsingToolbarScaffold(
        modifier = Modifier.fillMaxSize(),
        state = state,
        scrollStrategy = ScrollStrategy.EnterAlways,
        toolbar = {
            val progress = toolbarState.progress
            // This Box represents the entire collapsible toolbar area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(color = White)
            ) {
                // The large room name text is visible when the toolbar is expanded
                if (progress > 0.5f) {
                    Text(
                        text = roomName,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        color = Black,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .graphicsLayer {
                                val scale = 1f + (1f - progress) * 0.5f
                                scaleX = scale
                                scaleY = scale
                            }
                    )
                }
            }

            // TopAppBar is pinned and remains at the top
            TopAppBar(
                title = {
                    // The title is only visible when the toolbar is collapsed
                    if (progress < 0.5f) {
                        Text(
                            roomName, style = TextStyle(color = White),
                            fontSize = 18.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onBack(null) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.des_back_arrow),
                            tint = White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkGreenTheme.copy(alpha = 1f - progress),
                    titleContentColor = White
                ),
                modifier = Modifier.pin()
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(White),
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 3.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = LightGrey240)
                ) {
                    Spacer(modifier = Modifier.height(3.dp))
                }
            }

            item {
                Text(
                    text = "Members (${members.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = Black,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 3.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = LightGrey240)
                ) {
                    Spacer(modifier = Modifier.height(3.dp))
                }
            }

            item {
                if (isAdmin) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clickable(onClick = { /* Handle add member click */ })
                            .padding(start = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.des_add_members),
                            tint = LightGreen
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            fontSize = 16.sp,
                            text = stringResource(R.string.add_members),
                            color = Black,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            items(sortedMembers) { member ->
                MemberListItem(
                    member,
                    modifier = if (isAdmin && member.userId != currentSenderId) {
                        Modifier.combinedClickable(
                            onClick = { /* Handle normal click */ },
                            onLongClick = {
                                memberToManage.value = member
                                showMenu.value = true
                            }
                        )
                    } else {
                        Modifier
                    }
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 3.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = LightGrey240)
                ) {
                    Spacer(modifier = Modifier.height(3.dp))
                }
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier
                        .padding(start = 25.dp)
                        .clickable(onClick = { chatRoomDetailViewModel.onLeaveRoomClicked()
                        }),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = stringResource(R.string.des_leave_room),
                        tint = Color.Red
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        fontSize = 16.sp,
                        text = stringResource(R.string.leave_room),
                        color = Color.Red,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (showRemoveDialog.value) {
            memberToRemove.value?.let { member ->
                AlertDialog(
                    onDismissRequest = { showRemoveDialog.value = false },
                    text = {
                        Text(
                            text = stringResource(R.string.remove_message)
                                    + "${member.userName}?", color = Black, fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            onRemoveMember(member)
                            showRemoveDialog.value = false
                        }, colors = ButtonDefaults.buttonColors(containerColor = White)) {
                            Text(
                                stringResource(R.string.remove_member),
                                color = LightGreen,
                                fontSize = 14.sp
                            )
                        }
                    },
                    dismissButton = {
                        Button(onClick = {
                            showRemoveDialog.value = false
                        }, colors = ButtonDefaults.buttonColors(containerColor = White)) {
                            Text(
                                stringResource(R.string.cancel),
                                color = LightGreen,
                                fontSize = 14.sp
                            )
                        }
                    },
                    containerColor = White,
                    tonalElevation = 6.dp
                )
            }
        }
    }

    if (showMenu.value) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = { showMenu.value = false })
                .background(Black.copy(alpha = 0.4f))
                .padding(end = 150.dp, bottom = 80.dp),
            contentAlignment = Alignment.Center,
        ) {
            // This Box will be used as the anchor for the DropdownMenu
            Box(
                modifier = Modifier
                    .wrapContentSize(Alignment.Center)
            ) {
                DropdownMenu(
                    expanded = showMenu.value,
                    onDismissRequest = { showMenu.value = false },
                    modifier = Modifier.background(White)
                ) {
                    if (isAdmin) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.transfer_ownership),
                                    color = Black,
                                    fontSize = 14.sp
                                )
                            },
                            onClick = {
                                showMenu.value = false
                                memberToManage.value?.let { newOwner ->
                                    chatRoomDetailViewModel.transferOwnership(newOwner)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.remove_member)
                                            + "${memberToManage.value?.userName}",
                                    color = Black,
                                    fontSize = 14.sp
                                )
                            },
                            onClick = {
                                memberToRemove.value = memberToManage.value
                                showRemoveDialog.value = true
                                showMenu.value = false
                            }
                        )
                    }
                }
            }
        }
    }

    // Confirmation dialog for leaving the room
    if (showLeaveDialog.value) {
        AlertDialog(
            onDismissRequest = { chatRoomDetailViewModel.onDismissLeaveDialog() },
            text = { Text(text = stringResource(R.string.confirm_leave_room_message),
                     color = Black, fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = { onLeaveRoom()
                        chatRoomDetailViewModel.onDismissLeaveDialog()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = White)
                ) {
                    Text(stringResource(R.string.ok),
                        color = LightGreen, fontSize = 14.sp)
                }
            },
            dismissButton = {
                Button(
                    onClick = { chatRoomDetailViewModel.onDismissLeaveDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = White)
                ) {
                    Text(stringResource(R.string.cancel),
                        color = LightGreen, fontSize = 14.sp)
                }
            },
            containerColor = White,
            tonalElevation = 6.dp
        )
    }
}