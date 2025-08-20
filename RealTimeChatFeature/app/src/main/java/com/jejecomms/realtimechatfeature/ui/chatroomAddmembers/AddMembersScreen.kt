package com.jejecomms.realtimechatfeature.ui.chatroomAddmembers

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jejecomms.realtimechatfeature.R
import com.jejecomms.realtimechatfeature.data.local.entity.UsersEntity
import com.jejecomms.realtimechatfeature.ui.chatroomdetailscreen.ChatRoomDetailViewModel
import com.jejecomms.realtimechatfeature.ui.theme.DarkGreenTheme
import com.jejecomms.realtimechatfeature.ui.theme.LightGreen
import com.jejecomms.realtimechatfeature.ui.theme.White
import com.jejecomms.realtimechatfeature.utils.ToastUtils
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.text.isNotEmpty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMembersScreen(
    onBack: (showAddMembersScreen: Boolean) -> Unit,
    chatRoomDetailViewModel: ChatRoomDetailViewModel,
    senderId: String
) {
    /**
     * Collect the list of all users from the ViewModel.
     */
    val allUsers by chatRoomDetailViewModel.allUsers.collectAsStateWithLifecycle()

    /**
     * Collect the loading state from the ViewModel.
     */
    val isLoading by chatRoomDetailViewModel.isLoading.collectAsStateWithLifecycle()

    /**
     * State to track the selected users to add to the room.
     */
    var selectedUsersToAdd by remember { mutableStateOf(listOf<UsersEntity>()) }

    /**
     * Collect the list of members in the room from the ViewModel.
     */
    val membersInRoom by chatRoomDetailViewModel.members.collectAsStateWithLifecycle()

    /**
     * Calculate the existing member IDs to avoid adding them again.
     */
    val existingMemberIds = remember(membersInRoom) { membersInRoom.map { it.userId }.toSet() }

    /**
     * Filter the list of all users to get the ones not in the room.
     */
    val usersToAdd = remember(allUsers, existingMemberIds) {
        allUsers.filter { it.uid !in existingMemberIds}
    }

    /**
     * Get the context.
     */
    val context = LocalContext.current

    /**
     * Handle back button press.
     */
    BackHandler { onBack(false) }

    LaunchedEffect(Unit) {
        chatRoomDetailViewModel.loadAllUsers(senderId)
    }

    // This LaunchedEffect will handle members added successfully message.
    LaunchedEffect(chatRoomDetailViewModel) {
        chatRoomDetailViewModel.successMessage.onEach { message ->
            message?.let {
                if (message.isNotEmpty()) {
                    ToastUtils.showLongToast(context, message)
                    chatRoomDetailViewModel.clearSuccessMessage()
                }
            }
        }.launchIn(this)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(fontSize = 18.sp, text = stringResource(R.string.add_members),
                          color = White) },
                navigationIcon = {
                    IconButton(onClick = { onBack(false) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                            tint = White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkGreenTheme,
                    titleContentColor = White,
                    navigationIconContentColor = White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().alpha(if (isLoading) 0.5f else 1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (usersToAdd.isEmpty()) {
                    Text(
                        fontSize = 14.sp,
                        text = stringResource(R.string.no_members_to_add),
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxHeight(0.8f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(usersToAdd, key = { it.uid }) { user ->
                            val isSelected = user in selectedUsersToAdd
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isLoading) {
                                        selectedUsersToAdd = if (isSelected) {
                                            selectedUsersToAdd.filter { it != user }
                                        } else {
                                            selectedUsersToAdd + user
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
                                Text(
                                    fontSize = 14.sp,
                                    text = user.username,
                                    color = if (isSelected) LightGreen else Color.Black,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(start = 64.dp, end = 16.dp),
                                thickness = 1.dp,
                                color = Color.LightGray )
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = {
                    if (!isLoading && selectedUsersToAdd.isNotEmpty()) {
                        chatRoomDetailViewModel.addMembersToRoom(selectedUsersToAdd)
                    }
                },
                modifier = Modifier.align(Alignment.BottomEnd),
                containerColor = if (selectedUsersToAdd.isNotEmpty()) LightGreen
                else Color.LightGray,
                contentColor = White,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = stringResource(R.string.des_add_members),
                        tint = White
                    )
                }
            }
        }
    }
}