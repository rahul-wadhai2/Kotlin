package com.jejecomms.realtimechatfeature.ui.chatroomsscreen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jejecomms.realtimechatfeature.R
import com.jejecomms.realtimechatfeature.data.local.entity.ChatRoomEntity
import com.jejecomms.realtimechatfeature.data.local.entity.UsersEntity
import com.jejecomms.realtimechatfeature.ui.chatroomsscreen.components.ChatRoomsItem
import com.jejecomms.realtimechatfeature.ui.theme.DarkGreenTheme
import com.jejecomms.realtimechatfeature.ui.theme.LightGreen
import com.jejecomms.realtimechatfeature.ui.theme.White
import kotlinx.coroutines.launch

/**
 * Composable function for the chat rooms screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomsScreen(
    modifier: Modifier = Modifier,
    chatRooms: List<ChatRoomEntity>,
    onChatRoomClick: (ChatRoomEntity) -> Unit,
    onToggleMute: (ChatRoomEntity) -> Unit,
    onArchive: (ChatRoomEntity) -> Unit,
    onDelete: (ChatRoomEntity) -> Unit,
    chatRoomsViewModel: ChatRoomsViewModel = viewModel(),
    currentUserId: String
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var groupName by remember { mutableStateOf("") }
    var groupNameError by remember { mutableStateOf(false) }

    val groupCreationSuccessful by chatRoomsViewModel.groupCreationSuccessful
        .collectAsStateWithLifecycle()
    val createGroupError by chatRoomsViewModel.createGroupError.collectAsStateWithLifecycle()

    val allUsers by chatRoomsViewModel.allUsers.collectAsStateWithLifecycle()
    var selectedUsers by remember { mutableStateOf(listOf<UsersEntity>()) }

    val context = LocalContext.current
    var showBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(groupCreationSuccessful) {
        if (groupCreationSuccessful) {
            Toast.makeText(
                context,
                context.getString(R.string.group_created_successful),
                Toast.LENGTH_SHORT
            ).show()

            showBottomSheet = false
            scope.launch { sheetState.hide() }
            groupName = ""
            selectedUsers = listOf()
            chatRoomsViewModel.resetGroupCreationStatus()
        }
    }

    LaunchedEffect(Unit) {
        chatRoomsViewModel.startUsersDataSync()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.chat_rooms)
                        ,fontSize = 18.sp) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkGreenTheme,
                    titleContentColor = White
                )
            )
        },
        containerColor = Color.White,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showBottomSheet = true
                },
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
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .background(Color.White),
            contentPadding = PaddingValues(top = 8.dp)
        ) {
            items(chatRooms) { chatRoomEntity ->
                ChatRoomsItem(
                    chatRoom = chatRoomEntity,
                    onChatRoomClick = onChatRoomClick,
                    onArchive = onArchive,
                    onDelete = onDelete,
                    onToggleMute = onToggleMute
                )
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
                groupName = ""
                groupNameError = false
                selectedUsers = listOf()
                chatRoomsViewModel.resetGroupCreationStatus()
            },
            sheetState = sheetState,
            containerColor = White
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.new_group),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
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

                    // LazyListState to control scrolling for the horizontal list
                    val selectedUsersListState = rememberLazyListState()

                    // Automatically scroll to the last item when a new user is added
                    LaunchedEffect(selectedUsers) {
                        if (selectedUsers.isNotEmpty()) {
                            selectedUsersListState.animateScrollToItem(selectedUsers.size - 1)
                        }
                    }

                    if (selectedUsers.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            state = selectedUsersListState
                        ) {
                            items(selectedUsers, key = { it.uid }) { user ->
                                Card(
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = LightGreen)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color.Gray),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Person,
                                                contentDescription = "User icon",
                                                tint = White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = user.username, color = White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Remove ${user.username}",
                                            tint = White,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .clickable {
                                                    selectedUsers = selectedUsers - user
                                                }
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(allUsers, key = { it.uid }) { user ->
                            val isSelected = user in selectedUsers
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedUsers = if (isSelected) {
                                            selectedUsers - user
                                        } else {
                                            selectedUsers + user
                                        }
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) LightGreen else Color.Gray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = "User icon",
                                        tint = White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(text = user.username, color = if (isSelected) LightGreen else Color.Black)
                            }
                        }
                    }
                }
                FloatingActionButton(
                    onClick = {
                        if (groupName.isNotEmpty() && selectedUsers.isNotEmpty()) {
                            chatRoomsViewModel.createChatRoom(
                                groupName = groupName,
                                selectedUsers = selectedUsers,
                                currentUserId = currentUserId
                            )
                        } else {
                            if (groupName.isEmpty()) groupNameError = true
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomEnd),
                    containerColor = if (groupName.isNotEmpty()
                        && selectedUsers.isNotEmpty()) LightGreen else Color.LightGray,
                    contentColor = White,
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = stringResource(R.string.des_create_group),
                        tint = White
                    )
                }
            }
        }
    }
}