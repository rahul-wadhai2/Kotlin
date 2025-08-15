package com.jejecomms.realtimechatfeature.ui.chatroomscreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jejecomms.realtimechatfeature.R
import com.jejecomms.realtimechatfeature.data.local.entity.ChatMessageEntity
import com.jejecomms.realtimechatfeature.data.model.MessageStatus
import com.jejecomms.realtimechatfeature.data.model.MessageType
import com.jejecomms.realtimechatfeature.ui.theme.LightGreen
import com.jejecomms.realtimechatfeature.ui.theme.White
import java.io.File

/**
 * Composable function to display a single chat message bubble.
 * It adapts its appearance based on whether the message is from the current user,
 * is a system message, and its sending status.
 *
 * @param message The ChatMessage object to display.
 * @param isCurrentUser Boolean indicating if the message was sent by the current user.
 */
@Composable
fun MessageBubble(
    message: ChatMessageEntity,
    isCurrentUser: Boolean,
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

    /**
     * This value is used to set the vertical padding of the message bubble.
     */
    val messageBubblePadding = 8.dp

    /**
     * Local context.
     */
    val context = LocalContext.current


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = messageBubblePadding,
                end = messageBubblePadding,
                top = 4.dp,
                bottom = 1.dp
            ),
        horizontalAlignment = horizontalAlignment
    ) {
        Box(
            modifier = Modifier
                .shadow(elevation = bubbleElevation, shape = bubbleShape)
                .background(bubbleColor, bubbleShape)
                .wrapContentHeight()
                .widthIn(max = 280.dp)
        ) {
            when (message.messageType) {
                MessageType.TEXT -> {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = message.text,
                            color = textColor,
                            fontSize = 14.sp
                        )
                        MessageStatusFooter(
                            message = message,
                            isCurrentUser = isCurrentUser
                        )
                    }
                }

                MessageType.IMAGE -> {
                    message.url?.let { imageUrl ->
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .clip(bubbleShape)
                        ) {
                            // handle for local image message only not online upload.
                            // Will do later work on online.
                            val imageRequest = ImageRequest.Builder(context)
                                .data(imageUrl)
                                .build()

                            AsyncImage(
                                model = imageRequest,
                                contentDescription = stringResource(R.string.des_chat_image),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(bubbleShape),
                                contentScale = ContentScale.Crop
                            )
                            // Display a loading indicator on top of the image if it's still sending
                            if (message.status == MessageStatus.SENDING) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            }
                            // Overlay for time and status on the image
                            MessageStatusFooter(
                                message = message,
                                isCurrentUser = isCurrentUser,
                                isOverlay = true,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .background(
                                        Color.Black.copy(alpha = 0.5f),
                                        // Shape for the overlay footer
                                        shape = RoundedCornerShape(topStart = 8.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                MessageType.DOCUMENT -> {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                            contentDescription = "Document",
                            tint = textColor,

                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        message.url?.let { fileName ->
                            Text(
                                text =  File(fileName).name,
                                color = textColor,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Status footer for document messages
                        MessageStatusFooter(
                            message = message,
                            isCurrentUser = isCurrentUser,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                MessageType.AUDIO -> {
                    AudioBubbleContent(message, textColor, isCurrentUser = isCurrentUser)
                }
            }
        }
    }
}