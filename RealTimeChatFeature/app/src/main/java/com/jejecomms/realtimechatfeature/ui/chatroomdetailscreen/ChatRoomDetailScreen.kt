package com.jejecomms.realtimechatfeature.ui.chatroomdetailscreen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jejecomms.realtimechatfeature.R
import com.jejecomms.realtimechatfeature.data.local.entity.ChatRoomMemberEntity
import com.jejecomms.realtimechatfeature.ui.chatroomdetailscreen.components.MemberListItem
import com.jejecomms.realtimechatfeature.ui.theme.DarkGreenTheme
import com.jejecomms.realtimechatfeature.ui.theme.LightGrey240
import com.jejecomms.realtimechatfeature.ui.theme.White
import me.onebone.toolbar.CollapsingToolbarScaffold
import me.onebone.toolbar.CollapsingToolbarState
import me.onebone.toolbar.ScrollStrategy
import me.onebone.toolbar.rememberCollapsingToolbarScaffoldState

/**
 * Composable function to display the chat room detail screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomDetailScreen(
    roomName: String,
    members: List<ChatRoomMemberEntity>,
    onLeaveRoom: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val state = rememberCollapsingToolbarScaffoldState()
    val toolbarState: CollapsingToolbarState = state.toolbarState

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
                    IconButton(onClick = onBack) {
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
            items(members) { member ->
                MemberListItem(member)
            }
            // Gray space is now a white space
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
                        .clickable(onClick = onLeaveRoom),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = stringResource(R.string.des_leave_room),
                        tint = Color.Red
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.leave_room),
                        color = Color.Red,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}