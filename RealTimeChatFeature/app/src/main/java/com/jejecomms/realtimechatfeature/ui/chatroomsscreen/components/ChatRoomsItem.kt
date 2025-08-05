package com.jejecomms.realtimechatfeature.ui.chatroomsscreen.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jejecomms.realtimechatfeature.R
import com.jejecomms.realtimechatfeature.data.local.ChatRoomEntity
import com.jejecomms.realtimechatfeature.ui.theme.White
import com.jejecomms.realtimechatfeature.utils.DateUtils.formatTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomItem(
    chatRoom: ChatRoomEntity,
    onChatRoomClick: (ChatRoomEntity) -> Unit,
    onArchive: (ChatRoomEntity) -> Unit,
    onDelete: (ChatRoomEntity) -> Unit,
    onToggleMute: (ChatRoomEntity) -> Unit,
) {
    var isRevealed by remember { mutableStateOf(false) }

    // State to handle the UI for mute/unmute icon locally
    var isMutedUiState by remember { mutableStateOf(chatRoom.isMuted) }

    SwipeableItemWithActions(
        isRevealed = isRevealed,
        onExpanded = { isRevealed = true },
        onCollapsed = { isRevealed = false },
        actions = {
            // Use a Row for the actions to handle alignment and spacing
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(start = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Archive button
                Box(
                    modifier = Modifier
                        .width(50.dp)
                        .fillMaxHeight()
                        .background(White)
                        .clickable {
                            onArchive(chatRoom)
                            isRevealed = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Archive, contentDescription = "Archive", tint = Color.DarkGray)
                }
                Spacer(modifier = Modifier.width(4.dp))
                // Delete button
                Box(
                    modifier = Modifier
                        .width(50.dp)
                        .fillMaxHeight()
                        .background(White)
                        .clickable {
                            onDelete(chatRoom)
                            isRevealed = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                }
            }
        }
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clickable {
                    if (isRevealed) {
                        isRevealed = false
                    } else {
                        onChatRoomClick(chatRoom)
                    }
                },
            colors = CardDefaults.elevatedCardColors(
                containerColor = Color.White
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 4.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Avatar Profile Image with a light gray border
                Surface(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .border(BorderStroke(1.dp, Color.LightGray), CircleShape),
                    color = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = stringResource(R.string.user_avatar),
                        modifier = Modifier.padding(8.dp),
                        tint = Color.LightGray
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = chatRoom.groupName.toString(),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.DarkGray,
                        fontSize = 14.sp
                    )
                    Text(
                        text = chatRoom.lastMessage?.toString() ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.DarkGray,
                        fontSize = 12.sp
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = formatTime(chatRoom.lastTimestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (chatRoom.unreadCount > 0) {
                        UnreadCountBadge(count = chatRoom.unreadCount)
                    }
                }
                // Mute/Unmute icon on the far right, with a local state
                IconButton(onClick = {
                    isMutedUiState = !isMutedUiState // Toggle local UI state first
                    onToggleMute(chatRoom) // Then call the logic to update the database
                }) {
                    Icon(
                        imageVector = if (isMutedUiState) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                        contentDescription = if (isMutedUiState) stringResource(R.string.unmute) else stringResource(
                            R.string.mute
                        ),
                        tint = if (isMutedUiState) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}