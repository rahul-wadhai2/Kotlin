package com.jejecomms.realtimechatfeature.ui.chatscreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jejecomms.realtimechatfeature.R
import com.jejecomms.realtimechatfeature.data.model.ChatMessage
import com.jejecomms.realtimechatfeature.data.model.MessageStatus
import com.jejecomms.realtimechatfeature.ui.theme.LightGreen
import com.jejecomms.realtimechatfeature.ui.theme.White
import com.jejecomms.realtimechatfeature.utils.DateUtils.formatDate
import com.jejecomms.realtimechatfeature.utils.DateUtils.formatTime

/**
 * Composable function to display a single chat message bubble.
 * It adapts its appearance based on whether the message is from the current user,
 * is a system message, and its sending status.
 *
 * @param message The ChatMessage object to display.
 * @param isCurrentUser Boolean indicating if the message was sent by the current user.
 * @param onRetryClick Lambda function to be invoked when the retry icon for a failed message is clicked.
 */
@Composable
fun MessageBubble(
    message: ChatMessage,
    isCurrentUser: Boolean,
    onRetryClick: (ChatMessage) -> Unit
) {
    /**
     * This colour is used to set the background color of the message bubble.
     * for differentiate Sender and receiver.
     */
    val bubbleColor = if (isCurrentUser) LightGreen else White

    /**
     * This colour is used to set the text color of the message bubble.
     */
    val textColor = if (isCurrentUser) Color.White else Color.Black

    /**
     * This colour is used to set the timestamp text color of the message bubble.
     */
    val timestampColor = if (isCurrentUser) Color.White.copy(alpha = 0.7f)
    else Color.Black.copy(alpha = 0.7f)

    /**
     * This value is used to set the corner radius of the message bubble.
     */
    val cornerRadius = 10.dp

    /**
     * This shape is used to set the shape of the message bubble.
     */
    val bubbleShape = if (isCurrentUser) {
        RoundedCornerShape(
            topStart = cornerRadius,
            topEnd = cornerRadius,
            bottomStart = cornerRadius,
            bottomEnd = 0.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 0.dp,
            topEnd = cornerRadius,
            bottomStart = cornerRadius,
            bottomEnd = cornerRadius
        )
    }

    /**
     * This value is used to set the horizontal alignment of the message bubble.
     */
    val horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start

    /**
     * This value is used to set the elevation of the message bubble.
     */
    val bubbleElevation = 2.dp

    // --- Regular User Message Handling ---
    val contentHorizontalPadding = 10.dp
    val messageBubblePadding = 8.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = messageBubblePadding,
                end = messageBubblePadding,
                top = 1.dp,
                bottom = 1.dp
            ),
        horizontalAlignment = horizontalAlignment
    ) {
        Box(
            modifier = Modifier
                .shadow(elevation = bubbleElevation, shape = bubbleShape)
                .background(bubbleColor, bubbleShape)
                .wrapContentHeight()
                .padding(horizontal = contentHorizontalPadding, vertical = 2.dp)
                .widthIn(max = 280.dp)
        ) {
            Column {
                Text(
                    text = message.text,
                    color = textColor,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )

                Spacer(modifier = Modifier.height(0.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(message.timestamp),
                        fontSize = 10.sp,
                        color = timestampColor
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // --- Message Status Icons ---
                    when (message.status) {
                        MessageStatus.SENDING -> {
                            if (isCurrentUser) {
                                Icon(
                                    imageVector = Icons.Filled.Done, // Single tick for sending
                                    contentDescription = stringResource(R.string.des_sending),
                                    tint = timestampColor.copy(alpha = 0.8f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        MessageStatus.SENT -> {
                            if (isCurrentUser) {
                                Icon(
                                    imageVector = Icons.Filled.DoneAll, // Double tick for sent
                                    contentDescription = stringResource(R.string.des_sent),
                                    tint = timestampColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        MessageStatus.FAILED -> {
                            // Display a clickable retry icon
                            IconButton(
                                onClick = { onRetryClick(message)  },
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
    }
}



