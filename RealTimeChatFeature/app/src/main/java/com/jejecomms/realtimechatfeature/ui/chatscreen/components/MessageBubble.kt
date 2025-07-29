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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.LightGray
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jejecomms.realtimechatfeature.data.model.ChatMessage
import com.jejecomms.realtimechatfeature.ui.theme.DarkGreen
import com.jejecomms.realtimechatfeature.utils.DateUtils

@Composable
fun MessageBubble(
    message: ChatMessage,
    isCurrentUser: Boolean
) {
    val bubbleColor = if (isCurrentUser) DarkGreen else LightGray
    val textColor = if (isCurrentUser) Color.White else Color.Black
    val timestampColor = if (isCurrentUser) Color.White.copy(alpha = 0.7f)
    else Color.Black.copy(alpha = 0.7f)

    val tailWidth = 8.dp
    val tailHeight = 6.dp
    val cornerRadius = 10.dp

    // Create the custom bubble shape
    val customBubbleShape = CustomBubbleShape(
        cornerRadiusDp = cornerRadius,
        tailWidthDp = tailWidth,
        tailHeightDp = tailHeight,
        isLeftTail = !isCurrentUser
    )

    val horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start

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
        val startPadding = if (isCurrentUser) 8.dp else 8.dp + tailWidth
        val endPadding = if (isCurrentUser) 8.dp + tailWidth else 8.dp
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = startPadding,
                    end = endPadding,
                    top = 1.dp,
                    bottom = 1.dp
                ),
            horizontalAlignment = horizontalAlignment
        ) {
            Box(
                modifier = Modifier
                    .background(bubbleColor, customBubbleShape)
                    .wrapContentHeight()
                    .padding(
                        start = if (isCurrentUser) 10.dp else 10.dp + tailWidth,
                        end = if (isCurrentUser) 10.dp + tailWidth else 10.dp,
                        top = 3.dp,
                        bottom = 3.dp
                    )
                    .widthIn(max = 280.dp)
            ) {
                Column {
                    Text(
                        text = message.text,
                        color = textColor,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(1.dp))

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
