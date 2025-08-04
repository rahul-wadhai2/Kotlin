package com.jejecomms.realtimechatfeature.ui.chatroomscreen.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jejecomms.realtimechatfeature.R
import com.jejecomms.realtimechatfeature.data.local.ChatMessageEntity
import com.jejecomms.realtimechatfeature.data.model.MessageStatus
import com.jejecomms.realtimechatfeature.utils.DateUtils.formatTime

/**
 * Reusable composable for displaying the message timestamp and status icons.
 *
 * @param message The chat message entity.
 * @param isCurrentUser Boolean indicating if the message is from the current user.
 * @param onRetryClick Lambda for retry action on failed messages.
 * @param isOverlay Boolean to determine text color for overlays.
 * @param modifier Modifier for this composable.
 */
@Composable
fun MessageStatusFooter(
    message: ChatMessageEntity,
    isCurrentUser: Boolean,
    onRetryClick: (ChatMessageEntity) -> Unit,
    isOverlay: Boolean = false,
    modifier: Modifier = Modifier
) {
    val timeColor = if (isOverlay) Color.White.copy(alpha = 0.7f) else if (isCurrentUser) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f)
    val iconTint = if (isOverlay) Color.White.copy(alpha = 0.7f) else if (isCurrentUser) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatTime(message.timestamp),
            fontSize = 10.sp,
            color = timeColor
        )
        Spacer(modifier = Modifier.width(4.dp))
        if (isCurrentUser) {
            when (message.status) {
                MessageStatus.SENDING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = iconTint,
                        strokeWidth = 2.dp
                    )
                }
                MessageStatus.SENT -> {
                    Icon(
                        imageVector = Icons.Filled.DoneAll,
                        contentDescription = stringResource(R.string.des_sent),
                        tint = iconTint,
                        modifier = Modifier.size(14.dp)
                    )
                }
                MessageStatus.FAILED -> {
                    IconButton(
                        onClick = { onRetryClick(message) },
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.des_retry),
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}