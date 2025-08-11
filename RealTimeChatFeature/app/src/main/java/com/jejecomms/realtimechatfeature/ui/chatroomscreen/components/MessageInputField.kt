package com.jejecomms.realtimechatfeature.ui.chatroomscreen.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults.outlinedTextFieldColors
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jejecomms.realtimechatfeature.R
import com.jejecomms.realtimechatfeature.ui.theme.DarkGreen
import com.jejecomms.realtimechatfeature.ui.theme.LightGreen
import com.jejecomms.realtimechatfeature.ui.theme.LightGrey240
import com.jejecomms.realtimechatfeature.utils.Constants.MESSAGE_CHAR_LIMIT

/**
 *  Composable function for the message input field.
 *  This message input field allows users to send messages to the chat.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageInputField(
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    onAttachmentClick: () -> Unit
) {
    /**
     *  Stores the text entered in the message input field.
     */
    var messageText by remember { mutableStateOf("") }

    /**
     *  Controls the visibility of the send button.
     */
    val showSendButton = messageText.isNotBlank()

    /**
     * Maximum character limit.
     */
    val maxChar = MESSAGE_CHAR_LIMIT

    /**
     * State to track if the input field is focused then show the gallery icon.
     */
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Attachment Icon - now visible only when the text field is focused.
        if (isFocused) {
            IconButton(
                onClick = {
                    onAttachmentClick()
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_attached),
                    contentDescription = stringResource(R.string.des_send_image),
                    tint = LightGreen
                )
            }
        }
        Surface(
            modifier = Modifier
                .weight(1f)
                .animateContentSize()
                .padding(end = 8.dp, start = 8.dp, bottom = 8.dp),
            shape = RoundedCornerShape(25.dp),
            color = LightGrey240,
            shadowElevation = 2.dp
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { newValue ->
                    if (newValue.length <= maxChar) {
                        messageText = newValue
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                },
                placeholder = { Text(stringResource(R.string.message_placeholder)) },
                singleLine = false,
                maxLines = 5,
                shape = RoundedCornerShape(25.dp),
                colors = outlinedTextFieldColors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = Color.Black,
                    focusedPlaceholderColor = Color.LightGray,
                    unfocusedPlaceholderColor = Color.LightGray,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                )
            )
        }

        AnimatedContent(
            targetState = showSendButton,
            transitionSpec = {
                fadeIn(animationSpec = tween(200)) togetherWith
                        fadeOut(animationSpec = tween(200))
            }, label = "SendButtonAnimation"
        ) { targetShowSendButton ->
            if (targetShowSendButton) {
                FloatingActionButton(
                    onClick = {
                        onSendMessage(messageText.trim())
                        messageText = ""
                    },
                    modifier = Modifier.size(52.dp),
                    containerColor = DarkGreen,
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.send_message))
                }
            } else {
                Spacer(modifier = Modifier.width(0.dp))
            }
        }
    }
}