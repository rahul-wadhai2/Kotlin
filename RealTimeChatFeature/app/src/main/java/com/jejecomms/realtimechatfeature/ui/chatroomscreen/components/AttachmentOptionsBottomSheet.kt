package com.jejecomms.realtimechatfeature.ui.chatroomscreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.jejecomms.realtimechatfeature.ui.theme.LightGreen
import com.jejecomms.realtimechatfeature.ui.theme.White

/**
 * Composable function to display a bottom sheet for selecting different types of attachments.
 */
@Composable
fun AttachmentOptionsBottomSheet(
    onImageClick: () -> Unit,
    onDocumentClick: () -> Unit,
    onAudioClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            AttachmentOption(icon = Icons.Default.Image, onClick = onImageClick)
            AttachmentOption(icon = Icons.Default.Description, onClick = onDocumentClick)
            AttachmentOption(icon = Icons.Default.MusicNote, onClick = onAudioClick)
        }
    }
}

@Composable
fun AttachmentOption(icon: ImageVector, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
            .background(Color.White)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(LightGreen, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "text",
                tint = White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}