package com.jejecomms.realtimechatfeature.ui.chatroomscreen.components

import android.media.MediaPlayer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.jejecomms.realtimechatfeature.data.local.ChatMessageEntity
import com.jejecomms.realtimechatfeature.utils.DateUtils.formatTime
import java.io.File

/**
 * Composable function to display the content of an audio bubble.
 */
@Composable
fun AudioBubbleContent(message: ChatMessageEntity, textColor: Color, isCurrentUser: Boolean) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0) }
    var totalDuration by remember { mutableStateOf(0) }
    val mediaPlayer = remember { MediaPlayer() }
    val currentUrl by rememberUpdatedState(message.url)

    // MediaPlayer lifecycle and preparation
    DisposableEffect(currentUrl) {
        if (currentUrl != null) {
            try {
                val audioUri = currentUrl!!.toUri()
                mediaPlayer.reset()
                mediaPlayer.setDataSource(context, audioUri)
                mediaPlayer.prepare()
                totalDuration = mediaPlayer.duration
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        onDispose { mediaPlayer.release() }
    }

    // Coroutine to update the progress slider
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying) {
                currentPosition = mediaPlayer.currentPosition
                if (currentPosition >= totalDuration) {
                    isPlaying = false
                    currentPosition = 0
                    mediaPlayer.seekTo(0)
                }
                kotlinx.coroutines.delay(100)
            }
        }
    }

    // Main layout for the audio bubble content
    Column(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        // File name at the top
        message.url?.let { fileName ->
            Text(
                text = File(fileName).name,
                color = textColor,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // Horizontal row for the audio controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Play/Pause button
            IconButton(onClick = {
                if (isPlaying) mediaPlayer.pause() else mediaPlayer.start()
                isPlaying = !isPlaying
            }) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause Audio",
                    tint = textColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Slider
            Slider(
                value = if (totalDuration > 0) currentPosition.toFloat() / totalDuration else 0f,
                onValueChange = { newValue ->
                    val newPosition = (newValue * totalDuration).toInt()
                    mediaPlayer.seekTo(newPosition)
                    currentPosition = newPosition
                },
                colors = SliderDefaults.colors(
                    thumbColor = textColor,
                    activeTrackColor = textColor,
                    inactiveTrackColor = textColor.copy(alpha = 0.5f)
                ),
                modifier = Modifier.weight(1f)
            )

            // Play time and status footer
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text(
                    text = "${formatTime(currentPosition)} / ${formatTime(totalDuration)}",
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                MessageStatusFooter(
                    message = message,
                    isCurrentUser = isCurrentUser
                )
            }
        }
    }
}