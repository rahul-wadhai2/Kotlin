package com.jejecomms.realtimechatfeature.ui.chatscreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jejecomms.realtimechatfeature.data.model.ChatMessage
import com.jejecomms.realtimechatfeature.ui.theme.LightGreen
import com.jejecomms.realtimechatfeature.ui.theme.White
import com.jejecomms.realtimechatfeature.utils.DateUtils

/**
 *  Message bubble composable.
 */
@Composable
fun MessageBubble(
    message: ChatMessage,
    isCurrentUser: Boolean
) {
    val bubbleColor = if (isCurrentUser) LightGreen else White
    val textColor = if (isCurrentUser) Color.White else Color.Black
    val timestampColor = if (isCurrentUser) Color.White.copy(alpha = 0.7f)
    else Color.Black.copy(alpha = 0.7f)

    val cornerRadius = 10.dp

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

    val horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start

    val bubbleElevation = 2.dp

    if (message.isSystemMessage) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Text(
                text = DateUtils.formatTime(message.timestamp),
                fontSize = 10.sp,
                color = Color.Gray
            )
        }
    } else {
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
                            text = DateUtils.formatTime(message.timestamp),
                            fontSize = 10.sp,
                            color = timestampColor
                        )
                    }
                }
            }
        }
    }
}


