package com.jejecomms.realtimechatfeature.ui.chatroomsscreen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jejecomms.realtimechatfeature.R
import com.jejecomms.realtimechatfeature.data.local.ChatRoomEntity
import com.jejecomms.realtimechatfeature.ui.chatroomsscreen.components.ChatRoomItem
import com.jejecomms.realtimechatfeature.ui.theme.DarkGreenTheme
import com.jejecomms.realtimechatfeature.ui.theme.LightGreen
import com.jejecomms.realtimechatfeature.ui.theme.White

/**
 * Composable function for the chat rooms screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomListScreen(
    chatRooms: List<ChatRoomEntity>,
    onChatRoomClick: (ChatRoomEntity) -> Unit,
    onToggleMute: (ChatRoomEntity) -> Unit,
    onArchive: (ChatRoomEntity) -> Unit,
    onDelete: (ChatRoomEntity) -> Unit,
    chatRoomsViewModel: ChatRoomsViewModel = viewModel(),
    currentUserId: String
) {
    /**
     * State variables for the dialog and input fields.
     */
    var showCreateGroupDialog by remember { mutableStateOf(false) }

    /**
     * State variables for the group name.
     */
    var groupName by remember { mutableStateOf("") }

    /**
     * State variables for the user name.
     */
    var userName by remember { mutableStateOf("") }

    /**
     * State variables for the group name error.
     */
    var groupNameError by remember { mutableStateOf(false) }

    /**
     * State variables for the user name error.
     */
    var userNameError by remember { mutableStateOf(false) }

    /**
     * State indicating the success of group creation.
     */
    val groupCreationSuccessful by chatRoomsViewModel.groupCreationSuccessful.collectAsStateWithLifecycle()

    /**
     * State variable for the create group error.
     */
    val createGroupError by chatRoomsViewModel.createGroupError.collectAsStateWithLifecycle()

    /**
     * Context for the composable.
     */
    val context = LocalContext.current

    //This LaunchedEffect reacts to the success state.
    LaunchedEffect(groupCreationSuccessful) {
        if (groupCreationSuccessful) {
            // Show a toast message on successful creation
            Toast.makeText(context, context.getString(R.string.group_created_successful)
                ,Toast.LENGTH_SHORT).show()

            // Dismiss the dialog
            showCreateGroupDialog = false
            // Reset fields
            groupName = ""
            userName = ""
            // Reset the success state in the ViewModel to prevent
            // the LaunchedEffect from re-triggering if the screen recomposes
            // for other reasons while groupCreationSuccessful is still true.
            chatRoomsViewModel.resetGroupCreationStatus()
        }
    }

    if (showCreateGroupDialog) {
        Dialog(onDismissRequest = {
            showCreateGroupDialog = false
            groupName = ""
            userName = ""
            chatRoomsViewModel.resetGroupCreationStatus()
        }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.create_group),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = {
                            groupName = it
                            groupNameError = it.isEmpty()
                            if (groupNameError) {
                                chatRoomsViewModel.resetGroupCreationStatus()
                            }
                        },
                        label = { Text(stringResource(R.string.enter_group_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = groupNameError || createGroupError != null,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LightGreen,
                            unfocusedBorderColor = Color.LightGray,
                            errorBorderColor = Color.Red
                        )
                    )
                    if (groupNameError) {
                        Text(
                            text = stringResource(R.string.error_group_name_empty),
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    } else if (createGroupError != null) {
                        Text(
                            text = createGroupError!!,
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = userName,
                        onValueChange = {
                            userName = it
                            userNameError = it.isEmpty()
                        },
                        label = { Text(stringResource(R.string.enter_your_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = userNameError,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LightGreen,
                            unfocusedBorderColor = Color.LightGray,
                            errorBorderColor = Color.Red
                        )
                    )
                    if (userNameError) {
                        Text(
                            text = stringResource(R.string.error_user_name_empty),
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            groupNameError = groupName.isEmpty()
                            userNameError = userName.isEmpty()
                            if (!groupNameError && !userNameError) {
                                chatRoomsViewModel.createChatRoom(groupName, userName, currentUserId)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LightGreen,
                            contentColor = White
                        )
                    ) {
                        Text(stringResource(R.string.btn_dialog_text))
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.chat_rooms)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkGreenTheme,
                    titleContentColor = White
                )
            )
        },
        containerColor = Color.White,
        floatingActionButton = {
            AnimatedVisibility(
                visible = !showCreateGroupDialog,
                enter = fadeIn(animationSpec = tween(durationMillis = 500)) + slideInHorizontally(
                    animationSpec = tween(durationMillis = 500),
                    initialOffsetX = { fullWidth -> -fullWidth / 2 }
                ),
                exit = fadeOut(animationSpec = tween(durationMillis = 500)) + slideOutHorizontally(
                    animationSpec = tween(durationMillis = 500),
                    targetOffsetX = { fullWidth -> -fullWidth / 2 }
                )
            ) {
                FloatingActionButton(
                    onClick = { showCreateGroupDialog = true },
                    containerColor = LightGreen,
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(R.string.des_add_new_room),
                        tint = Color.White
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .background(Color.White),
            contentPadding = PaddingValues(top = 8.dp)
        ) {
            items(chatRooms) { chatRoomEntity ->
                ChatRoomItem(
                    chatRoom = chatRoomEntity,
                    onChatRoomClick = onChatRoomClick,
                    onArchive = onArchive,
                    onDelete = onDelete,
                    onToggleMute = onToggleMute
                )
            }
        }
    }
}